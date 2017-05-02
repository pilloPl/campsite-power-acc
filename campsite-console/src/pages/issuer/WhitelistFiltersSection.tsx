import AddIcon from '@mui/icons-material/Add';
import { Stack } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation, useApiQuery } from '../../api/hooks';
import { ActionButton } from '../../components/ActionButton';
import { DataTable } from '../../components/DataTable';
import { FormDialog, type FormValues } from '../../components/FormDialog';
import { QueryState, Section } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';

type Filter = Schemas['WhitelistFilterDTO'];

export const FILTER_TYPES = [
  { value: 'IMPORT_EXPORT', label: 'Import and export' },
  { value: 'IMPORT', label: 'Card import only' },
  { value: 'EXPORT', label: 'Card export only' },
];

const ALL_PARTNERS = 0;

/**
 * Whitelist filters of the card issuer groups: per group whether its cards take part in the exchange with one partner
 * or all, per direction. The nearest filter above a card's group decides; without any the cards are exchanged.
 */
export function WhitelistFiltersSection({ configureReason, issuerGroups, partners }: {
  configureReason: string | null;
  issuerGroups: Schemas['GroupDTO'][] | undefined;
  partners: Schemas['IssuerPartnerDTO'][] | undefined;
}) {
  const filters = useApiQuery('/api/console/issuer/findWhitelistFilters', {});
  const [editing, setEditing] = useState<Filter | null>(null);
  const save = useApiMutation('/api/console/issuer/saveWhitelistFilter', {
    invalidates: ['/api/console/issuer/findWhitelistFilters'],
    success: (response) => `Whitelist filter of ${response.filter?.groupDisplayName || response.filter?.groupName} saved`,
    onSuccess: () => setEditing(null),
  });
  const remove = useApiMutation('/api/console/issuer/deleteWhitelistFilter', {
    invalidates: ['/api/console/issuer/findWhitelistFilters'],
    success: 'Whitelist filter removed: the group follows the filter above it again',
  });

  const rows = filters.data?.filters ?? [];
  const typeLabel = (type: string | undefined) => FILTER_TYPES.find((option) => option.value === type)?.label ?? type;

  const columns = useMemo<GridColDef<Filter>[]>(() => [
    { field: 'groupDisplayName', headerName: 'Card issuer group', flex: 1, minWidth: 200,
      valueGetter: (_value, row) => row.groupDisplayName || row.groupName },
    { field: 'filterType', headerName: 'Direction', width: 170, valueFormatter: (value) => typeLabel(value) },
    { field: 'issuerPartnerName', headerName: 'Partner', width: 170, valueFormatter: (value) => value || 'All partners' },
    { field: 'synchronizable', headerName: 'Cards exchanged', width: 150,
      renderCell: ({ value }) => <StatusChip status={value ? 'OK' : 'BLOCKED'} label={value ? 'Exchanged' : 'Kept out'} /> },
    {
      field: 'actions', headerName: '', width: 200, sortable: false,
      renderCell: ({ row }) => (
        <Stack direction="row" spacing={1} sx={{ py: 0.5 }}>
          <ActionButton label="Edit" disabledReason={configureReason} onClick={() => setEditing(row)} testId={`edit-filter-${row.id}`} />
          <ActionButton label="Remove" color="error" disabledReason={configureReason} busy={remove.isPending}
            confirm={`Remove the whitelist filter of ${row.groupDisplayName || row.groupName}? Its cards follow the filter above it again.`}
            onClick={() => remove.mutate({ filterId: row.id })} testId={`delete-filter-${row.id}`} />
        </Stack>
      ),
    },
  ], [configureReason, remove]);

  const submit = (values: FormValues) => {
    const partnerId = Number(values.issuerPartnerId ?? ALL_PARTNERS);
    save.mutate({
      filter: {
        id: editing?.id,
        groupId: values.groupId as number,
        filterType: values.filterType as string,
        synchronizable: Boolean(values.synchronizable),
        issuerPartnerId: partnerId === ALL_PARTNERS ? undefined : partnerId,
      },
    });
  };

  return (
    <Section
      title="Whitelist filters"
      testId="whitelist-filters"
      actions={(
        <ActionButton label="Add filter" icon={<AddIcon />} disabledReason={configureReason} testId="add-filter"
          onClick={() => setEditing({ filterType: 'IMPORT_EXPORT', synchronizable: false })} />
      )}
    >
      <QueryState isLoading={filters.isLoading} error={filters.error}>
        <DataTable rows={rows} columns={columns} getRowId={(row) => row.id ?? 0} height={rows.length > 6 ? 300 : 'auto'}
          hideFooter={rows.length <= 25} localeText={{ noRowsLabel: 'No filter: the cards of every issuer group are exchanged' }} />
      </QueryState>
      <FormDialog
        open={editing !== null}
        title={editing?.id ? 'Edit whitelist filter' : 'New whitelist filter'}
        description="A group has one filter. It applies to the group's whole subtree unless a group below has its own."
        fields={[
          { name: 'groupId', label: 'Card issuer group', type: 'select', required: true,
            options: (issuerGroups ?? []).map((group) => ({ value: group.id ?? 0, label: group.displayName || group.name || '' })) },
          { name: 'filterType', label: 'Direction', type: 'select', required: true, options: FILTER_TYPES },
          { name: 'issuerPartnerId', label: 'Partner', type: 'select',
            options: [{ value: ALL_PARTNERS, label: 'All partners' },
              ...(partners ?? []).map((partner) => ({ value: partner.id ?? 0, label: partner.displayName || partner.name || '' }))] },
          { name: 'synchronizable', label: 'Cards of the group are exchanged', type: 'checkbox' },
        ]}
        initialValues={editing ? {
          groupId: editing.groupId, filterType: editing.filterType, synchronizable: editing.synchronizable,
          issuerPartnerId: editing.issuerPartnerId ?? ALL_PARTNERS,
        } : {}}
        submitLabel="Save filter"
        busy={save.isPending}
        onClose={() => setEditing(null)}
        onSubmit={submit}
      />
    </Section>
  );
}
