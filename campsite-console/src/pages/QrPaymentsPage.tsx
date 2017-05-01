import { Chip, MenuItem, Stack, TextField, Typography } from '@mui/material';
import type { GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton } from '../components/ActionButton';
import { CampsiteSelect } from '../components/CampsiteSelect';
import { Chart } from '../components/Chart';
import { DataTable } from '../components/DataTable';
import { enumLabel, formatDateTime, formatMoney, formatNumber } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';
import { stateDiagramOption } from './qrpay/stateDiagram';

type Process = Schemas['QrPayProcessOverviewDTO'];
type Step = Schemas['Step'];

export const QR_STATES = ['START_REQUESTED', 'SOCKET_START_WAITING', 'SOCKET_START_RECEIVED', 'START_CONFIRMED',
  'STOP_REQUESTED', 'SOCKET_STOP_WAITING', 'SOCKET_STOP_RECEIVED', 'CALCULATION_REQUESTED', 'CALCULATION_DONE',
  'CLOSED', 'ERROR'];

const REFUND_REASON = 'Refunds are not offered: the backend has no payment provider connection';

/** Cancelling is offered for stuck processes and for ERROR processes; the backend checks the running supply. */
export function cancelReason(process: Process): string | null {
  if (process.archived) return 'Archived processes cannot be cancelled';
  if (process.state === 'CLOSED') return 'The process is closed';
  if (!process.stuck && process.state !== 'ERROR') return 'Only stuck processes or processes in ERROR';
  return null;
}

/** Tab 10: QR payment processes, the state timeline of a process as a state diagram, cancel and archive. */
export default function QrPaymentsPage() {
  const { hasRight } = useAuth();
  const billingReason = hasRight(Right.BILLING_EDIT) ? null : 'Needs the right BILLING_EDIT';
  const [campsiteGroupId, setCampsiteGroupId] = useState<number | undefined>();
  const [state, setState] = useState('');
  const [pagination, setPagination] = useState<GridPaginationModel>({ page: 0, pageSize: 25 });
  const [selectedId, setSelectedId] = useState<number | undefined>();

  const processes = useApiQuery('/api/console/qrPay/findQrPayProcesses', {
    campsiteGroupId, state: state || undefined, page: pagination.page, rowsPerPage: pagination.pageSize,
  });
  const rows = processes.data?.processes ?? [];
  const processId = selectedId ?? rows[0]?.processId;
  const timeline = useApiQuery('/api/console/qrPay/getQrPayTimeline', { processId }, { enabled: processId !== undefined });
  const detail = timeline.data?.timeline;

  const invalidates = ['/api/console/qrPay/', '/api/console/dashboard/'];
  const cancel = useApiMutation('/api/console/qrPay/cancelQrPayProcess', {
    invalidates, success: (response) => `Process ${response.timeline?.orderReference ?? ''} is ${enumLabel(response.timeline?.currentState)}`,
  });
  const archive = useApiMutation('/api/console/qrPay/runQrPayArchive', {
    invalidates, success: (response) => `${response.archivedRows ?? 0} process row(s) archived`,
  });

  const columns: GridColDef<Process>[] = [
    { field: 'orderReference', headerName: 'Order', width: 100 },
    {
      field: 'state', headerName: 'State', width: 190,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%' }}>
          <StatusChip status={row.state} />
          {row.stuck && <Chip size="small" color="warning" label="Stuck" />}
          {row.archived && <Chip size="small" variant="outlined" label="Archived" />}
        </Stack>
      ),
    },
    { field: 'campsiteName', headerName: 'Campsite', width: 130 },
    { field: 'socketCode', headerName: 'Socket', width: 130 },
    { field: 'appUserId', headerName: 'App user', width: 95 },
    { field: 'requestDate', headerName: 'Requested', width: 128, valueFormatter: (value) => formatDateTime(value) },
    { field: 'kwh', headerName: 'kWh', width: 70, type: 'number', valueFormatter: (value) => formatNumber(value, 2) },
    { field: 'cost', headerName: 'Amount', width: 95, type: 'number', valueFormatter: (value, row) => formatMoney(value, row.currency) },
    { field: 'errorMessage', headerName: 'Error', flex: 1, minWidth: 60 },
    {
      field: 'actions', headerName: '', width: 160, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%', '& .MuiButton-root': { px: 1, minWidth: 0 } }}>
          <ActionButton label="Cancel" color="error" busy={cancel.isPending}
            disabledReason={billingReason ?? cancelReason(row)}
            confirm={`Cancel the QR payment ${row.orderReference}? A running supply is stopped first.`}
            onClick={() => cancel.mutate({ processId: row.processId })} testId={`cancel-${row.processId}`} />
          <ActionButton label="Refund" disabledReason={REFUND_REASON} onClick={() => undefined} />
        </Stack>
      ),
    },
  ];

  const total = processes.data?.total ?? 0;
  const stuck = rows.filter((row) => row.stuck).length;
  const errors = rows.filter((row) => row.state === 'ERROR').length;
  const amount = rows.reduce((sum, row) => sum + (row.cost ?? 0), 0);

  const stepColumns: GridColDef<Step>[] = [
    { field: 'state', headerName: 'State', width: 190, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'since', headerName: 'Since', width: 140, valueFormatter: (value) => formatDateTime(value) },
    { field: 'until', headerName: 'Until', width: 140, valueFormatter: (value) => formatDateTime(value) },
    { field: 'transactionId', headerName: 'Supply', width: 110 },
    { field: 'errorMessage', headerName: 'Error', flex: 1, valueGetter: (_v, row) => [row.errorCode, row.errorMessage].filter(Boolean).join(': ') },
  ];

  return (
    <Page
      title="QR payments"
      toolbar={(
        <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
          <CampsiteSelect value={campsiteGroupId} onChange={(value) => { setCampsiteGroupId(value); setPagination({ ...pagination, page: 0 }); }} />
          <TextField select size="small" label="State" value={state} sx={{ minWidth: 190 }}
            onChange={(e) => { setState(e.target.value); setPagination({ ...pagination, page: 0 }); }}
            slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}>
            <MenuItem value="">All states</MenuItem>
            {QR_STATES.map((name) => <MenuItem key={name} value={name}>{enumLabel(name)}</MenuItem>)}
          </TextField>
          <ActionButton label="Run archive" variant="contained" busy={archive.isPending} disabledReason={billingReason}
            confirm="Move finished QR payment processes to the archive now?" onClick={() => archive.mutate({})} testId="run-archive" />
        </Stack>
      )}
    >
      <Section
        title={`Processes (${total})`}
        testId="qr-processes"
        actions={(
          <Stack direction="row" spacing={1}>
            <Chip size="small" color={stuck ? 'warning' : 'default'} label={`${stuck} stuck`} />
            <Chip size="small" color={errors ? 'error' : 'default'} label={`${errors} in error`} />
            <Chip size="small" color="success" variant="outlined" label={formatMoney(amount, rows.find((row) => row.currency)?.currency)} />
          </Stack>
        )}
      >
        <QueryState isLoading={processes.isLoading} error={processes.error}>
          <DataTable rows={rows} columns={columns} getRowId={(row) => row.processId ?? 0} height={300} hideFooterSelectedRowCount
            paginationMode="server" rowCount={total} paginationModel={pagination} onPaginationModelChange={setPagination}
            onRowClick={({ row }) => setSelectedId(row.processId)}
            rowSelectionModel={processId !== undefined ? [processId] : []}
            sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' }, bgcolor: 'background.paper' }} />
        </QueryState>
      </Section>
      <Section
        title={detail ? `State timeline of ${detail.orderReference} (process ${detail.processId})` : 'State timeline'}
        testId="qr-timeline"
        actions={detail && (
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="body2" color="text.secondary">Current state</Typography>
            <StatusChip status={detail.currentState} />
            {detail.archived && <Chip size="small" label="Archived" />}
          </Stack>
        )}
      >
        {processId === undefined ? (
          <Typography color="text.secondary">No process matches the filter.</Typography>
        ) : (
          <QueryState isLoading={timeline.isLoading} error={timeline.error}>
            <Chart option={stateDiagramOption(detail)} height={250} testId="qr-state-diagram" />
            <DataTable rows={detail?.steps ?? []} columns={stepColumns} getRowId={(row) => row.rowId ?? 0}
              height="auto" hideFooter />
          </QueryState>
        )}
      </Section>
    </Page>
  );
}
