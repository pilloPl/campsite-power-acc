import Grid from '@mui/material/Grid2';
import EditIcon from '@mui/icons-material/Edit';
import { Alert, Box, Chip, IconButton, Stack, Switch, Tooltip, Typography } from '@mui/material';
import type { GridColDef } from '@mui/x-data-grid';
import { useMemo, useState, type ReactNode } from 'react';
import type { Schemas } from '../../api/client';
import { useApiMutation, useApiQuery } from '../../api/hooks';
import { useAuth } from '../../auth/AuthContext';
import { Right } from '../../auth/rights';
import { ActionButton, NOT_OFFERED } from '../../components/ActionButton';
import { DataTable } from '../../components/DataTable';
import { FormDialog, type FormValues } from '../../components/FormDialog';
import { formatDateTime, formatKwh } from '../../components/format';
import { QueryState } from '../../components/Page';
import { StatusChip } from '../../components/StatusChip';

type Pedestal = Schemas['PedestalDTO'];
type Socket = Schemas['SocketDTO'];
type Location = Schemas['LocationDTO'];

/** Reads every console list that shows pedestals or sockets after a change of one of them. */
export const PEDESTAL_READS = ['/api/console/pedestal/', '/api/console/map/', '/api/console/commissioning/', '/api/console/powerBudget/'];

export const LOCATION_FIELDS = [
  { name: 'name', label: 'Location name', required: true },
  { name: 'campsiteCode', label: 'Campsite code' },
  { name: 'street', label: 'Street' },
  { name: 'houseNumber', label: 'House number' },
  { name: 'zipcode', label: 'Zip code' },
  { name: 'city', label: 'City', required: true },
  { name: 'region', label: 'Region' },
  { name: 'countryIso', label: 'Country (ISO)', required: true },
  { name: 'latitude', label: 'Latitude', type: 'number' as const },
  { name: 'longitude', label: 'Longitude', type: 'number' as const },
  { name: 'note', label: 'Note', type: 'textarea' as const },
];

/** Location of the form values (fields of LOCATION_FIELDS), keeping the id and reception hours of the old one. */
export function toLocation(values: FormValues, old?: Location): Location {
  const text = (name: string) => (values[name] === undefined || values[name] === '' ? undefined : String(values[name]));
  const number = (name: string) => (typeof values[name] === 'number' ? (values[name] as number) : undefined);
  return {
    ...old,
    name: text('name'), campsiteCode: text('campsiteCode'), street: text('street'), houseNumber: text('houseNumber'),
    zipcode: text('zipcode'), city: text('city'), region: text('region'), countryIso: text('countryIso'),
    latitude: number('latitude'), longitude: number('longitude'), note: text('note'),
  };
}

function Fact({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Grid size={{ xs: 6, md: 4 }}>
      <Typography variant="caption" color="text.secondary" component="div">{label}</Typography>
      <Typography variant="body2" component="div">{children || '–'}</Typography>
    </Grid>
  );
}

/**
 * Detail of a pedestal: type, firmware, heartbeat, location and its sockets, with the actions of the pedestals tab
 * (edit location, assign pitch, free socket, send card list, uninstall to stock, delete); actions the user may not
 * do are shown disabled with the missing right.
 */
export function PedestalDetail({ pedestalCode, onRemoved }: { pedestalCode: string; onRemoved?: () => void }) {
  const { user, hasRight } = useAuth();
  const canEdit = hasRight(Right.PEDESTAL_EDIT);
  const canCommission = hasRight(Right.PEDESTAL_COMMISSION);
  const query = useApiQuery('/api/console/pedestal/getPedestal', { pedestalCode });
  const pedestal: Pedestal | undefined = query.data?.pedestal;
  const [editingLocation, setEditingLocation] = useState(false);
  const [assigning, setAssigning] = useState<Socket | null>(null);

  const updateLocation = useApiMutation('/api/console/pedestal/updateLocation', {
    invalidates: PEDESTAL_READS, success: 'Location saved', onSuccess: () => setEditingLocation(false),
  });
  const assignPitch = useApiMutation('/api/console/pedestal/assignPitch', {
    invalidates: PEDESTAL_READS, success: (r) => `Socket ${r.socket?.name ?? ''} assigned to pitch ${r.socket?.pitchNo ?? '–'}`,
    onSuccess: () => setAssigning(null),
  });
  const setFreeSocket = useApiMutation('/api/console/pedestal/setFreeSocket', {
    invalidates: PEDESTAL_READS,
    success: (r) => (r.socket?.freeSocket ? `Socket ${r.socket?.name ?? ''} supplies free of charge` : `Socket ${r.socket?.name ?? ''} is billed again`),
  });
  const sendCardList = useApiMutation('/api/console/card/sendLocalCardList', {
    success: (r) => (r.sent ? `Card list sent to ${r.pedestalCode}` : `Card list of ${r.pedestalCode} not sent`),
  });
  const decommission = useApiMutation('/api/console/commissioning/decommissionPedestal', {
    invalidates: PEDESTAL_READS, success: 'Pedestal uninstalled back to the supplier stock',
  });
  const deletePedestal = useApiMutation('/api/console/pedestal/deletePedestal', {
    invalidates: PEDESTAL_READS, success: (r) => `Pedestal ${r.pedestalCode} deleted`, onSuccess: () => onRemoved?.(),
  });

  const columns = useMemo<GridColDef<Socket>[]>(() => [
    { field: 'name', headerName: 'Socket', width: 130 },
    {
      field: 'pitchNo', headerName: 'Pitch', width: 80,
      renderCell: ({ row }) => (
        <Stack direction="row" alignItems="center" spacing={0.5} sx={{ height: '100%' }}>
          <span>{row.pitchNo || '–'}</span>
          <Tooltip title={canEdit ? 'Assign the socket to a pitch' : 'Requires right PEDESTAL_EDIT'}>
            <span>
              <IconButton size="small" disabled={!canEdit} onClick={() => setAssigning(row)} aria-label={`Assign pitch of ${row.name}`}
                data-testid={`assign-pitch-${row.id}`}>
                <EditIcon fontSize="inherit" />
              </IconButton>
            </span>
          </Tooltip>
        </Stack>
      ),
    },
    { field: 'socketType', headerName: 'Type', width: 105 },
    { field: 'meterKwh', headerName: 'Meter', width: 105, valueFormatter: (value) => formatKwh(value) },
    { field: 'status', headerName: 'Status', width: 100, renderCell: ({ value }) => <StatusChip status={value} /> },
    { field: 'lastStatusUpdate', headerName: 'Status since', width: 125, valueFormatter: (value) => formatDateTime(value) },
    {
      field: 'freeSocket', headerName: 'Free', width: 62,
      renderCell: ({ row }) => (
        <Tooltip title={canEdit ? 'Supply free of charge' : 'Requires right PEDESTAL_EDIT'}>
          <span>
            <Switch size="small" checked={Boolean(row.freeSocket)} disabled={!canEdit || setFreeSocket.isPending}
              onChange={(e) => setFreeSocket.mutate({ socketId: row.id, freeSocket: e.target.checked })}
              inputProps={{ 'aria-label': `Free socket ${row.name}` }} />
          </span>
        </Tooltip>
      ),
    },
  ], [canEdit, setFreeSocket]);

  return (
    <QueryState isLoading={query.isLoading} error={query.error}>
      {pedestal && (
        <Stack spacing={2} data-testid="pedestal-detail">
          <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
            <Typography variant="h6">{pedestal.code}</Typography>
            {pedestal.linkTimeout ? <Chip size="small" color="error" label="Heartbeat timeout" />
              : <Chip size="small" color="success" label="Online" />}
            {pedestal.remoteControl && <Chip size="small" variant="outlined" label="Remote control" />}
            {pedestal.deleted && <Chip size="small" color="default" label="Deleted" />}
          </Stack>
          <Grid container spacing={1.5}>
            <Fact label="Type">{[pedestal.type?.manufacturer, pedestal.type?.name].filter(Boolean).join(' ')}</Fact>
            <Fact label="Link generation">{pedestal.type?.linkVersion}</Fact>
            <Fact label="Firmware">{pedestal.firmwareVersion}</Fact>
            <Fact label="Serial number">{pedestal.serialNumber}</Fact>
            <Fact label="Modem ICCID">{pedestal.modemIccid}</Fact>
            <Fact label="Last heartbeat">{formatDateTime(pedestal.lastIncoming)}</Fact>
            <Fact label="Campsite">{pedestal.groupName}</Fact>
            <Fact label="Location">{[pedestal.location?.name, pedestal.location?.note].filter(Boolean).join(' – ')}</Fact>
            <Fact label="Address">{[pedestal.location?.street, pedestal.location?.houseNumber, pedestal.location?.zipcode,
              pedestal.location?.city, pedestal.location?.countryIso].filter(Boolean).join(' ')}</Fact>
            <Fact label="Local card list">
              {pedestal.type?.localCardList ? `yes, up to ${pedestal.type.localCardListCapacity ?? '?'} cards` : 'no'}
            </Fact>
            <Fact label="Billing">{pedestal.billable ? (pedestal.settledByCampsite ? 'billable, settled by the campsite' : 'billable') : 'not billable'}</Fact>
            <Fact label="Listed in the app">{pedestal.listedInApp ? 'yes' : 'no'}</Fact>
          </Grid>
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <ActionButton label="Edit location" onClick={() => setEditingLocation(true)}
              disabledReason={!canEdit && 'Requires right PEDESTAL_EDIT'} />
            <ActionButton label="Send card list now" busy={sendCardList.isPending}
              onClick={() => sendCardList.mutate({ pedestalCode: pedestal.code })}
              disabledReason={!pedestal.type?.localCardList ? 'The pedestal type keeps no local card list' : null}
              testId="send-card-list" />
            <ActionButton label="Request firmware update" onClick={() => undefined} disabledReason={NOT_OFFERED} />
            <ActionButton label="Uninstall to stock" color="warning" busy={decommission.isPending}
              confirm={`Uninstall ${pedestal.code} back to the supplier stock? Its sockets stop taking new supplies.`}
              onClick={() => decommission.mutate({ pedestalId: pedestal.id })}
              disabledReason={!canCommission && 'Requires right PEDESTAL_COMMISSION'} testId="uninstall" />
            <ActionButton label="Delete" color="error" busy={deletePedestal.isPending}
              confirm={`Delete pedestal ${pedestal.code}?`}
              onClick={() => deletePedestal.mutate({ pedestalCode: pedestal.code, ownerUsername: user?.username })}
              disabledReason={!canEdit && 'Requires right PEDESTAL_EDIT'} />
          </Stack>
          {(pedestal.sockets ?? []).length === 0
            ? <Alert severity="info">The pedestal has no sockets.</Alert>
            : <DataTable rows={pedestal.sockets} columns={columns} height="auto" hideFooter testId="socket-table" />}
          <Box>
            <FormDialog
              open={editingLocation}
              title={`Location of ${pedestal.code}`}
              fields={LOCATION_FIELDS}
              initialValues={{ ...(pedestal.location as FormValues | undefined) }}
              busy={updateLocation.isPending}
              onClose={() => setEditingLocation(false)}
              onSubmit={(values) => updateLocation.mutate({
                pedestalCode: pedestal.code, ownerUsername: user?.username, location: toLocation(values, pedestal.location),
              })}
            />
            <FormDialog
              open={assigning !== null}
              title={`Pitch of socket ${assigning?.name ?? ''}`}
              description="The pitch the socket supplies; empty removes the assignment."
              fields={[{ name: 'pitchNo', label: 'Pitch number' }]}
              initialValues={{ pitchNo: assigning?.pitchNo ?? '' }}
              submitLabel="Assign"
              busy={assignPitch.isPending}
              onClose={() => setAssigning(null)}
              onSubmit={(values) => assignPitch.mutate({
                socketId: assigning?.id, pitchNo: values.pitchNo ? String(values.pitchNo) : undefined,
              })}
            />
          </Box>
        </Stack>
      )}
    </QueryState>
  );
}
