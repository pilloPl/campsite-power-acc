import { MenuItem, Stack, TextField } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiQuery } from '../../api/hooks';
import { DataTable } from '../../components/DataTable';
import { enumLabel, formatDate, formatMoney, formatNumber } from '../../components/format';
import { QueryState } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';

type Contract = Schemas['SeasonalContractDTO'];

/** Seasonal contracts of the pitches: fixed fee, power package, requested termination. */
export function ContractsTab({ campsiteGroupId }: { campsiteGroupId: number | undefined }) {
  const [status, setStatus] = useState('');
  const contracts = useApiQuery('/api/console/guest/findSeasonalContracts', { status: status || undefined });
  const rows = (contracts.data?.seasonalContracts ?? [])
    .filter((contract) => campsiteGroupId === undefined || contract.siteGroupId === campsiteGroupId);

  const columns = useMemo<GridColDef<Contract>[]>(() => [
    { field: 'guestName', headerName: 'Guest', flex: 1, minWidth: 150 },
    { field: 'campsiteName', headerName: 'Campsite', width: 170 },
    { field: 'pitchNo', headerName: 'Pitch', width: 70 },
    { field: 'status', headerName: 'Status', width: 190, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'tariffName', headerName: 'Tariff', width: 130 },
    { field: 'startDate', headerName: 'Start', width: 100, valueFormatter: (value) => formatDate(value) },
    { field: 'endDate', headerName: 'End', width: 100, valueFormatter: (value) => formatDate(value) },
    { field: 'requestedEndDate', headerName: 'Requested end', width: 115, valueFormatter: (value) => formatDate(value) },
    { field: 'fixedFee', headerName: 'Fixed fee', width: 100, type: 'number', valueFormatter: (value) => formatMoney(value) },
    { field: 'feePeriod', headerName: 'Fee period', width: 100, valueFormatter: (value) => enumLabel(value) },
    { field: 'packageKwh', headerName: 'Package kWh', width: 110, type: 'number', valueFormatter: (value) => formatNumber(value, 0) },
    { field: 'billingAccountNo', headerName: 'Billing account', width: 125 },
  ], []);

  return (
    <Stack spacing={2}>
      <TextField select size="small" label="Status" value={status} onChange={(e) => setStatus(e.target.value)} sx={{ width: 220 }}
        slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}>
        <MenuItem value="">All</MenuItem>
        <MenuItem value="ACTIVE">Active</MenuItem>
        <MenuItem value="TERMINATION_REQUESTED">Termination requested</MenuItem>
        <MenuItem value="ENDED">Ended</MenuItem>
      </TextField>
      <QueryState isLoading={contracts.isLoading} error={contracts.error}>
        <DataTable rows={rows} columns={columns} height={600} testId="contracts-table" />
      </QueryState>
    </Stack>
  );
}
