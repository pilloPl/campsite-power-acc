import { MenuItem, TextField } from '@mui/material';
import { useApiQuery } from '../api/hooks';

/** Campsites in the scope of the logged-in user (from the map data, which every console role may read). */
export function useCampsites() {
  const query = useApiQuery('/api/console/map/getMapData', {}, { staleTime: 5 * 60_000 });
  const campsites = (query.data?.map?.campsites ?? []).map((campsite) => ({
    groupId: campsite.groupId as number,
    name: campsite.displayName || campsite.name || String(campsite.groupId),
  }));
  return { ...query, campsites };
}

/** The campsite with the most supplying sockets: the default of the views of one campsite. */
export function busiestCampsite<C extends { pedestals?: { sockets?: { activeSessionId?: number }[] }[] }>(
  campsites: C[],
): C | undefined {
  const supplying = (campsite: C) => (campsite.pedestals ?? [])
    .reduce((sum, pedestal) => sum + (pedestal.sockets ?? []).filter((socket) => socket.activeSessionId).length, 0);
  return campsites.reduce<C | undefined>((best, campsite) => (!best || supplying(campsite) > supplying(best) ? campsite : best),
    undefined);
}

/** Filter by campsite; empty value = all campsites of the scope. */
export function CampsiteSelect({ value, onChange, allowAll = true, label = 'Campsite' }: {
  value: number | undefined;
  onChange: (groupId: number | undefined) => void;
  allowAll?: boolean;
  label?: string;
}) {
  const { campsites } = useCampsites();
  return (
    <TextField
      select
      size="small"
      label={label}
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value === '' ? undefined : Number(e.target.value))}
      sx={{ minWidth: 220 }}
      slotProps={{ select: { displayEmpty: allowAll }, inputLabel: { shrink: true } }}
      data-testid="campsite-select"
    >
      {allowAll && <MenuItem value="">All campsites</MenuItem>}
      {campsites.map((campsite) => (
        <MenuItem key={campsite.groupId} value={campsite.groupId}>
          {campsite.name}
        </MenuItem>
      ))}
    </TextField>
  );
}
