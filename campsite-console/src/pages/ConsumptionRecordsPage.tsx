import { MenuItem, Stack, TextField } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton } from '../components/ActionButton';
import { CampsiteSelect, useCampsites } from '../components/CampsiteSelect';
import { DataTable } from '../components/DataTable';
import { enumLabel, formatDateTime, formatNumber, formatMoney, isoDay } from '../components/format';
import { KpiCard } from '../components/KpiCard';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';
import { ReviewDialog } from './records/ReviewDialog';

type ConsumptionRecord = Schemas['ConsumptionRecordDTO'];

const VALIDATION_STATES = ['VALID', 'SUSPECT', 'CONFIRMED', 'REJECTED'];
const PAYMENT_CHANNELS = ['CARD', 'HOUSE_CARD', 'QR_APP'];
const EXPORT_STATES = ['NEW', 'WORK', 'TRANSFERRED', 'PROCESSED', 'ERROR', 'REVIEW', 'IGNORED', 'DUPLICATE'];
/** The backend answers at most 500 rows per page. */
const MAX_ROWS = 500;

function FilterSelect({ label, value, values, onChange }: {
  label: string; value: string; values: string[]; onChange: (value: string) => void;
}) {
  return (
    <TextField select size="small" label={label} value={value} onChange={(e) => onChange(e.target.value)}
      sx={{ minWidth: 150 }} slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}>
      <MenuItem value="">All</MenuItem>
      {values.map((option) => <MenuItem key={option} value={option}>{enumLabel(option)}</MenuItem>)}
    </TextField>
  );
}

/** Re-pricing is offered for records whose pricing failed and that are not exported, rejected or suspect. */
export function canReprice(record: ConsumptionRecord): boolean {
  return record.pricingStatus === 'PRICING_FAILED'
    && ['NEW', 'REVIEW'].includes(record.exportStatus ?? '')
    && record.validationState !== 'SUSPECT' && record.validationState !== 'REJECTED';
}

/** Tab 8: priced consumption records with payment channel and export state, and the SUSPECT review queue. */
export default function ConsumptionRecordsPage() {
  const { hasRight } = useAuth();
  const { campsites } = useCampsites();
  const [campsiteGroupId, setCampsiteGroupId] = useState<number | undefined>();
  const [from, setFrom] = useState(isoDay(-14));
  const [to, setTo] = useState(isoDay(0));
  const [validationState, setValidationState] = useState('');
  const [paymentChannel, setPaymentChannel] = useState('');
  const [exportStatus, setExportStatus] = useState('');
  const [reviewing, setReviewing] = useState<ConsumptionRecord | null>(null);

  const records = useApiQuery('/api/console/consumptionRecord/findConsumptionRecords', {
    from, to, validationState: validationState || undefined, page: 0, rowsPerPage: MAX_ROWS,
  });
  const queue = useApiQuery('/api/console/consumptionRecord/findConsumptionRecords', {
    from: isoDay(-365), to: isoDay(1), validationState: 'SUSPECT', page: 0, rowsPerPage: MAX_ROWS,
  });

  const invalidates = ['/api/console/consumptionRecord/', '/api/console/dashboard/'];
  const review = useApiMutation('/api/console/consumptionRecord/reviewConsumptionRecord', {
    invalidates,
    success: (response) => `Record ${response.consumptionRecord?.recordNo ?? ''} ${
      response.consumptionRecord?.validationState === 'CONFIRMED' ? 'approved' : 'rejected'}`,
    onSuccess: () => setReviewing(null),
  });
  const reprice = useApiMutation('/api/console/consumptionRecord/repriceConsumptionRecord', {
    invalidates,
    success: (response) => `Record re-priced: ${formatMoney(response.consumptionRecord?.cost, response.consumptionRecord?.currency)}`,
  });
  const requeue = useApiMutation('/api/console/consumptionRecord/requeueConsumptionRecordExport', {
    invalidates, success: 'Export queued again',
  });

  const campsiteName = campsites.find((campsite) => campsite.groupId === campsiteGroupId)?.name;
  const matchesCampsite = (record: ConsumptionRecord) => !campsiteName || record.campsiteName === campsiteName;
  const rows = useMemo(() => (records.data?.consumptionRecords ?? []).filter((record) =>
    matchesCampsite(record)
    && (!paymentChannel || record.paymentChannel === paymentChannel)
    && (!exportStatus || record.exportStatus === exportStatus)),
  // eslint-disable-next-line react-hooks/exhaustive-deps
  [records.data, campsiteName, paymentChannel, exportStatus]);
  const queueRows = (queue.data?.consumptionRecords ?? []).filter(matchesCampsite);

  const reviewRight = hasRight(Right.CONSUMPTION_RECORD_REVIEW) ? null : 'Needs the right CONSUMPTION_RECORD_REVIEW';
  const billingRight = hasRight(Right.BILLING_EDIT) ? null : 'Needs the right BILLING_EDIT';

  const recordColumn: GridColDef<ConsumptionRecord> = { field: 'recordNo', headerName: 'Record', width: 200 };
  const supplyColumns: GridColDef<ConsumptionRecord>[] = [
    { field: 'plugOut', headerName: 'Plug-out', width: 125, valueFormatter: (value) => formatDateTime(value) },
    { field: 'kwh', headerName: 'kWh', width: 70, type: 'number', valueFormatter: (value) => formatNumber(value, 2) },
    { field: 'cost', headerName: 'Cost', width: 100, type: 'number',
      valueGetter: (_value, row) => row.cost, valueFormatter: (value, row) => formatMoney(value, row.currency) },
  ];
  const baseColumns: GridColDef<ConsumptionRecord>[] = [
    recordColumn,
    { field: 'pedestalCode', headerName: 'Pedestal', width: 115 },
    { field: 'pitchNo', headerName: 'Pitch', width: 60 },
    { field: 'guestName', headerName: 'Guest', width: 130 },
    ...supplyColumns,
  ];

  const queueColumns: GridColDef<ConsumptionRecord>[] = [
    ...baseColumns,
    { field: 'suspectReason', headerName: 'Suspect because', flex: 1, minWidth: 260 },
    {
      field: 'review', headerName: '', width: 110, sortable: false,
      renderCell: ({ row }) => (
        <ActionButton label="Review" variant="contained" color="warning" disabledReason={reviewRight}
          onClick={() => setReviewing(row)} testId={`review-${row.id}`} />
      ),
    },
  ];

  const columns: GridColDef<ConsumptionRecord>[] = [
    recordColumn,
    ...supplyColumns,
    { field: 'paymentChannel', headerName: 'Channel', width: 90, valueFormatter: (value) => enumLabel(value) },
    { field: 'tariffName', headerName: 'Tariff', width: 115 },
    { field: 'pricingStatus', headerName: 'Pricing', width: 92, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'validationState', headerName: 'Validation', width: 95, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'exportStatus', headerName: 'Export', width: 92, renderCell: ({ value }) => <StatusChip status={value} /> },
    {
      field: 'actions', headerName: 'Actions', width: 185, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={0.5} alignItems="center" sx={{ height: '100%', '& .MuiButton-root': { px: 1, minWidth: 0 } }}>
          {row.validationState === 'SUSPECT' ? (
            <ActionButton label="Review" color="warning" disabledReason={reviewRight} onClick={() => setReviewing(row)} />
          ) : (
            <ActionButton label="Re-price" busy={reprice.isPending}
              disabledReason={reviewRight ?? (canReprice(row) ? null : 'Only records whose pricing failed and that are not exported')}
              onClick={() => reprice.mutate({ consumptionRecordId: row.id })} testId={`reprice-${row.id}`} />
          )}
          <ActionButton label="Re-queue" busy={requeue.isPending}
            disabledReason={billingRight ?? (row.exportStatus === 'ERROR' ? null : 'Only exports in ERROR')}
            onClick={() => requeue.mutate({ consumptionRecordId: row.id })} testId={`requeue-${row.id}`} />
        </Stack>
      ),
    },
  ];

  const totalKwh = rows.reduce((sum, record) => sum + (record.kwh ?? 0), 0);
  const totalCost = rows.reduce((sum, record) => sum + (record.cost ?? 0), 0);
  const currency = rows.find((record) => record.currency)?.currency;
  const exportErrors = rows.filter((record) => record.exportStatus === 'ERROR').length;

  return (
    <Page
      title="Consumption records"
      toolbar={(
        <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
          <CampsiteSelect value={campsiteGroupId} onChange={setCampsiteGroupId} />
          <TextField size="small" type="date" label="From" value={from} onChange={(e) => setFrom(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} />
          <TextField size="small" type="date" label="To" value={to} onChange={(e) => setTo(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} />
        </Stack>
      )}
    >
      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
        <KpiCard label="Records" value={rows.length} testId="kpi-records"
          hint={(records.data?.consumptionRecords?.length ?? 0) >= MAX_ROWS ? `first ${MAX_ROWS}, narrow the period` : `${from} - ${to}`} />
        <KpiCard label="Energy" value={formatNumber(totalKwh, 0)} hint="kWh" />
        <KpiCard label="Amount" value={formatNumber(totalCost, 2)} hint={currency ?? ''} tone="success" />
        <KpiCard label="SUSPECT queue" value={queueRows.length} tone={queueRows.length ? 'warning' : 'default'} hint="records" />
        <KpiCard label="Export errors" value={exportErrors} tone={exportErrors ? 'error' : 'default'} hint="records" />
      </Stack>
      <Section title={`SUSPECT review queue (${queueRows.length})`} testId="review-queue">
        <QueryState isLoading={queue.isLoading} error={queue.error}>
          <DataTable rows={queueRows} columns={queueColumns} height={queueRows.length > 5 ? 300 : 'auto'}
            hideFooter={queueRows.length <= 25} localeText={{ noRowsLabel: 'No record waits for review' }} />
        </QueryState>
      </Section>
      <Section
        title="Records"
        testId="records"
        actions={(
          <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
            <FilterSelect label="Validation" value={validationState} values={VALIDATION_STATES} onChange={setValidationState} />
            <FilterSelect label="Payment channel" value={paymentChannel} values={PAYMENT_CHANNELS} onChange={setPaymentChannel} />
            <FilterSelect label="Export" value={exportStatus} values={EXPORT_STATES} onChange={setExportStatus} />
          </Stack>
        )}
      >
        <QueryState isLoading={records.isLoading} error={records.error}>
          <DataTable rows={rows} columns={columns} height={560} />
        </QueryState>
      </Section>
      <ReviewDialog
        record={reviewing}
        busy={review.isPending}
        onClose={() => setReviewing(null)}
        onDecide={(confirmed, comment) => review.mutate({ consumptionRecordId: reviewing?.id, confirmed, comment: comment || undefined })}
      />
    </Page>
  );
}
