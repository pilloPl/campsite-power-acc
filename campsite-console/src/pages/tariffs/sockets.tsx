import { MenuItem, TextField } from '@mui/material';
import { useCampsites } from '../../components/CampsiteSelect';

export interface SocketOption {
  id: number;
  label: string;
  campsite: string;
}

/** Sockets of the scope (from the map data), labelled with campsite, socket code and pitch. */
export function useSocketOptions(): SocketOption[] {
  const { data } = useCampsites();
  return (data?.map?.campsites ?? []).flatMap((campsite) => (campsite.pedestals ?? []).flatMap((pedestal) =>
    (pedestal.sockets ?? []).map((socket) => ({
      id: socket.id as number,
      campsite: campsite.displayName ?? campsite.name ?? '',
      label: `${socket.socketCode ?? socket.id}${socket.pitchNo ? ` (pitch ${socket.pitchNo})` : ''}`,
    }))));
}

export function SocketSelect({ value, onChange, label = 'Socket', testId }: {
  value: number | undefined; onChange: (socketId: number | undefined) => void; label?: string; testId?: string;
}) {
  const sockets = useSocketOptions();
  return (
    <TextField select size="small" label={label} value={sockets.some((s) => s.id === value) ? value : ''}
      onChange={(e) => onChange(e.target.value === '' ? undefined : Number(e.target.value))}
      sx={{ minWidth: 260 }} data-testid={testId}
      slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }}>
      <MenuItem value="">Choose a socket</MenuItem>
      {sockets.map((socket) => (
        <MenuItem key={socket.id} value={socket.id}>{socket.campsite}: {socket.label}</MenuItem>
      ))}
    </TextField>
  );
}
