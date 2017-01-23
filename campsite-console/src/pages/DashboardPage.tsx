import { FormControlLabel, Stack, Switch } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { ActionButton } from '../components/ActionButton';
import { CampsiteSelect } from '../components/CampsiteSelect';
import { Chart } from '../components/Chart';
import { DataTable } from '../components/DataTable';
import { FormDialog } from '../components/FormDialog';
import { enumLabel, formatDateTime, formatNumber } from '../components/format';
import { KpiCard } from '../components/KpiCard';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';

type Alert = Schemas['AlertDTO'];

/** Tab 1: key figures of the network and the live alerts, which can be acknowledged. */
export default function DashboardPage() {
  const [campsiteGroupId, setCampsiteGroupId] = useState<number | undefined>();
  const [includeAcknowledged, setIncludeAcknowledged] = useState(false);
  const [acknowledging, setAcknowledging] = useState<Alert | null>(null);

  const dashboard = useApiQuery('/api/console/dashboard/getDashboard', { campsiteGroupId }, { refetchInterval: 60_000 });
  const alerts = useApiQuery('/api/console/dashboard/findAlerts', { campsiteGroupId, includeAcknowledged, maxRows: 200 });
  const acknowledge = useApiMutation('/api/console/dashboard/acknowledgeAlert', {
    invalidates: ['/api/console/dashboard/'],
    success: 'Alert acknowledged',
    onSuccess: () => setAcknowledging(null),
  });

  const kpi = dashboard.data?.dashboard;
  const rows = alerts.data?.alerts ?? [];

  const columns = useMemo<GridColDef<Alert>[]>(() => [
    { field: 'alertTime', headerName: 'Time', width: 125, valueFormatter: (value) => formatDateTime(value) },
    { field: 'severity', headerName: 'Severity', width: 90, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'alertType', headerName: 'Type', width: 120, valueFormatter: (value) => enumLabel(value) },
    { field: 'pedestalCode', headerName: 'Pedestal', width: 120 },
    { field: 'text', headerName: 'Alert', flex: 1, minWidth: 260 },
    {
      field: 'acknowledged', headerName: 'Acknowledged', width: 170,
      renderCell: ({ row }) => row.acknowledged
        ? `${row.acknowledgedBy ?? ''} ${formatDateTime(row.acknowledgeDate)}`
        : (
          <ActionButton label="Acknowledge" onClick={() => setAcknowledging(row)} testId={`ack-${row.alertKey}`} />
        ),
    },
  ], []);

  const byType = useMemo(() => {
    const counts = new Map<string, number>();
    rows.filter((alert) => !alert.acknowledged).forEach((alert) => {
      const type = enumLabel(alert.alertType);
      counts.set(type, (counts.get(type) ?? 0) + 1);
    });
    return [...counts.entries()].sort((a, b) => b[1] - a[1]);
  }, [rows]);

  return (
    <Page title="Dashboard" toolbar={<CampsiteSelect value={campsiteGroupId} onChange={setCampsiteGroupId} />}>
      <QueryState isLoading={dashboard.isLoading} error={dashboard.error}>
        <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap data-testid="kpis">
          <KpiCard label="Active sessions" value={kpi?.activeSessions ?? 0} tone="success" testId="kpi-active" />
          <KpiCard label="kWh today" value={formatNumber(kpi?.kwhToday, 1)} hint="records + running" />
          <KpiCard label="Faulted sockets" value={(kpi?.faultedSockets ?? 0) + (kpi?.breakerTrippedSockets ?? 0)}
            hint={`${kpi?.breakerTrippedSockets ?? 0} breaker tripped`} tone={kpi?.faultedSockets ? 'error' : 'default'} />
          <KpiCard label="No heartbeat" value={kpi?.timedOutPedestals ?? 0} hint="pedestals"
            tone={kpi?.timedOutPedestals ? 'warning' : 'default'} />
          <KpiCard label="SUSPECT review" value={kpi?.reviewQueue ?? 0} hint="records" tone={kpi?.reviewQueue ? 'warning' : 'default'} />
          <KpiCard label="Stale sessions" value={kpi?.staleSessions ?? 0} tone={kpi?.staleSessions ? 'warning' : 'default'} />
          <KpiCard label="Failed events" value={kpi?.failedEvents ?? 0} tone={kpi?.failedEvents ? 'warning' : 'default'} />
          <KpiCard label="ERP errors" value={kpi?.erpLinesInError ?? 0} hint="billing lines" tone={kpi?.erpLinesInError ? 'error' : 'default'} />
          <KpiCard label="Open alerts" value={kpi?.openAlerts ?? 0} hint={kpi?.asOf ? formatDateTime(kpi.asOf) : undefined} />
        </Stack>
      </QueryState>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <Section
          title="Live alerts"
          actions={(
            <FormControlLabel
              control={<Switch checked={includeAcknowledged} onChange={(e) => setIncludeAcknowledged(e.target.checked)} />}
              label="Show acknowledged"
            />
          )}
          testId="alerts"
          flex={3}
        >
          <QueryState isLoading={alerts.isLoading} error={alerts.error}>
            <DataTable rows={rows} columns={columns} getRowId={(row) => row.alertKey ?? ''} height={560} />
          </QueryState>
        </Section>
        <Section title="Open alerts by type" flex={1}>
          <Chart
            height={300}
            testId="alerts-by-type"
            option={{
              grid: { left: 130, right: 30, top: 10, bottom: 30 },
              xAxis: { type: 'value', minInterval: 1 },
              yAxis: { type: 'category', data: byType.map(([type]) => type).reverse() },
              tooltip: { trigger: 'axis' },
              series: [{ type: 'bar', data: byType.map(([, count]) => count).reverse(), itemStyle: { color: '#c77700' } }],
            }}
          />
        </Section>
      </Stack>
      <FormDialog
        open={acknowledging !== null}
        title="Acknowledge alert"
        description={acknowledging?.text}
        fields={[{ name: 'comment', label: 'Comment', type: 'textarea' }]}
        submitLabel="Acknowledge"
        busy={acknowledge.isPending}
        onClose={() => setAcknowledging(null)}
        onSubmit={(values) => acknowledge.mutate({ alertKey: acknowledging?.alertKey, comment: values.comment as string | undefined })}
      />
    </Page>
  );
}
