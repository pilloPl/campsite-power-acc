import { MenuItem, Tab, Tabs, TextField, Typography } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiQuery } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import { Right } from '../../auth/rights';
import { DataTable } from '../../components/DataTable';
import { enumLabel, formatDateTime, formatNumber } from '../../components/format';
import { QueryState, Section } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';

type View = 'status' | 'communication' | 'heartbeat' | 'events';

const statusColumns: GridColDef<Schemas['SocketStatusLogDTO']>[] = [
  { field: 'remoteDate', headerName: 'Pedestal time', width: 130, valueFormatter: (value) => formatDateTime(value) },
  { field: 'socketName', headerName: 'Socket', width: 140 },
  { field: 'status', headerName: 'Status', width: 190, renderCell: ({ value }) => <StatusChip status={value} /> },
  { field: 'meterKwh', headerName: 'Meter kWh', width: 100, type: 'number', valueFormatter: (value) => formatNumber(value, 2) },
  { field: 'currentA', headerName: 'Current A', width: 90, type: 'number', valueFormatter: (value) => formatNumber(value, 2) },
  { field: 'errors', headerName: 'Errors', flex: 1, minWidth: 200, sortable: false,
    valueGetter: (_v, row) => (row.errors ?? []).map((error) => enumLabel(error)).join(', ') },
  { field: 'createDate', headerName: 'Received', width: 130, valueFormatter: (value) => formatDateTime(value) },
];

const communicationColumns: GridColDef<Schemas['CommunicationLogDTO']>[] = [
  { field: 'date', headerName: 'Time', width: 130, valueFormatter: (value) => formatDateTime(value) },
  { field: 'logType', headerName: 'Type', width: 170, valueFormatter: (value) => enumLabel(value) },
  { field: 'message', headerName: 'Message', flex: 1, minWidth: 300 },
];

const heartbeatColumns: GridColDef<Schemas['HeartbeatTimeoutDTO']>[] = [
  { field: 'pedestalCode', headerName: 'Pedestal', width: 150 },
  { field: 'campsiteName', headerName: 'Campsite', width: 170 },
  { field: 'linkVersion', headerName: 'Link', width: 70 },
  { field: 'lastIncoming', headerName: 'Last message', width: 150, valueFormatter: (value) => formatDateTime(value) },
  { field: 'lastTimeout', headerName: 'Timed out', flex: 1, valueFormatter: (value) => formatDateTime(value) },
];

const eventColumns: GridColDef<Schemas['FailedEventDTO']>[] = [
  { field: 'createDate', headerName: 'Created', width: 130, valueFormatter: (value) => formatDateTime(value) },
  { field: 'eventType', headerName: 'Event', width: 220 },
  { field: 'attributes', headerName: 'Attributes', width: 260, valueFormatter: (value: string) => value?.replace(/\n/g, ' ') },
  { field: 'failure', headerName: 'Failure', flex: 1, minWidth: 200 },
  { field: 'retryCount', headerName: 'Retries', width: 75, type: 'number' },
  { field: 'lastRetryDate', headerName: 'Last retry', width: 130, valueFormatter: (value) => formatDateTime(value) },
];

/** Error history of the sockets, communication log of a pedestal, heartbeat timeouts and failed events. */
export function LogsSection({ pedestals, pedestalCode, onPedestalChange, campsiteGroupId }: {
  pedestals: string[]; pedestalCode: string; onPedestalChange: (code: string) => void; campsiteGroupId: number | undefined;
}) {
  const { hasRight } = useAuth();
  const [view, setView] = useState<View>('status');
  const request = { pedestalCode: pedestalCode || undefined, campsiteGroupId, maxRows: 300 };
  const needsPedestal = view === 'status' || view === 'communication';
  const status = useApiQuery('/api/console/monitoring/findSocketStatusLogs', request, { enabled: view === 'status' && !!pedestalCode });
  const communication = useApiQuery('/api/console/monitoring/findCommunicationLogs', request, { enabled: view === 'communication' && !!pedestalCode });
  const heartbeat = useApiQuery('/api/console/monitoring/findHeartbeatTimeouts', { campsiteGroupId, maxRows: 300 }, { enabled: view === 'heartbeat' });
  const events = useApiQuery('/api/console/monitoring/findFailedEvents', { maxRows: 300 }, { enabled: view === 'events' });

  const current = useMemo(() => {
    switch (view) {
      case 'status': return { query: status, rows: status.data?.logs, columns: statusColumns as GridColDef[] };
      case 'communication': return { query: communication, rows: communication.data?.logs, columns: communicationColumns as GridColDef[] };
      case 'heartbeat': return { query: heartbeat, rows: heartbeat.data?.timeouts, columns: heartbeatColumns as GridColDef[] };
      default: return { query: events, rows: events.data?.events, columns: eventColumns as GridColDef[] };
    }
  }, [view, status, communication, heartbeat, events]);

  return (
    <Section
      title="Logs"
      testId="monitoring-logs"
      actions={needsPedestal && (
        <TextField select size="small" label="Pedestal" value={pedestalCode} onChange={(e) => onPedestalChange(e.target.value)}
          sx={{ minWidth: 200 }} data-testid="log-pedestal">
          {pedestals.map((code) => <MenuItem key={code} value={code}>{code}</MenuItem>)}
        </TextField>
      )}
    >
      <Tabs value={view} onChange={(_e, value) => setView(value)} sx={{ mb: 1.5, borderBottom: 1, borderColor: 'divider' }}>
        <Tab value="status" label="Socket status and errors" />
        <Tab value="communication" label="Communication" />
        <Tab value="heartbeat" label="Heartbeat timeouts" />
        {hasRight(Right.CONFIG_EDIT) && <Tab value="events" label="Failed events" />}
      </Tabs>
      <QueryState isLoading={current.query.isLoading} error={current.query.error}>
        {(current.rows ?? []).length === 0 && !current.query.isFetching ? (
          <Typography color="text.secondary" sx={{ p: 2 }} data-testid="logs-empty">
            {needsPedestal && !pedestalCode ? 'Select a pedestal.' : view === 'heartbeat'
              ? 'No pedestal is timed out: every pedestal sent a heartbeat within the timeout.' : 'No entries.'}
          </Typography>
        ) : (
          <DataTable rows={current.rows as never[]} columns={current.columns} height={380} testId={`logs-${view}`} />
        )}
      </QueryState>
    </Section>
  );
}
