import {
  Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState } from 'react';
import type { Schemas } from '../api/client';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { useAuth } from '../auth/AuthContext';
import { Right } from '../auth/rights';
import { ActionButton } from '../components/ActionButton';
import { useCampsites } from '../components/CampsiteSelect';
import { DataTable } from '../components/DataTable';
import { FormDialog } from '../components/FormDialog';
import { formatDateTime, isoDay } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';
import { AssignmentChain } from './tariffs/AssignmentChain';
import { PriceSimulator } from './tariffs/PriceSimulator';
import { useSocketOptions } from './tariffs/sockets';
import { TariffEditorDialog, newTariff } from './tariffs/TariffEditorDialog';
import { TariffElements } from './tariffs/TariffElements';

type Tariff = Schemas['TariffDTO'];

/** Copy of a tariff without ids, saved as a new tariff. */
export function copyOf(tariff: Tariff): Tariff {
  return {
    ...tariff,
    id: undefined,
    defaultTariff: false,
    lastUpdateTime: undefined,
    name: `${tariff.name ?? ''} COPY`,
    elements: (tariff.elements ?? []).map((element) => ({
      ...element, id: undefined, priceComponents: (element.priceComponents ?? []).map((c) => ({ ...c, id: undefined })),
    })),
  };
}

const PUBLISH_REASON = 'A saved tariff is live: it prices supplies once it is a default or assigned to pedestals, '
  + 'the backend has no separate publish step';

function ApplyToPedestalsDialog({ tariff, busy, onApply, onClose }: {
  tariff: Tariff | null; busy?: boolean; onApply: (codes: string[], startDate: string) => void; onClose: () => void;
}) {
  const { data } = useCampsites();
  const pedestals = (data?.map?.campsites ?? []).flatMap((campsite) =>
    (campsite.pedestals ?? []).map((pedestal) => ({ code: pedestal.code ?? '', campsite: campsite.displayName ?? '' })));
  const [codes, setCodes] = useState<string[]>([]);
  const [startDate, setStartDate] = useState(`${isoDay(1)}T00:00`);
  return (
    <Dialog open={tariff !== null} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Apply {tariff?.name} to pedestals</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <TextField select label="Pedestals" size="small" value={codes}
            slotProps={{ select: { multiple: true, renderValue: (selected) => (selected as string[]).join(', ') } }}
            onChange={(e) => setCodes(typeof e.target.value === 'string' ? e.target.value.split(',') : e.target.value as string[])}>
            {pedestals.map((pedestal) => (
              <MenuItem key={pedestal.code} value={pedestal.code}>{pedestal.campsite}: {pedestal.code}</MenuItem>
            ))}
          </TextField>
          <TextField type="datetime-local" label="Valid from" size="small" value={startDate}
            onChange={(e) => setStartDate(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={busy || codes.length === 0 || !startDate}
          onClick={() => onApply(codes, startDate.length === 16 ? `${startDate}:00` : startDate)}>
          Apply
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/** Tab 9: tariffs with their elements, the tariff builder, defaults and assignments, the assignment chain of a socket and the price simulator. */
export default function TariffsPage() {
  const { hasRight } = useAuth();
  const editReason = hasRight(Right.TARIFF_EDIT) ? null : 'Needs the right TARIFF_EDIT';
  const { campsites } = useCampsites();
  const sockets = useSocketOptions();
  const [selectedId, setSelectedId] = useState<number | undefined>();
  const [editing, setEditing] = useState<Tariff | null>(null);
  const [campsiteDefault, setCampsiteDefault] = useState<Tariff | null>(null);
  const [applying, setApplying] = useState<Tariff | null>(null);

  const tariffs = useApiQuery('/api/console/tariff/findTariffs', {});
  const rows = useMemo(() => tariffs.data?.tariffs ?? [], [tariffs.data]);
  const selected = rows.find((tariff) => tariff.id === selectedId) ?? rows.find((tariff) => tariff.defaultTariff) ?? rows[0];

  const invalidates = ['/api/console/tariff/'];
  const save = useApiMutation('/api/console/tariff/saveTariff', {
    invalidates, success: (response) => `Tariff ${response.tariff?.name ?? ''} saved`,
    onSuccess: (response) => { setEditing(null); setSelectedId(response.tariff?.id); },
  });
  const update = useApiMutation('/api/console/tariff/updateTariff', {
    invalidates, success: (response) => `Tariff ${response.tariff?.name ?? ''} updated`, onSuccess: () => setEditing(null),
  });
  const marketDefault = useApiMutation('/api/console/tariff/setMarketDefaultTariff', {
    invalidates, success: (response) => `${response.tariff?.name ?? ''} is the default tariff of market ${response.tariff?.market ?? ''}`,
  });
  const siteDefault = useApiMutation('/api/console/tariff/setCampsiteDefaultTariff', {
    invalidates, success: (response) => `Campsite default tariff: ${response.effectiveDefaultTariff?.name ?? ''}`,
    onSuccess: () => setCampsiteDefault(null),
  });
  const apply = useApiMutation('/api/console/tariff/applyTariffToPedestals', {
    invalidates, success: (response) => `Tariff assigned ${response.assignments?.length ?? 0} time(s)`,
    onSuccess: () => setApplying(null),
  });

  const columns: GridColDef<Tariff>[] = [
    { field: 'name', headerName: 'Tariff', width: 160 },
    { field: 'market', headerName: 'Market', width: 75 },
    {
      field: 'defaultTariff', headerName: 'Default', width: 90,
      renderCell: ({ value }) => (value ? <Chip size="small" color="success" label="Market" /> : null),
    },
    { field: 'priceText', headerName: 'Prices', flex: 1, minWidth: 220 },
    { field: 'lastUpdateTime', headerName: 'Updated', width: 125, valueFormatter: (value) => formatDateTime(value) },
  ];

  return (
    <Page
      title="Tariffs"
      toolbar={(
        <ActionButton label="New tariff" variant="contained" icon={<AddIcon />} disabledReason={editReason}
          onClick={() => setEditing(newTariff(rows[0]?.market, rows[0]?.currency))} testId="new-tariff" />
      )}
    >
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} alignItems="flex-start">
        <Section title="Tariffs" flex={3} testId="tariffs">
          <QueryState isLoading={tariffs.isLoading} error={tariffs.error}>
            <DataTable rows={rows} columns={columns} height="auto" hideFooter={rows.length <= 25}
              onRowClick={({ row }) => setSelectedId(row.id)}
              rowSelectionModel={selected?.id !== undefined ? [selected.id] : []}
              sx={{ '& .MuiDataGrid-row': { cursor: 'pointer' }, bgcolor: 'background.paper' }} />
          </QueryState>
        </Section>
        <Section
          title={selected ? `Tariff builder: ${selected.name}` : 'Tariff builder'}
          flex={2}
          testId="tariff-detail"
          actions={selected && (
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <ActionButton label="Edit" disabledReason={editReason} onClick={() => setEditing(structuredClone(selected))} />
              <ActionButton label="Copy" disabledReason={editReason} onClick={() => setEditing(copyOf(selected))} />
              <ActionButton label="Publish" disabledReason={PUBLISH_REASON} onClick={() => undefined} />
            </Stack>
          )}
        >
          {selected ? (
            <Stack spacing={1.5}>
              {selected.description && <Typography variant="body2" color="text.secondary">{selected.description}</Typography>}
              <TariffElements elements={selected.elements} currency={selected.currency} />
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <ActionButton label="Set as market default" disabledReason={editReason ?? (selected.defaultTariff ? 'Already the market default' : null)}
                  confirm={`Make ${selected.name} the default tariff of market ${selected.market}?`}
                  onClick={() => marketDefault.mutate({ tariffId: selected.id })} />
                <ActionButton label="Set as campsite default" disabledReason={editReason} onClick={() => setCampsiteDefault(selected)} />
                <ActionButton label="Apply to pedestals" disabledReason={editReason} onClick={() => setApplying(selected)} />
              </Stack>
            </Stack>
          ) : (
            <Typography color="text.secondary">No tariff yet.</Typography>
          )}
        </Section>
      </Stack>
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} alignItems="flex-start">
        <Stack sx={{ flex: 1, minWidth: 0 }}>
          <PriceSimulator tariffs={rows} defaultSocketId={sockets[0]?.id} />
        </Stack>
        <Stack sx={{ flex: 1, minWidth: 0 }}>
          <AssignmentChain defaultSocketId={sockets[0]?.id} />
        </Stack>
      </Stack>
      <TariffEditorDialog
        tariff={editing}
        busy={save.isPending || update.isPending}
        onClose={() => setEditing(null)}
        onSave={(tariff) => (tariff.id
          ? update.mutate({ tariffId: tariff.id, tariff })
          : save.mutate({ tariff }))}
      />
      <FormDialog
        open={campsiteDefault !== null}
        title={`Set ${campsiteDefault?.name ?? ''} as campsite default`}
        fields={[{
          name: 'groupId', label: 'Campsite', type: 'select', required: true,
          options: campsites.map((campsite) => ({ value: campsite.groupId, label: campsite.name })),
        }]}
        submitLabel="Set default"
        busy={siteDefault.isPending}
        onClose={() => setCampsiteDefault(null)}
        onSubmit={(values) => siteDefault.mutate({ groupId: Number(values.groupId), tariffId: campsiteDefault?.id })}
      />
      <ApplyToPedestalsDialog
        tariff={applying}
        busy={apply.isPending}
        onClose={() => setApplying(null)}
        onApply={(pedestalCodes, startDate) => apply.mutate({ pedestalCodes, tariffId: applying?.id, startDate })}
      />
    </Page>
  );
}
