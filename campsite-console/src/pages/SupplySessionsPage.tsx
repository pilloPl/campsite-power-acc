import { Box, FormControlLabel, Stack, Switch, TextField } from '@mui/material';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton } from '../components/ActionButton';
import { CampsiteSelect } from '../components/CampsiteSelect';
import { Chart } from '../components/Chart';
import { DataTable } from '../components/DataTable';
import { FormDialog } from '../components/FormDialog';
import { enumLabel, formatDateTime, formatNumber, isoDay } from '../components/format';
import { KpiCard } from '../components/KpiCard';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';

type Session = Schemas['SupplySessionDTO'];
type Incident = Schemas['SupplyIncidentDTO'];

function sessionState(session: Session): string {
  if (session.active) return session.stale ? 'STALE' : 'SUPPLYING';
  return 'CLOSED';
}

/** Tab 7: live and recent supply sessions with remote stop and stale-session close; supply incidents and their review. */
export default function SupplySessionsPage() {
  const { hasRight } = useAuth();
  const [campsiteGroupId, setCampsiteGroupId] = useState<number | undefined>();
  const [activeOnly, setActiveOnly] = useState(true);
  const [pedestalCode, setPedestalCode] = useState('');
  const [from, setFrom] = useState(isoDay(-7));
  const [to, setTo] = useState(isoDay(0));
  const [paging, setPaging] = useState<GridPaginationModel>({ page: 0, pageSize: 25 });
  const [reviewing, setReviewing] = useState<Incident | null>(null);

  const sessions = useApiQuery('/api/console/supply/findSupplySessions', {
    campsiteGroupId, activeOnly, pedestalCode: pedestalCode.trim() || undefined,
    from: activeOnly ? undefined : from, to: activeOnly ? undefined : to, page: paging.page, rowsPerPage: paging.pageSize,
  }, { placeholderData: (previous) => previous, refetchInterval: 60_000 });
  const recent = useApiQuery('/api/console/supply/findSupplySessions', {
    campsiteGroupId, from: isoDay(-13), to: isoDay(0), page: 0, rowsPerPage: 500,
  });
  const incidents = useApiQuery('/api/console/supply/findSupplyIncidents', {
    campsiteGroupId, unreviewedOnly: true, page: 0, rowsPerPage: 100,
  });

  const stop = useApiMutation('/api/console/supply/stopSupply', {
    invalidates: ['/api/console/supply/', '/api/console/dashboard/', '/api/console/map/'],
    success: (response) => response.command?.message ?? (response.command?.accepted ? 'Supply stopped' : 'Stop sent'),
  });
  const closeStale = useApiMutation('/api/console/supply/closeStaleSession', {
    invalidates: ['/api/console/supply/', '/api/console/dashboard/'],
    success: (response) => `Session ${response.session?.id ?? ''} closed (${enumLabel(response.session?.stopReason)})`,
  });
  const review = useApiMutation('/api/console/supply/reviewSupplyIncident', {
    invalidates: ['/api/console/supply/'], success: 'Incident marked as reviewed', onSuccess: () => setReviewing(null),
  });

  const canStop = hasRight(Right.SOCKET_REMOTE_CONTROL);
  const canReview = hasRight(Right.PEDESTAL_EDIT);

  const columns = useMemo<GridColDef<Session>[]>(() => [
    { field: 'state', headerName: 'State', width: 105, sortable: false, valueGetter: (_v, row) => sessionState(row),
      renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'socketName', headerName: 'Socket', width: 140 },
    { field: 'pitchNo', headerName: 'Pitch', width: 60 },
    { field: 'campsiteName', headerName: 'Campsite', width: 135 },
    { field: 'guestName', headerName: 'Guest / card', flex: 1, minWidth: 150,
      valueGetter: (_v, row) => row.guestName ?? (row.houseCardType ? `House card ${enumLabel(row.houseCardType)}` : row.qrPayProcessId ? 'QR payment' : row.cardNumber) },
    { field: 'start', headerName: 'Start', width: 125, valueFormatter: (value) => formatDateTime(value) },
    { field: 'stop', headerName: 'End', width: 125, valueFormatter: (value) => formatDateTime(value) },
    { field: 'startMeterKwh', headerName: 'Meter start', width: 95, type: 'number', valueFormatter: (value) => formatNumber(value, 1) },
    { field: 'stopMeterKwh', headerName: 'Meter end', width: 95, type: 'number', valueFormatter: (value) => formatNumber(value, 1) },
    { field: 'kwh', headerName: 'kWh', width: 75, type: 'number', valueFormatter: (value) => formatNumber(value, 2) },
    { field: 'stopReason', headerName: 'End reason', width: 100, valueFormatter: (value) => enumLabel(value) },
    {
      field: 'actions', headerName: 'Actions', width: 120, sortable: false,
      renderCell: ({ row }) => {
        if (!row.active) return null;
        if (row.stale) {
          return (
            <ActionButton label="Close" color="warning" testId={`close-stale-${row.id}`}
              confirm={`Close the stale session on ${row.socketName}? It is closed like the stale-session job does.`}
              disabledReason={!canStop && 'Requires the right SOCKET_REMOTE_CONTROL'} busy={closeStale.isPending}
              onClick={() => closeStale.mutate({ sessionId: row.id })} />
          );
        }
        return (
          <ActionButton label="Stop" color="error" testId={`stop-${row.id}`}
            confirm={`Stop the supply on ${row.socketName} (${row.guestName ?? row.cardNumber ?? ''}) remotely?`}
            disabledReason={!canStop && 'Requires the right SOCKET_REMOTE_CONTROL'} busy={stop.isPending}
            onClick={() => stop.mutate({ sessionId: row.id })} />
        );
      },
    },
  ], [canStop, closeStale, stop]);

  const incidentColumns = useMemo<GridColDef<Incident>[]>(() => [
    { field: 'createDate', headerName: 'Time', width: 125, valueFormatter: (value) => formatDateTime(value) },
    { field: 'incidentType', headerName: 'Type', width: 150, valueFormatter: (value) => enumLabel(value) },
    { field: 'pedestalCode', headerName: 'Pedestal', width: 125 },
    { field: 'meterKwh', headerName: 'Meter', width: 85, type: 'number', valueFormatter: (value) => formatNumber(value, 1) },
    { field: 'message', headerName: 'Message', flex: 1, minWidth: 200 },
    {
      field: 'review', headerName: '', width: 130, sortable: false,
      renderCell: ({ row }) => (
        <ActionButton label="Mark reviewed" onClick={() => setReviewing(row)} testId={`review-incident-${row.id}`}
          disabledReason={!canReview && 'Requires the right PEDESTAL_EDIT'} />
      ),
    },
  ], [canReview]);

  const perDay = useMemo(() => {
    const days = new Map<string, { sessions: number; kwh: number }>();
    for (let offset = -13; offset <= 0; offset += 1) days.set(isoDay(offset), { sessions: 0, kwh: 0 });
    (recent.data?.sessions ?? []).forEach((session) => {
      const day = session.start?.substring(0, 10);
      const entry = day ? days.get(day) : undefined;
      if (entry) {
        entry.sessions += 1;
        entry.kwh += session.kwh ?? 0;
      }
    });
    return [...days.entries()];
  }, [recent.data]);

  const live = sessions.data;
  return (
    <Page title="Supply sessions" toolbar={<CampsiteSelect value={campsiteGroupId} onChange={setCampsiteGroupId} />}>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <Box sx={{ flex: 1, minWidth: 0, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2, alignContent: 'start' }}>
          <KpiCard label={activeOnly ? 'Running sessions' : 'Sessions in period'} value={live?.total ?? '-'} tone="success" testId="kpi-sessions" />
          <KpiCard label="Stale on this page" value={(live?.sessions ?? []).filter((s) => s.active && s.stale).length} tone="warning" />
          <KpiCard label="kWh on this page" value={formatNumber((live?.sessions ?? []).reduce((sum, s) => sum + (s.kwh ?? 0), 0), 1)} />
          <KpiCard label="Unreviewed incidents" value={incidents.data?.total ?? '-'} tone={incidents.data?.total ? 'warning' : 'default'} />
        </Box>
        <Section title="Sessions and kWh per day (14 days)" flex={2}>
          <Chart height={170} testId="sessions-per-day" option={{
            grid: { left: 40, right: 45, top: 25, bottom: 25 },
            tooltip: { trigger: 'axis' },
            legend: { top: 0, right: 0, textStyle: { fontSize: 11 } },
            xAxis: { type: 'category', data: perDay.map(([day]) => day.substring(5)) },
            yAxis: [{ type: 'value', minInterval: 1 }, { type: 'value', splitLine: { show: false } }],
            series: [
              { name: 'Sessions', type: 'bar', data: perDay.map(([, v]) => v.sessions), itemStyle: { color: '#2e6b3f' } },
              { name: 'kWh', type: 'line', yAxisIndex: 1, smooth: true, data: perDay.map(([, v]) => Number(v.kwh.toFixed(1))), itemStyle: { color: '#c77700' } },
            ],
          }} />
        </Section>
      </Stack>
      <Section
        title="Sessions"
        testId="sessions"
        actions={(
          <Stack direction="row" spacing={2} alignItems="center">
            <TextField size="small" label="Pedestal" value={pedestalCode} onChange={(e) => setPedestalCode(e.target.value)} sx={{ width: 170 }} />
            {!activeOnly && (
              <>
                <TextField size="small" type="date" label="From" value={from} onChange={(e) => setFrom(e.target.value)}
                  slotProps={{ inputLabel: { shrink: true } }} />
                <TextField size="small" type="date" label="To" value={to} onChange={(e) => setTo(e.target.value)}
                  slotProps={{ inputLabel: { shrink: true } }} />
              </>
            )}
            <FormControlLabel label="Running only" control={(
              <Switch checked={activeOnly} onChange={(e) => { setActiveOnly(e.target.checked); setPaging({ ...paging, page: 0 }); }}
                data-testid="active-only" />
            )} />
          </Stack>
        )}
      >
        <QueryState isLoading={sessions.isLoading} error={sessions.error}>
          <DataTable rows={live?.sessions} columns={columns} height={460} testId="sessions-table"
            paginationMode="server" rowCount={live?.total ?? 0} paginationModel={paging} onPaginationModelChange={setPaging}
            loading={sessions.isFetching} sortingMode="server" disableColumnSorting
            columnVisibilityModel={{ stop: !activeOnly, stopReason: !activeOnly, campsiteName: campsiteGroupId === undefined }} />
        </QueryState>
      </Section>
      <Section title="Unreviewed supply incidents" testId="incidents">
        <QueryState isLoading={incidents.isLoading} error={incidents.error}>
          <DataTable rows={incidents.data?.incidents} columns={incidentColumns} height={320} testId="incidents-table" />
        </QueryState>
      </Section>
      <FormDialog
        open={reviewing !== null}
        title="Mark incident as reviewed"
        description={reviewing?.message}
        fields={[{ name: 'comment', label: 'Comment', type: 'textarea' }]}
        submitLabel="Mark reviewed"
        busy={review.isPending}
        onClose={() => setReviewing(null)}
        onSubmit={(values) => review.mutate({ incidentId: reviewing?.id, comment: values.comment as string | undefined })}
      />
    </Page>
  );
}
