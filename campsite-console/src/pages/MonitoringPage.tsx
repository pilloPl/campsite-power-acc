import { Box, Stack, TextField } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton, NOT_OFFERED } from '../components/ActionButton';
import { CampsiteSelect } from '../components/CampsiteSelect';
import { Chart } from '../components/Chart';
import { DataTable } from '../components/DataTable';
import { enumLabel, formatDateTime, formatNumber } from '../components/format';
import { KpiCard } from '../components/KpiCard';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';
import { LogsSection } from './monitoring/LogsSection';

interface SocketRow {
  id: number;
  pedestalCode: string;
  campsiteName: string;
  linkVersion?: string;
  remoteControl?: boolean;
  lastIncoming?: string;
  socketNo?: number;
  name?: string;
  pitchNo?: string;
  status?: string;
  withoutConsumptionHours: number;
}

const FAULT_STATUSES = ['FAULTED', 'BREAKER_TRIPPED', 'UNAVAILABLE', 'UNKNOWN'];
const STATUS_COLOURS: Record<string, string> = {
  AVAILABLE: '#66bb6a', SUPPLYING: '#1e88e5', SUPPLYING_WITHOUT_CONSUMPTION: '#ffa726', OCCUPIED: '#90caf9',
  FAULTED: '#e53935', BREAKER_TRIPPED: '#b71c1c', UNAVAILABLE: '#9e9e9e', UNKNOWN: '#bdbdbd',
};

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

/** Tab 12: status report of the sockets (e.g. supplying without consumption), service commands and the logs. */
export default function MonitoringPage() {
  const { hasRight } = useAuth();
  const [campsiteGroupId, setCampsiteGroupId] = useState<number | undefined>();
  const [month, setMonth] = useState(currentMonth());
  const [pedestalCode, setPedestalCode] = useState('');

  const report = useApiQuery('/api/console/monitoring/getStatusReport', { month, campsiteGroupId });
  const map = useApiQuery('/api/console/map/getMapData', { campsiteGroupId });
  const commandOptions = (verb: string) => ({
    invalidates: ['/api/console/monitoring/', '/api/console/map/', '/api/console/dashboard/'],
    success: (response: { command?: { accepted?: boolean; message?: string; socketStatus?: string } }) =>
      response.command?.message ?? `${verb}${response.command?.socketStatus ? `: ${enumLabel(response.command.socketStatus)}` : ''}`,
  });
  const requestStatus = useApiMutation('/api/console/monitoring/requestStatus', commandOptions('Status requested'));
  const resetFault = useApiMutation('/api/console/monitoring/resetFault', commandOptions('Fault reset'));
  const resendMail = useApiMutation('/api/console/monitoring/resendFaultMail', commandOptions('Fault mail sent'));

  const lines = report.data?.lines ?? [];
  const withoutConsumption = useMemo(() => {
    const hours = new Map<number, number>();
    lines.filter((line) => line.status === 'SUPPLYING_WITHOUT_CONSUMPTION')
      .forEach((line) => hours.set(line.socketId ?? 0, (line.timeInSeconds ?? 0) / 3600));
    return hours;
  }, [lines]);

  const sockets = useMemo<SocketRow[]>(() => (map.data?.map?.campsites ?? []).flatMap((campsite) =>
    (campsite.pedestals ?? []).flatMap((pedestal) => (pedestal.sockets ?? []).map((socket) => ({
      id: socket.id ?? 0, pedestalCode: pedestal.code ?? '', campsiteName: campsite.displayName ?? campsite.name ?? '',
      linkVersion: pedestal.linkVersion, remoteControl: pedestal.remoteControl, lastIncoming: pedestal.lastIncoming,
      socketNo: socket.socketNo, name: socket.name, pitchNo: socket.pitchNo, status: socket.status,
      withoutConsumptionHours: withoutConsumption.get(socket.id ?? 0) ?? 0,
    })))), [map.data, withoutConsumption]);
  const pedestals = useMemo(() => [...new Set(sockets.map((socket) => socket.pedestalCode))].sort(), [sockets]);
  const selectedPedestal = pedestalCode || pedestals[0] || '';

  const chart = useMemo(() => {
    const socketNames = [...new Set(lines.map((line) => line.socketName ?? ''))].sort();
    const statuses = [...new Set(lines.map((line) => line.status ?? ''))];
    const series = statuses.map((status) => ({
      name: enumLabel(status), type: 'bar' as const, stack: 'time', itemStyle: { color: STATUS_COLOURS[status] ?? '#78909c' },
      emphasis: { focus: 'series' as const },
      data: socketNames.map((name) => {
        const line = lines.find((candidate) => candidate.socketName === name && candidate.status === status);
        return line ? Number(((line.timeInSeconds ?? 0) / 3600).toFixed(1)) : 0;
      }),
    }));
    return { socketNames, series };
  }, [lines]);

  const canEdit = hasRight(Right.PEDESTAL_EDIT);
  const noEdit = !canEdit && 'Requires the right PEDESTAL_EDIT';

  const columns = useMemo<GridColDef<SocketRow>[]>(() => [
    { field: 'name', headerName: 'Socket', width: 140 },
    { field: 'pitchNo', headerName: 'Pitch', width: 60 },
    { field: 'status', headerName: 'Status', width: 140, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'withoutConsumptionHours', headerName: 'No consumption (h)', width: 140, type: 'number',
      valueFormatter: (value: number) => (value ? formatNumber(value, 1) : '') },
    { field: 'linkVersion', headerName: 'Link', width: 55 },
    { field: 'lastIncoming', headerName: 'Last message', width: 125, valueFormatter: (value) => formatDateTime(value) },
    {
      field: 'actions', headerName: 'Service commands', flex: 1, minWidth: 420, sortable: false,
      renderCell: ({ row }) => {
        const faulted = FAULT_STATUSES.includes(row.status ?? '');
        return (
          <Stack direction="row" spacing={1} alignItems="center" sx={{ height: '100%' }}>
            <ActionButton label="Request status" testId={`request-status-${row.id}`} busy={requestStatus.isPending}
              disabledReason={noEdit || (!row.remoteControl && 'The pedestal has no remote control')}
              onClick={() => requestStatus.mutate({ pedestalCode: row.pedestalCode, socketNo: row.socketNo })} />
            <ActionButton label="Reset fault" color="warning" testId={`reset-fault-${row.id}`} busy={resetFault.isPending}
              disabledReason={noEdit || (!faulted && 'The socket reports no fault')}
              confirm={`Reset the fault of ${row.name}? The socket is set available again (or occupied with a running session).`}
              onClick={() => resetFault.mutate({ socketId: row.id })} />
            <ActionButton label="Resend fault mail" testId={`resend-mail-${row.id}`} busy={resendMail.isPending}
              disabledReason={noEdit || (!faulted && 'The socket reports no fault')}
              onClick={() => resendMail.mutate({ socketId: row.id })} />
          </Stack>
        );
      },
    },
  ], [noEdit, requestStatus, resetFault, resendMail]);

  const faulted = sockets.filter((socket) => FAULT_STATUSES.includes(socket.status ?? '')).length;
  const noConsumption = sockets.filter((socket) => socket.withoutConsumptionHours > 0).length;

  return (
    <Page
      title="Monitoring & service"
      toolbar={(
        <Stack direction="row" spacing={2}>
          <TextField size="small" type="month" label="Report month" value={month} onChange={(e) => setMonth(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} />
          <CampsiteSelect value={campsiteGroupId} onChange={setCampsiteGroupId} />
          <ActionButton label="Maintenance contacts" onClick={() => undefined} disabledReason={NOT_OFFERED} />
        </Stack>
      )}
    >
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <Box sx={{ display: 'grid', gridTemplateColumns: '1fr', gap: 2, width: { lg: 220 }, alignContent: 'start' }}>
          <KpiCard label="Monitored sockets" value={sockets.length} />
          <KpiCard label="Faulted sockets" value={faulted} tone={faulted ? 'error' : 'default'} testId="kpi-faulted" />
          <KpiCard label="Supplying without consumption" value={noConsumption} hint={`sockets in ${month}`}
            tone={noConsumption ? 'warning' : 'default'} />
        </Box>
        <Section title={`Status report ${month}: hours per socket and status`} flex={1} testId="status-report">
          <QueryState isLoading={report.isLoading} error={report.error}>
            <Chart height={330} testId="status-report-chart" option={{
              tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
              legend: { top: 0, textStyle: { fontSize: 11 } },
              grid: { left: 45, right: 15, top: 35, bottom: 75 },
              xAxis: { type: 'category', data: chart.socketNames, axisLabel: { rotate: 45, fontSize: 10 } },
              yAxis: { type: 'value', name: 'hours' },
              series: chart.series,
            }} />
          </QueryState>
        </Section>
      </Stack>
      <Section title="Sockets and service commands" testId="monitoring-sockets">
        <QueryState isLoading={map.isLoading} error={map.error}>
          <DataTable rows={sockets} columns={columns} height={420} testId="sockets-table"
            onRowClick={({ row }) => setPedestalCode(row.pedestalCode)}
            initialState={{
              pagination: { paginationModel: { pageSize: 25 } },
              sorting: { sortModel: [{ field: 'withoutConsumptionHours', sort: 'desc' }] },
            }} />
        </QueryState>
      </Section>
      <LogsSection pedestals={pedestals} pedestalCode={selectedPedestal} onPedestalChange={setPedestalCode}
        campsiteGroupId={campsiteGroupId} />
    </Page>
  );
}
