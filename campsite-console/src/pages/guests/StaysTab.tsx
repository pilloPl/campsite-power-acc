import { MenuItem, Stack, TextField } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation, useApiQuery } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import { Right } from '../../auth/rights';
import { ActionButton } from '../../components/ActionButton';
import { DataTable } from '../../components/DataTable';
import { FormDialog } from '../../components/FormDialog';
import { formatDate, formatDateTime, isoDay } from '../../components/format';
import { QueryState } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';
import { CheckOutDialog } from './CheckOutDialog';

type Stay = Schemas['StayDTO'];
type Session = Schemas['SupplySessionDTO'];

/** Stays of the campsites with their open supply sessions; check-out and extension of a stay. */
export function StaysTab({ campsiteGroupId }: { campsiteGroupId: number | undefined }) {
  const { hasRight } = useAuth();
  const [state, setState] = useState('OPEN');
  const [from, setFrom] = useState(isoDay(-30));
  const [to, setTo] = useState(isoDay(30));
  const [checkingOut, setCheckingOut] = useState<Stay | null>(null);
  const [extending, setExtending] = useState<Stay | null>(null);

  const stays = useApiQuery('/api/console/guest/findStays', { campsiteGroupId, state: state || undefined, from, to });
  const active = useApiQuery('/api/console/supply/findSupplySessions', { campsiteGroupId, activeOnly: true, page: 0, rowsPerPage: 500 });
  const extend = useApiMutation('/api/console/guest/changeDeparture', {
    invalidates: ['/api/console/guest/'],
    success: (response) => `Departure of ${response.stay?.guestName ?? 'the stay'} moved to ${formatDate(response.stay?.departureDate)}`,
    onSuccess: () => setExtending(null),
  });

  const sessionsByStay = useMemo(() => {
    const byStay = new Map<number, Session[]>();
    (active.data?.sessions ?? []).forEach((session) => {
      if (session.stayId == null) return;
      byStay.set(session.stayId, [...(byStay.get(session.stayId) ?? []), session]);
    });
    return byStay;
  }, [active.data]);

  const canCheckOut = hasRight(Right.STAY_CHECKOUT);
  const canEdit = hasRight(Right.GUEST_EDIT);

  const columns = useMemo<GridColDef<Stay>[]>(() => [
    { field: 'guestName', headerName: 'Guest', flex: 1, minWidth: 130 },
    { field: 'campsiteName', headerName: 'Campsite', width: 135 },
    { field: 'pitchNo', headerName: 'Pitch', width: 60 },
    { field: 'unitPlate', headerName: 'Unit', width: 95 },
    { field: 'cardNumber', headerName: 'Card', width: 100 },
    { field: 'arrivalDate', headerName: 'Arrival', width: 95, valueFormatter: (value) => formatDate(value) },
    { field: 'departureDate', headerName: 'Departure', width: 95, valueFormatter: (value) => formatDate(value) },
    { field: 'nights', headerName: 'Nights', width: 65, type: 'number' },
    {
      field: 'state', headerName: 'State', width: 105,
      valueGetter: (_value, row) => (row.checkOutDate ? 'CHECKED_OUT' : 'OPEN'),
      renderCell: ({ value }) => <StatusChip status={value} />,
    },
    {
      field: 'openSessions', headerName: 'Open supply', width: 105, type: 'number',
      valueGetter: (_value, row) => sessionsByStay.get(row.id ?? -1)?.length ?? 0,
      renderCell: ({ value }) => (value ? <StatusChip status="SUPPLYING" label={`${value} running`} /> : ''),
    },
    { field: 'checkOutDate', headerName: 'Checked out', width: 135, valueFormatter: (value) => formatDateTime(value) },
    {
      field: 'actions', headerName: 'Actions', width: 195, sortable: false, filterable: false,
      renderCell: ({ row }) => (row.checkOutDate ? null : (
        <Stack direction="row" spacing={1} alignItems="center" sx={{ height: '100%' }}>
          <ActionButton label="Check out" color="warning" onClick={() => setCheckingOut(row)}
            disabledReason={!canCheckOut && 'Requires the right STAY_CHECKOUT'} testId={`checkout-${row.id}`} />
          <ActionButton label="Extend" onClick={() => setExtending(row)}
            disabledReason={!canEdit && 'Requires the right GUEST_EDIT'} testId={`extend-${row.id}`} />
        </Stack>
      )),
    },
  ], [sessionsByStay, canCheckOut, canEdit]);

  // stays with running supplies first (the grid sorts again only when its rows change)
  const sortedStays = [...(stays.data?.stays ?? [])].sort((a, b) =>
    (sessionsByStay.get(b.id ?? -1)?.length ?? 0) - (sessionsByStay.get(a.id ?? -1)?.length ?? 0));

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
        <TextField select size="small" label="State" value={state} onChange={(e) => setState(e.target.value)} sx={{ minWidth: 150 }}
          slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }} data-testid="stay-state">
          <MenuItem value="">All</MenuItem>
          <MenuItem value="OPEN">Open</MenuItem>
          <MenuItem value="CHECKED_OUT">Checked out</MenuItem>
        </TextField>
        <TextField size="small" type="date" label="From" value={from} onChange={(e) => setFrom(e.target.value)}
          slotProps={{ inputLabel: { shrink: true } }} />
        <TextField size="small" type="date" label="To" value={to} onChange={(e) => setTo(e.target.value)}
          slotProps={{ inputLabel: { shrink: true } }} />
      </Stack>
      <QueryState isLoading={stays.isLoading} error={stays.error}>
        <DataTable rows={sortedStays} columns={columns} height={600} testId="stays-table"
          columnVisibilityModel={{ checkOutDate: state !== 'OPEN', actions: state !== 'CHECKED_OUT' }}
          initialState={{
            pagination: { paginationModel: { pageSize: 25 } },
            sorting: { sortModel: [{ field: 'openSessions', sort: 'desc' }] },
          }} />
      </QueryState>
      <CheckOutDialog stay={checkingOut} openSessions={sessionsByStay.get(checkingOut?.id ?? -1) ?? []}
        onClose={() => setCheckingOut(null)} />
      <FormDialog
        open={extending !== null}
        title={`Extend the stay of ${extending?.guestName ?? ''}`}
        description="The card validity follows the new departure."
        fields={[{ name: 'departure', label: 'New departure', type: 'date', required: true }]}
        initialValues={{ departure: extending?.departureDate }}
        submitLabel="Change departure"
        busy={extend.isPending}
        onClose={() => setExtending(null)}
        onSubmit={(values) => extend.mutate({ stayId: extending?.id, departure: values.departure as string })}
      />
    </Stack>
  );
}
