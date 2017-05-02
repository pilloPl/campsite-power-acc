import { Alert, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Button, MenuItem, Stack, TextField } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { ActionButton } from '../components/ActionButton';
import { Chart } from '../components/Chart';
import { DataTable } from '../components/DataTable';
import { enumLabel, formatDateTime, formatKwh, formatMoney, formatNumber } from '../components/format';
import { KpiCard } from '../components/KpiCard';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';
import { EXPORT_STATUSES, billingRunsOption, linesPerStatus } from './erp/erpCharts';

type Run = Schemas['ErpBillingRunDTO'];
type Line = Schemas['ErpBillingLineDTO'];
type RecordRow = Schemas['ConsumptionRecordDTO'];

function previousMonth(today = new Date()): string {
  const month = new Date(today.getFullYear(), today.getMonth() - 1, 1);
  return `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, '0')}`;
}

const lineColumns: GridColDef<Line>[] = [
  { field: 'billingAccountNo', headerName: 'Billing account', width: 130 },
  { field: 'erpPartnerNo', headerName: 'ERP partner', width: 100 },
  { field: 'lineType', headerName: 'Type', width: 90, valueFormatter: (value) => enumLabel(value) },
  { field: 'description', headerName: 'Description', flex: 1, minWidth: 150 },
  { field: 'quantity', headerName: 'Quantity', width: 80, type: 'number', valueFormatter: (value) => formatNumber(value, 3) },
  { field: 'unitPrice', headerName: 'Unit price', width: 80, type: 'number', valueFormatter: (value) => formatNumber(value, 4) },
  { field: 'amount', headerName: 'Amount', width: 110, type: 'number', valueGetter: (_, row) => row.amount,
    valueFormatter: (value, row) => formatMoney(value, row.currency) },
  { field: 'exportStatus', headerName: 'Export', width: 120, renderCell: ({ value }) => <StatusChip status={value} /> },
  { field: 'errorText', headerName: 'ERP answer', flex: 1, minWidth: 170 },
  { field: 'exportDate', headerName: 'Exported', width: 125, valueFormatter: (value) => formatDateTime(value) },
];

const errorColumns: GridColDef<Line>[] = [
  { field: 'billingAccountNo', headerName: 'Billing account', width: 130 },
  { field: 'amount', headerName: 'Amount', width: 100, type: 'number', valueFormatter: (value, row) => formatMoney(value, row.currency) },
  { field: 'errorText', headerName: 'ERP answer', flex: 1, minWidth: 220 },
  { field: 'answerDate', headerName: 'Answered', width: 130, valueFormatter: (value) => formatDateTime(value) },
];

const recordColumns: GridColDef<RecordRow>[] = [
  { field: 'recordNo', headerName: 'Record', width: 210 },
  { field: 'campsiteName', headerName: 'Campsite', width: 170 },
  { field: 'pedestalCode', headerName: 'Pedestal', width: 130 },
  { field: 'guestName', headerName: 'Guest', width: 150 },
  { field: 'plugOut', headerName: 'Plug-out', width: 130, valueFormatter: (value) => formatDateTime(value) },
  { field: 'kwh', headerName: 'kWh', width: 100, type: 'number', valueFormatter: (value) => formatKwh(value) },
  { field: 'cost', headerName: 'Cost', width: 100, type: 'number', valueFormatter: (value, row) => formatMoney(value, row.currency) },
  { field: 'paymentChannel', headerName: 'Payment', width: 120, valueFormatter: (value) => enumLabel(value) },
  { field: 'exportStatus', headerName: 'Export', width: 100, renderCell: ({ value }) => <StatusChip status={value} /> },
];

/**
 * Tab 14: ERP billing of the network's billing accounts: runs per month, their export lines and ERP errors,
 * consumption records still without export; preview and run a month, send refused lines again.
 */
export default function ErpBillingPage() {
  const [month, setMonth] = useState(previousMonth());
  const [selectedPeriod, setSelectedPeriod] = useState<string | undefined>();
  const [exportStatus, setExportStatus] = useState('');
  const [preview, setPreview] = useState<Line[] | null>(null);

  const runs = useApiQuery('/api/console/erpBilling/findErpBillingRuns', {});
  const runRows = useMemo(() => [...(runs.data?.runs ?? [])]
    .sort((a, b) => (b.billingPeriod ?? '').localeCompare(a.billingPeriod ?? '')), [runs.data]);
  const period = selectedPeriod ?? runRows[0]?.billingPeriod;
  const lines = useApiQuery('/api/console/erpBilling/findErpBillingLines',
    { billingPeriod: period, exportStatus: exportStatus || undefined }, { enabled: Boolean(period) });
  const errors = useApiQuery('/api/console/erpBilling/findErpBillingLines',
    { billingPeriod: period, exportStatus: 'ERROR' }, { enabled: Boolean(period) });
  const withoutExport = useApiQuery('/api/console/erpBilling/findRecordsWithoutExport', {});

  const [year, monthNo] = month.split('-').map(Number);
  const previewCall = useApiMutation('/api/console/erpBilling/previewErpBilling', {
    onSuccess: (response) => setPreview(response.lines ?? []),
  });
  const runCall = useApiMutation('/api/console/erpBilling/runErpBilling', {
    invalidates: ['/api/console/erpBilling/'],
    success: (response) => `Billing of ${month} created ${response.lines?.length ?? 0} lines`,
  });
  const retry = useApiMutation('/api/console/erpBilling/retryErpExport', {
    invalidates: ['/api/console/erpBilling/'],
    success: (response) => `${response.resentLines ?? 0} refused lines go to the ERP again`,
  });

  const latest = runRows[0];
  const errorsOfLatest = latest ? linesPerStatus(latest).ERROR ?? 0 : 0;

  const runColumns = useMemo<GridColDef<Run>[]>(() => [
    { field: 'billingPeriod', headerName: 'Month', width: 90, valueFormatter: (value: string) => value?.replace('_', '/') },
    { field: 'runDate', headerName: 'Run', width: 130, valueFormatter: (value) => formatDateTime(value) },
    { field: 'lines', headerName: 'Lines', width: 70, type: 'number' },
    { field: 'accounts', headerName: 'Accounts', width: 85, type: 'number' },
    { field: 'amount', headerName: 'Amount', width: 115, type: 'number', valueFormatter: (value) => formatMoney(value, 'EUR') },
    {
      field: 'linesPerExportStatus', headerName: 'Lines per export status', flex: 1, minWidth: 230, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%' }}>
          {EXPORT_STATUSES.filter((status) => linesPerStatus(row)[status]).map((status) => (
            <Chip key={status} size="small" variant="outlined" color={status === 'ERROR' ? 'error' : 'default'}
              label={`${enumLabel(status)} ${linesPerStatus(row)[status]}`} />
          ))}
        </Stack>
      ),
    },
    {
      field: 'retry', headerName: '', width: 150, sortable: false,
      renderCell: ({ row }) => (
        <ActionButton
          label="Retry failed"
          disabledReason={linesPerStatus(row).ERROR ? null : 'No refused lines in this month'}
          busy={retry.isPending}
          confirm={`Send the ${linesPerStatus(row).ERROR} refused lines of ${row.billingPeriod} to the ERP again?`}
          onClick={() => retry.mutate({ billingPeriod: row.billingPeriod })}
          testId={`retry-${row.billingPeriod}`}
        />
      ),
    },
  ], [retry]);

  return (
    <Page
      title="ERP billing"
      toolbar={(
        <Stack direction="row" spacing={1} alignItems="center">
          <TextField size="small" type="month" label="Billing month" value={month} onChange={(e) => setMonth(e.target.value)}
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { 'data-testid': 'billing-month' } }} />
          <ActionButton label="Preview billing" busy={previewCall.isPending} disabledReason={!month && 'Choose a month'}
            onClick={() => previewCall.mutate({ year, month: monthNo })} testId="preview-billing" />
          <ActionButton label="Run billing" variant="contained" busy={runCall.isPending} disabledReason={!month && 'Choose a month'}
            confirm={`Run the ERP billing of ${month}? The lines are stored and exported with the next ERP file.`}
            onClick={() => runCall.mutate({ year, month: monthNo })} testId="run-billing" />
        </Stack>
      )}
    >
      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
        <KpiCard label="Last billed month" value={latest?.billingPeriod?.replace('_', '/') ?? '-'}
          hint={latest ? formatDateTime(latest.runDate) : undefined} />
        <KpiCard label="Lines" value={latest?.lines ?? 0} hint={`${latest?.accounts ?? 0} billing accounts`} />
        <KpiCard label="Amount" value={formatNumber(latest?.amount, 2)} hint="EUR" tone="success" />
        <KpiCard label="Refused by the ERP" value={errorsOfLatest} tone={errorsOfLatest ? 'error' : 'default'} hint="lines" />
        <KpiCard label="Records without export" value={withoutExport.data?.consumptionRecords?.length ?? 0}
          tone={withoutExport.data?.consumptionRecords?.length ? 'warning' : 'default'} hint="older than the configured days" />
      </Stack>
      <Section title="Billing runs" testId="billing-runs">
        <QueryState isLoading={runs.isLoading} error={runs.error}>
          <DataTable rows={runRows} columns={runColumns} getRowId={(row) => row.billingPeriod ?? ''} height="auto"
            onRowClick={({ row }) => setSelectedPeriod(row.billingPeriod)} hideFooter />
        </QueryState>
      </Section>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <Section title="Lines per month and export status" flex={2}>
          <Chart option={billingRunsOption(runRows)} height={280} testId="billing-runs-chart" />
        </Section>
        <Section title={`Refused by the ERP in ${period?.replace('_', '/') ?? '-'}`} flex={3} testId="billing-errors">
          <QueryState isLoading={errors.isLoading} error={errors.error}>
            <DataTable rows={errors.data?.lines} columns={errorColumns} getRowId={(row) => row.id ?? 0} height={280} />
          </QueryState>
        </Section>
      </Stack>
      <Section
        title={`Export lines of ${period?.replace('_', '/') ?? '-'}`}
        testId="billing-lines"
        actions={(
          <TextField select size="small" label="Export status" value={exportStatus} onChange={(e) => setExportStatus(e.target.value)}
            sx={{ minWidth: 160 }} slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}>
            <MenuItem value="">All lines</MenuItem>
            {EXPORT_STATUSES.map((status) => <MenuItem key={status} value={status}>{enumLabel(status)}</MenuItem>)}
          </TextField>
        )}
      >
        <QueryState isLoading={lines.isLoading} error={lines.error}>
          <DataTable rows={lines.data?.lines} columns={lineColumns} getRowId={(row) => row.id ?? 0} height={420} />
        </QueryState>
      </Section>
      <Section title="Consumption records without export" testId="records-without-export">
        <QueryState isLoading={withoutExport.isLoading} error={withoutExport.error}>
          <DataTable rows={withoutExport.data?.consumptionRecords} columns={recordColumns} getRowId={(row) => row.id ?? 0}
            height={(withoutExport.data?.consumptionRecords?.length ?? 0) > 8 ? 360 : 'auto'} />
        </QueryState>
      </Section>
      <Dialog open={preview !== null} onClose={() => setPreview(null)} fullWidth maxWidth="xl">
        <DialogTitle>Billing preview of {month}</DialogTitle>
        <DialogContent>
          {preview && preview.length === 0 ? <Alert severity="info">The month has nothing to bill.</Alert> : (
            <>
              <Alert severity="info" sx={{ mb: 1 }}>
                {preview?.length ?? 0} lines, {formatMoney((preview ?? []).reduce((sum, line) => sum + (line.amount ?? 0), 0), 'EUR')}
                {' '}- nothing is stored by the preview.
              </Alert>
              <DataTable rows={(preview ?? []).map((line, index) => ({ ...line, id: index + 1 }))} columns={lineColumns}
                height={480} testId="preview-lines" />
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPreview(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Page>
  );
}
