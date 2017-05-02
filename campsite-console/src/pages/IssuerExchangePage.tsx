import { Stack, TextField } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SyncIcon from '@mui/icons-material/Sync';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton } from '../components/ActionButton';
import { Chart } from '../components/Chart';
import { DataTable } from '../components/DataTable';
import { FormDialog, type FormValues } from '../components/FormDialog';
import { enumLabel, formatDate, formatDateTime } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';
import { WhitelistFiltersSection } from './issuer/WhitelistFiltersSection';
import { StatusChip } from '../components/StatusChip';
import { syncMatrix, syncMatrixOption } from './issuer/syncMatrix';

type Partner = Schemas['IssuerPartnerDTO'];
type SyncStatus = Schemas['IssuerSyncStatusDTO'];
type Card = Schemas['CampingCardDTO'];

const FLAGS = ['cardImport', 'cardExport', 'recordExport', 'recordImport', 'active'] as const;

const statusColumns: GridColDef<SyncStatus>[] = [
  { field: 'partnerName', headerName: 'Partner', width: 110 },
  { field: 'direction', headerName: 'Direction', width: 115, valueFormatter: (value) => enumLabel(value) },
  { field: 'status', headerName: 'State', width: 80, renderCell: ({ value }) => <StatusChip status={value} /> },
  { field: 'lastRun', headerName: 'Last run', width: 135, valueFormatter: (value) => formatDateTime(value) },
  { field: 'lastSuccess', headerName: 'Last success', width: 135, valueFormatter: (value) => formatDateTime(value) },
  { field: 'items', headerName: 'Items ok / refused', width: 120, sortable: false,
    valueGetter: (_, row) => `${row.itemsAccepted ?? 0} / ${row.itemsRejected ?? 0}` },
  { field: 'message', headerName: 'Message', flex: 1, minWidth: 300 },
];

/**
 * Tab 15: exchange of cards and consumption records with partner card issuers: partners, the sync state matrix
 * (partner x direction), running the exchange now and sending one card to the partners again.
 */
export default function IssuerExchangePage() {
  const { hasRight } = useAuth();
  const mayConfigure = hasRight(Right.CONFIG_EDIT);
  const configureReason = mayConfigure ? null : 'Needs the right CONFIG_EDIT';
  const [editing, setEditing] = useState<Partner | null>(null);
  const [cardSearch, setCardSearch] = useState('');
  const [submittedSearch, setSubmittedSearch] = useState('');

  const partners = useApiQuery('/api/console/issuer/findIssuerPartners', {});
  const statuses = useApiQuery('/api/console/issuer/findIssuerSyncStatuses', {});
  const issuerGroups = useApiQuery('/api/console/group/findManageableGroups', { groupType: 'ISSUER' }, { enabled: mayConfigure });
  const siteGroups = useApiQuery('/api/console/group/findManageableGroups', { groupType: 'SITE' }, { enabled: mayConfigure });
  const cards = useApiQuery('/api/console/card/findCards', { rfidOrNumber: submittedSearch }, { enabled: submittedSearch.length > 0 });

  const save = useApiMutation('/api/console/issuer/saveIssuerPartner', {
    invalidates: ['/api/console/issuer/'],
    success: (response) => `Partner ${response.partner?.name ?? ''} saved`,
    onSuccess: () => setEditing(null),
  });
  const run = useApiMutation('/api/console/issuer/runIssuerExchange', {
    invalidates: ['/api/console/issuer/'],
    success: (response) => {
      const failed = (response.syncStatuses ?? []).filter((status) => status.status === 'ERROR').length;
      return failed ? `Exchange run: ${failed} direction(s) failed, see the matrix` : 'Exchange run with every active partner';
    },
  });
  const resync = useApiMutation('/api/console/issuer/resyncCard', {
    invalidates: ['/api/console/card/findCards'],
    success: (response) => `Card ${response.card?.cardNumber ?? ''} goes to the partners with the next card export`,
  });

  const partnerRows = partners.data?.partners ?? [];
  const statusRows = statuses.data?.syncStatuses ?? [];
  const cells = useMemo(() => syncMatrix(partnerRows, statusRows), [partnerRows, statusRows]);

  const partnerColumns = useMemo<GridColDef<Partner>[]>(() => [
    { field: 'name', headerName: 'Partner', width: 130 },
    { field: 'displayName', headerName: 'Display name', width: 140 },
    { field: 'interfaceVersion', headerName: 'Interface', width: 85 },
    { field: 'endpoint', headerName: 'Endpoint', flex: 1, minWidth: 180 },
    { field: 'issuerGroupName', headerName: 'Cards in', width: 160 },
    { field: 'siteGroupName', headerName: 'Campsites', width: 130 },
    {
      field: 'directions', headerName: 'Directions', width: 180, sortable: false,
      valueGetter: (_, row) => [row.cardImport && 'card import', row.cardExport && 'card export',
        row.recordExport && 'record export', row.recordImport && 'record import'].filter(Boolean).join(', '),
    },
    { field: 'active', headerName: 'Active', width: 80, renderCell: ({ value }) => <StatusChip status={value ? 'ACTIVE' : 'DEACTIVATED'} label={value ? 'Yes' : 'No'} /> },
    {
      field: 'edit', headerName: '', width: 90, sortable: false,
      renderCell: ({ row }) => <ActionButton label="Edit" disabledReason={configureReason} onClick={() => setEditing(row)} testId={`edit-partner-${row.id}`} />,
    },
  ], [configureReason]);

  const cardColumns = useMemo<GridColDef<Card>[]>(() => [
    { field: 'cardNumber', headerName: 'Card number', width: 160 },
    { field: 'cardRfid', headerName: 'RFID', width: 150 },
    { field: 'guestName', headerName: 'Guest', width: 170 },
    { field: 'issuerGroupName', headerName: 'Issuer', width: 170 },
    { field: 'validTo', headerName: 'Valid to', width: 100, valueFormatter: (value) => formatDate(value) },
    { field: 'cardStatus', headerName: 'Status', width: 100, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'partnerSyncState', headerName: 'Partner sync', flex: 1, minWidth: 130, valueFormatter: (value) => enumLabel(value) },
    {
      field: 'resync', headerName: '', width: 120, sortable: false,
      renderCell: ({ row }) => (
        <ActionButton label="Re-sync" disabledReason={hasRight(Right.BILLING_EDIT) ? null : 'Needs the right BILLING_EDIT'}
          busy={resync.isPending} onClick={() => resync.mutate({ cardId: row.id })} testId={`resync-${row.id}`} />
      ),
    },
  ], [hasRight, resync]);

  const groupOptions = (groups: Schemas['GroupDTO'][] | undefined) =>
    (groups ?? []).map((group) => ({ value: group.id ?? 0, label: group.displayName || group.name || String(group.id) }));

  const submitPartner = (values: FormValues) => {
    const partner: Partner = {
      ...(editing ?? {}),
      name: values.name as string,
      displayName: values.displayName as string | undefined,
      interfaceVersion: values.interfaceVersion as string,
      endpoint: values.endpoint as string,
      issuerGroupId: values.issuerGroupId as number,
      siteGroupId: values.siteGroupId ? (values.siteGroupId as number) : undefined,
    };
    FLAGS.forEach((flag) => { partner[flag] = Boolean(values[flag]); });
    save.mutate({ partner });
  };

  return (
    <Page
      title="Issuer exchange"
      toolbar={(
        <ActionButton label="Run exchange now" variant="contained" icon={<SyncIcon />} busy={run.isPending}
          disabledReason={configureReason}
          confirm="Import the cards of the partners and export cards and consumption records to every active partner now?"
          onClick={() => run.mutate({})} testId="run-exchange" />
      )}
    >
      <Section title="Sync state per partner and direction" testId="sync-matrix">
        <QueryState isLoading={statuses.isLoading || partners.isLoading} error={statuses.error ?? partners.error}>
          <Chart option={syncMatrixOption(cells)} height={Math.max(180, 120 + 50 * new Set(cells.map((cell) => cell.partner)).size)} />
        </QueryState>
      </Section>
      <Section title="Import and export runs" testId="sync-statuses">
        <QueryState isLoading={statuses.isLoading} error={statuses.error}>
          <DataTable rows={statusRows} columns={statusColumns} getRowId={(row) => `${row.partnerName}:${row.direction}`}
            height={statusRows.length > 6 ? 300 : 'auto'} hideFooter={statusRows.length <= 25}
            getRowHeight={() => 'auto'} sx={{ bgcolor: 'background.paper', '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 600 },
              '& .MuiDataGrid-cell': { py: 0.75, whiteSpace: 'normal', lineHeight: 1.4 } }} />
        </QueryState>
      </Section>
      <Section
        title="Partner card issuers"
        testId="partners"
        actions={(
          <>
            <ActionButton label="New partner" icon={<AddIcon />} disabledReason={configureReason}
              onClick={() => setEditing({ interfaceVersion: 'V2', active: true, cardImport: true, recordExport: true })}
              testId="new-partner" />
          </>
        )}
      >
        <QueryState isLoading={partners.isLoading} error={partners.error}>
          <DataTable rows={partnerRows} columns={partnerColumns} getRowId={(row) => row.id ?? 0}
            height={partnerRows.length > 8 ? 360 : 'auto'} hideFooter={partnerRows.length <= 25} />
        </QueryState>
      </Section>
      <WhitelistFiltersSection configureReason={configureReason} issuerGroups={issuerGroups.data?.groups}
        partners={partners.data?.partners} />
      <Section
        title="Re-sync a card with the partners"
        testId="resync"
        actions={(
          <form onSubmit={(e) => { e.preventDefault(); setSubmittedSearch(cardSearch.trim()); }}>
            <Stack direction="row" spacing={1}>
              <TextField size="small" label="Card number or RFID" value={cardSearch} onChange={(e) => setCardSearch(e.target.value)}
                sx={{ minWidth: 260 }} slotProps={{ htmlInput: { 'data-testid': 'card-search' } }} />
              <ActionButton label="Find card" variant="contained" disabledReason={!cardSearch.trim() && 'Enter a card number or RFID'}
                onClick={() => setSubmittedSearch(cardSearch.trim())} testId="find-card" />
            </Stack>
          </form>
        )}
      >
        {submittedSearch ? (
          <QueryState isLoading={cards.isLoading} error={cards.error}>
            <DataTable rows={cards.data?.cards} columns={cardColumns} getRowId={(row) => row.id ?? 0} height="auto"
              hideFooter localeText={{ noRowsLabel: `No card ${submittedSearch}` }} />
          </QueryState>
        ) : (
          <span>Find a camping card to send it to the partner card issuers again with the next card export
            (a change for a known card, an add for an unknown one, a delete for a blocked one).</span>
        )}
      </Section>
      <FormDialog
        open={editing !== null}
        title={editing?.id ? `Edit partner ${editing.name}` : 'New partner card issuer'}
        initialValues={editing ? {
          name: editing.name, displayName: editing.displayName, interfaceVersion: editing.interfaceVersion,
          endpoint: editing.endpoint, issuerGroupId: editing.issuerGroupId, siteGroupId: editing.siteGroupId,
          ...Object.fromEntries(FLAGS.map((flag) => [flag, Boolean(editing[flag])])),
        } : undefined}
        fields={[
          { name: 'name', label: 'Name', required: true },
          { name: 'displayName', label: 'Display name' },
          { name: 'interfaceVersion', label: 'Interface', type: 'select', required: true, options: [
            { value: 'V1', label: 'V1 - CSV files in an SFTP directory' }, { value: 'V2', label: 'V2 - REST calls' }] },
          { name: 'endpoint', label: 'Endpoint (directory or base URL)', required: true },
          { name: 'issuerGroupId', label: 'ISSUER group of the partner cards', type: 'select', required: true, options: groupOptions(issuerGroups.data?.groups) },
          { name: 'siteGroupId', label: 'SITE group of the partner campsites', type: 'select',
            options: [{ value: '', label: 'None' }, ...groupOptions(siteGroups.data?.groups)],
            helperText: 'Card export and record import need the SITE group' },
          { name: 'cardImport', label: 'Import partner cards', type: 'checkbox' },
          { name: 'cardExport', label: 'Export our cards', type: 'checkbox' },
          { name: 'recordExport', label: 'Export consumption records', type: 'checkbox' },
          { name: 'recordImport', label: 'Import partner records', type: 'checkbox' },
          { name: 'active', label: 'Active', type: 'checkbox' },
        ]}
        busy={save.isPending}
        onClose={() => setEditing(null)}
        onSubmit={submitPartner}
      />
    </Page>
  );
}
