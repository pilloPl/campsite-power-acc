import { Button, InputAdornment, Stack, TextField } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation, useApiQuery } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import { Right } from '../../auth/rights';
import { DataTable } from '../../components/DataTable';
import { FormDialog } from '../../components/FormDialog';
import { enumLabel, formatDate } from '../../components/format';
import { QueryState } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';
import { GuestDetailDialog } from './GuestDetailDialog';

type Guest = Schemas['GuestDTO'];

/** Guest search (name, plate, unit plate, card number) with the guest detail and new guests. */
export function GuestsTab() {
  const { user, hasRight } = useAuth();
  const [input, setInput] = useState('');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<number | null>(null);
  const [creating, setCreating] = useState(false);
  const guests = useApiQuery('/api/console/guest/findGuests', { search });
  const create = useApiMutation('/api/console/guest/createGuest', {
    invalidates: ['/api/console/guest/'],
    success: (response) => `Guest ${response.guest?.fullName ?? ''} created`,
    onSuccess: (response) => {
      setCreating(false);
      if (response.guest?.id) setSelected(response.guest.id);
    },
  });

  const issuers = useMemo(() => {
    const options = new Map<number, string>();
    (guests.data?.guests ?? []).forEach((guest) => {
      if (guest.issuerGroupId) options.set(guest.issuerGroupId, guest.issuerName ?? String(guest.issuerGroupId));
    });
    (user?.scopeGroups ?? []).filter((group) => group.type === 'ISSUER' && group.id)
      .forEach((group) => options.set(group.id as number, group.displayName || group.name || String(group.id)));
    return [...options.entries()].map(([value, label]) => ({ value, label }));
  }, [guests.data, user]);

  const columns = useMemo<GridColDef<Guest>[]>(() => [
    { field: 'fullName', headerName: 'Name', flex: 1, minWidth: 160 },
    { field: 'guestType', headerName: 'Type', width: 95, valueFormatter: (value) => enumLabel(value) },
    { field: 'status', headerName: 'Status', width: 100, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'issuerName', headerName: 'Issuer', width: 140 },
    { field: 'city', headerName: 'City', width: 105 },
    { field: 'countryIso', headerName: 'Country', width: 75 },
    { field: 'carPlate', headerName: 'Car plate', width: 110 },
    { field: 'email', headerName: 'E-mail', width: 205 },
    { field: 'paymentMethod', headerName: 'Payment', width: 100, valueFormatter: (value) => enumLabel(value) },
    { field: 'erpPartnerNo', headerName: 'ERP partner', width: 100 },
    { field: 'createDate', headerName: 'Created', width: 100, valueFormatter: (value) => formatDate(value) },
  ], []);

  return (
    <Stack spacing={2}>
      <Stack direction="row" spacing={2}>
        <TextField size="small" placeholder="Name, plate or card number" value={input} sx={{ width: 340 }}
          onChange={(e) => setInput(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && setSearch(input.trim())}
          slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> } }}
          inputProps={{ 'aria-label': 'Search guests' }} />
        <Button variant="outlined" onClick={() => setSearch(input.trim())}>Search</Button>
        <Stack sx={{ flexGrow: 1 }} />
        <Button variant="contained" startIcon={<AddIcon />} disabled={!hasRight(Right.GUEST_EDIT)} onClick={() => setCreating(true)}>
          New guest
        </Button>
      </Stack>
      <QueryState isLoading={guests.isLoading} error={guests.error}>
        <DataTable rows={guests.data?.guests} columns={columns} height={600} testId="guests-table"
          onRowClick={({ row }) => setSelected(row.id ?? null)} sx={{ cursor: 'pointer' }} />
      </QueryState>
      <GuestDetailDialog guestId={selected} onClose={() => setSelected(null)} />
      <FormDialog
        open={creating}
        title="New guest"
        fields={[
          { name: 'issuerGroupId', label: 'Card issuer', type: 'select', required: true, options: issuers },
          { name: 'guestType', label: 'Type', type: 'select', required: true,
            options: ['TRANSIT', 'SEASONAL'].map((value) => ({ value, label: enumLabel(value) })) },
          { name: 'firstName', label: 'First name' },
          { name: 'name', label: 'Name', required: true },
          { name: 'email', label: 'E-mail' },
          { name: 'phone', label: 'Phone' },
          { name: 'city', label: 'City' },
          { name: 'countryIso', label: 'Country (ISO)' },
          { name: 'carPlate', label: 'Car plate' },
          { name: 'trailerPlate', label: 'Trailer plate' },
          { name: 'paymentMethod', label: 'Payment', type: 'select', required: true,
            options: ['RECEPTION', 'DIRECT_DEBIT'].map((value) => ({ value, label: enumLabel(value) })) },
        ]}
        initialValues={{ issuerGroupId: issuers[0]?.value, guestType: 'TRANSIT', paymentMethod: 'RECEPTION' }}
        submitLabel="Create"
        busy={create.isPending}
        onClose={() => setCreating(false)}
        onSubmit={(values) => create.mutate({
          issuerGroupId: values.issuerGroupId as number, guestType: values.guestType as string, name: values.name as string,
          firstName: values.firstName as string | undefined, email: values.email as string | undefined,
          phone: values.phone as string | undefined, city: values.city as string | undefined,
          countryIso: values.countryIso as string | undefined, carPlate: values.carPlate as string | undefined,
          trailerPlate: values.trailerPlate as string | undefined, paymentMethod: values.paymentMethod as string,
        })}
      />
    </Stack>
  );
}
