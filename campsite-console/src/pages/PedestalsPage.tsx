import { Alert, Box, MenuItem, Stack, TextField, Typography } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { ActionButton, NOT_OFFERED } from '../components/ActionButton';
import { DataTable } from '../components/DataTable';
import { FormDialog } from '../components/FormDialog';
import { formatDateTime } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';
import { StatusChip } from '../components/StatusChip';
import { ACCESS_MODES, InstallWizard } from './pedestals/InstallWizard';
import { LOCATION_FIELDS, PEDESTAL_READS, PedestalDetail, toLocation } from './pedestals/PedestalDetail';

type PedestalSummary = Schemas['PedestalSummaryDTO'];
type Commissioning = Schemas['CommissioningDTO'];

const STATES = [
  { value: '', label: 'All pedestals' },
  { value: 'ERROR', label: 'With error or no heartbeat' },
  { value: 'DISABLED', label: 'With unmonitored sockets' },
];

/** Tab 4: pedestals and sockets, installation from the supplier stock and uninstallation back to it. */
export default function PedestalsPage() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const [state, setState] = useState('');
  const [search, setSearch] = useState('');
  const [installing, setInstalling] = useState(false);
  const [creatingToken, setCreatingToken] = useState(false);
  const selected = params.get('code') ?? undefined;

  const pedestals = useApiQuery('/api/console/pedestal/findPedestalsByState', { state: state || undefined });
  const stock = useApiQuery('/api/console/commissioning/getSupplierStock', {});
  const commissionings = useApiQuery('/api/console/commissioning/findCommissionings', {});
  const createCommissioning = useApiMutation('/api/console/commissioning/createCommissioning', {
    invalidates: PEDESTAL_READS,
    success: (r) => `Installer token ${r.commissioning?.token ?? ''} created`,
    onSuccess: () => setCreatingToken(false),
  });

  const rows = useMemo(() => {
    const all = pedestals.data?.pedestals ?? [];
    const text = search.trim().toLowerCase();
    return text ? all.filter((p) => [p.code, p.type, p.groupName, p.city].some((v) => v?.toLowerCase().includes(text))) : all;
  }, [pedestals.data, search]);

  const select = (code: string | undefined) => setParams(code ? { code } : {}, { replace: true });
  useEffect(() => {
    if (!selected && rows.length > 0) select(rows[0].code);
    // select the first pedestal once the list is there
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rows, selected]);

  const columns = useMemo<GridColDef<PedestalSummary>[]>(() => [
    { field: 'code', headerName: 'Pedestal', width: 130 },
    { field: 'groupName', headerName: 'Campsite', flex: 1, minWidth: 120 },
    { field: 'numberOfSockets', headerName: 'Sockets', width: 70, type: 'number' },
    {
      field: 'socketInError', headerName: 'State', width: 80,
      renderCell: ({ row }) => <StatusChip status={row.deleted ? 'DEACTIVATED' : row.socketInError ? 'ERROR' : 'OK'}
        label={row.deleted ? 'Deleted' : row.socketInError ? 'Error' : 'OK'} />,
    },
  ], []);

  const commissioningColumns = useMemo<GridColDef<Commissioning>[]>(() => [
    { field: 'token', headerName: 'Installer token', width: 150 },
    { field: 'ownerName', headerName: 'Owner', width: 160, valueGetter: (_v, row) => row.ownerName || row.ownerUsername },
    { field: 'location', headerName: 'Location', flex: 1, valueGetter: (_v, row) => [row.location?.name, row.location?.city].filter(Boolean).join(', ') },
    { field: 'accessMode', headerName: 'Access', width: 140 },
    { field: 'createDate', headerName: 'Created', width: 130, valueFormatter: (value) => formatDateTime(value) },
  ], []);

  const stockEntries = stock.data?.pedestals ?? [];
  const toolbar = (
    <Stack direction="row" spacing={1.5} alignItems="center">
      <TextField size="small" label="Search" value={search} onChange={(e) => setSearch(e.target.value)} sx={{ width: 180 }} />
      <TextField select size="small" label="State" value={state} onChange={(e) => setState(e.target.value)} sx={{ minWidth: 220 }}
        slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}>
        {STATES.map((option) => <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>)}
      </TextField>
      <ActionButton label="Install from stock" variant="contained" onClick={() => setInstalling(true)} testId="install-wizard" />
      <ActionButton label="Pedestal types" onClick={() => undefined} disabledReason={`${NOT_OFFERED}: pedestal types are read-only`} />
    </Stack>
  );

  return (
    <Page title="Pedestals & sockets" toolbar={toolbar}>
      <Stack direction="row" spacing={2} alignItems="flex-start">
        <Box sx={{ width: 440, flexShrink: 0 }}>
        <Section title={`Pedestals (${rows.length})`} testId="pedestal-list">
          <QueryState isLoading={pedestals.isLoading} error={pedestals.error}>
            <DataTable rows={rows} columns={columns} height="auto" hideFooterSelectedRowCount
              onRowClick={({ row }) => select(row.code)}
              rowSelectionModel={rows.filter((r) => r.code === selected).map((r) => r.id as number)}
              getRowClassName={({ row }) => (row.code === selected ? 'Mui-selected' : '')} />
          </QueryState>
        </Section>
        </Box>
        <Section title="Pedestal" flex={1}>
          {selected ? <PedestalDetail key={selected} pedestalCode={selected} onRemoved={() => select(undefined)} />
            : <Typography color="text.secondary">Select a pedestal.</Typography>}
        </Section>
      </Stack>
      <Stack direction="row" spacing={2} alignItems="flex-start">
        <Section title="Supplier stock" flex={1} testId="supplier-stock">
          <QueryState isLoading={stock.isLoading} error={stock.error}>
            {stockEntries.length === 0 ? (
              <Alert severity="info">No pedestal of your scope waits in the supplier stock. Uninstalled pedestals return here.</Alert>
            ) : (
              <Stack spacing={0.5}>
                {stockEntries.map((entry) => (
                  <Typography key={entry.id} variant="body2">
                    {entry.pedestalCode} · {entry.pedestalType} · {entry.numberOfSockets} sockets · boot {formatDateTime(entry.lastBoot)}
                  </Typography>
                ))}
              </Stack>
            )}
          </QueryState>
        </Section>
        <Section title="Installer tokens" flex={2}
          actions={<ActionButton label="New installer token" onClick={() => setCreatingToken(true)} />}>
          <QueryState isLoading={commissionings.isLoading} error={commissionings.error}>
            {(commissionings.data?.commissionings ?? []).length === 0
              ? <Alert severity="info">No open commissioning. An installer token lets an installer commission pedestals on site.</Alert>
              : <DataTable rows={commissionings.data?.commissionings} columns={commissioningColumns} height="auto" hideFooter />}
          </QueryState>
        </Section>
      </Stack>
      <InstallWizard open={installing} stock={stockEntries} onClose={() => setInstalling(false)} />
      <FormDialog
        open={creatingToken}
        title="New installer token"
        description="The installer commissions the pedestals of the location with the token."
        fields={[
          { name: 'ownerUsername', label: 'Affiliate owner (username)', required: true },
          { name: 'accessMode', label: 'Access mode', type: 'select', options: ACCESS_MODES },
          { name: 'comments', label: 'Comments', type: 'textarea' },
          ...LOCATION_FIELDS.filter((field) => field.type !== 'textarea'),
        ]}
        initialValues={{ ownerUsername: user?.username, accessMode: 'HOUSE_CARDS_ONLY' }}
        submitLabel="Create"
        busy={createCommissioning.isPending}
        onClose={() => setCreatingToken(false)}
        onSubmit={(values) => createCommissioning.mutate({
          ownerUsername: String(values.ownerUsername), accessMode: String(values.accessMode ?? 'HOUSE_CARDS_ONLY'),
          comments: values.comments ? String(values.comments) : undefined, listedInApp: true, location: toLocation(values),
        })}
      />
    </Page>
  );
}
