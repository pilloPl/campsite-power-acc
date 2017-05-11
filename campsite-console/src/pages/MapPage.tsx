import { Box, Dialog, DialogContent, DialogTitle, FormControlLabel, IconButton, LinearProgress, MenuItem, Stack, Switch, TextField, Typography } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import L from 'leaflet';
import { useEffect, useMemo, useState } from 'react';
import { CircleMarker, GeoJSON, MapContainer, Popup, TileLayer, Tooltip, useMap } from 'react-leaflet';
import type { Schemas } from '../api/client';
import { useApiQuery } from '../api/hooks';
import { formatNumber } from '../components/format';
import { Page, QueryState, Section } from '../components/Page';
import { PedestalPopup } from './map/PedestalPopup';
import { loadColour, pedestalStatus, sectionShare, statusColour, STATUS_COLOURS, type MapSectionLoad } from './map/mapStyle';
import { PedestalDetail } from './pedestals/PedestalDetail';
import { enumLabel } from '../components/format';

type CampsiteMap = Schemas['CampsiteMapDTO'];
type MapArea = Schemas['MapAreaDTO'];

interface ParsedArea {
  area: MapArea;
  geometry: GeoJSON.GeoJsonObject;
}

function parseAreas(campsite: CampsiteMap | undefined): ParsedArea[] {
  const parsed: ParsedArea[] = [];
  for (const area of campsite?.areas ?? []) {
    try {
      if (area.geoJson) parsed.push({ area, geometry: JSON.parse(area.geoJson) as GeoJSON.GeoJsonObject });
    } catch {
      // an outline that is not valid GeoJSON is left out
    }
  }
  const order = { CAMPSITE: 0, SECTION: 1, PITCH: 2 } as Record<string, number>;
  return parsed.sort((a, b) => (order[a.area.areaType ?? ''] ?? 3) - (order[b.area.areaType ?? ''] ?? 3));
}

/** Fits the map to the outlines of the campsite (or its centre when it has none). */
function FitToCampsite({ campsite, areas }: { campsite: CampsiteMap | undefined; areas: ParsedArea[] }) {
  const map = useMap();
  useEffect(() => {
    if (!campsite) return;
    const outline = areas.find((entry) => entry.area.areaType === 'CAMPSITE') ?? areas[0];
    if (outline) {
      map.fitBounds(L.geoJSON(outline.geometry).getBounds(), { padding: [24, 24] });
    } else if (campsite.latitude !== undefined && campsite.longitude !== undefined) {
      map.setView([campsite.latitude, campsite.longitude], 17);
    }
  }, [map, campsite, areas]);
  return null;
}

function Legend() {
  return (
    <Stack spacing={0.5}>
      {Object.entries(STATUS_COLOURS).map(([status, colour]) => (
        <Stack key={status} direction="row" spacing={1} alignItems="center">
          <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: colour, border: '2px solid #fff', boxShadow: '0 0 0 1px #999' }} />
          <Typography variant="body2">{status === 'TIMEOUT' ? 'Heartbeat timeout' : enumLabel(status)}</Typography>
        </Stack>
      ))}
      <Typography variant="caption" color="text.secondary" sx={{ pt: 1 }}>
        A pedestal shows its worst socket status. Sections are coloured by the current allowed to running supplies
        against the section capacity.
      </Typography>
    </Stack>
  );
}

/** Tab 2: campsites, sections and pitches on OpenStreetMap with the pedestals and the section load overlay. */
export function busiestCampsite<C extends { pedestals?: { sockets?: { activeSessionId?: number }[] }[] }>(
  campsites: C[],
): C | undefined {
  const supplying = (campsite: C) => (campsite.pedestals ?? [])
    .reduce((sum, pedestal) => sum + (pedestal.sockets ?? []).filter((socket) => socket.activeSessionId).length, 0);
  return campsites.reduce<C | undefined>((best, campsite) => (!best || supplying(campsite) > supplying(best) ? campsite : best),
    undefined);
}

export default function MapPage() {
  const query = useApiQuery('/api/console/map/getMapData', {}, { refetchInterval: 60_000 });
  const campsites = useMemo(() => query.data?.map?.campsites ?? [], [query.data]);
  const sections = useMemo(() => query.data?.map?.sections ?? [], [query.data]);
  const [campsiteId, setCampsiteId] = useState<number | undefined>();
  const [showLoad, setShowLoad] = useState(true);
  const [openCode, setOpenCode] = useState<string | null>(null);

  // without a choice the busiest campsite: the one with the most supplying sockets
  const campsite = campsites.find((candidate) => candidate.groupId === campsiteId) ?? busiestCampsite(campsites);
  const areas = useMemo(() => parseAreas(campsite), [campsite]);
  const sectionById = useMemo(() => new Map(sections.map((section) => [section.id, section])), [sections]);
  const campsiteSections = sections.filter((section) =>
    areas.some((entry) => entry.area.areaType === 'SECTION' && entry.area.powerSectionId === section.id));

  const areaStyle = (area: MapArea): L.PathOptions => {
    switch (area.areaType) {
      case 'CAMPSITE':
        return { color: '#1f4d2c', weight: 3, dashArray: '6 4', fillOpacity: 0.04 };
      case 'SECTION': {
        const section = sectionById.get(area.powerSectionId);
        const colour = showLoad && section ? loadColour(sectionShare(section)) : '#5c6bc0';
        return { color: colour, weight: 2, fillColor: colour, fillOpacity: showLoad ? 0.22 : 0.06 };
      }
      default:
        return { color: '#8d6e63', weight: 1, fillColor: '#fff8e1', fillOpacity: 0.35 };
    }
  };

  const toolbar = (
    <Stack direction="row" spacing={2} alignItems="center">
      <FormControlLabel control={<Switch checked={showLoad} onChange={(e) => setShowLoad(e.target.checked)} />}
        label="Section load" />
      <TextField select size="small" label="Campsite" value={campsite?.groupId ?? ''} sx={{ minWidth: 240 }}
        onChange={(e) => setCampsiteId(Number(e.target.value))} data-testid="map-campsite">
        {campsites.map((candidate) => (
          <MenuItem key={candidate.groupId} value={candidate.groupId}>{candidate.displayName || candidate.name}</MenuItem>
        ))}
      </TextField>
    </Stack>
  );

  return (
    <Page title="Map" toolbar={toolbar}>
      <QueryState isLoading={query.isLoading} error={query.error}>
        <Stack direction="row" spacing={2} alignItems="stretch">
          <Box sx={{ flex: 1, minWidth: 0, height: 740, borderRadius: 1, overflow: 'hidden', border: 1, borderColor: 'divider' }}
            data-testid="map">
            <MapContainer center={[campsite?.latitude ?? 45, campsite?.longitude ?? 13]} zoom={17} maxZoom={20}
              style={{ height: '100%', width: '100%' }} scrollWheelZoom>
              <TileLayer
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                maxNativeZoom={19}
                maxZoom={20}
              />
              <FitToCampsite campsite={campsite} areas={areas} />
              {areas.map(({ area, geometry }) => (
                <GeoJSON key={`${area.id}-${showLoad}`} data={geometry} style={() => areaStyle(area)}>
                  {area.areaType !== 'CAMPSITE' && (
                    <Tooltip sticky>
                      {area.areaType === 'SECTION'
                        ? `${area.name}: ${sectionLabel(sectionById.get(area.powerSectionId))}`
                        : `${area.name ?? `Pitch ${area.pitchNo}`}`}
                    </Tooltip>
                  )}
                </GeoJSON>
              ))}
              {(campsite?.pedestals ?? []).map((pedestal) => {
                const status = pedestalStatus(pedestal);
                if (pedestal.latitude === undefined || pedestal.longitude === undefined) return null;
                return (
                  <CircleMarker key={pedestal.id} center={[pedestal.latitude, pedestal.longitude]} radius={10}
                    pathOptions={{ color: '#fff', weight: 2, fillColor: statusColour(status), fillOpacity: 0.95 }}>
                    <Tooltip direction="top" offset={[0, -8]}>{pedestal.code}</Tooltip>
                    <Popup minWidth={400} maxWidth={460}>
                      <PedestalPopup pedestal={pedestal} onOpen={setOpenCode} />
                    </Popup>
                  </CircleMarker>
                );
              })}
            </MapContainer>
          </Box>
          <Stack spacing={2} sx={{ width: 300, flexShrink: 0 }}>
            <Section title="Legend">
              <Legend />
            </Section>
            <Section title="Section load" testId="section-load">
              <Stack spacing={1.5}>
                {campsiteSections.length === 0 && <Typography variant="body2" color="text.secondary">No power sections.</Typography>}
                {campsiteSections.map((section) => {
                  const share = sectionShare(section) ?? 0;
                  return (
                    <Box key={section.id}>
                      <Stack direction="row" justifyContent="space-between">
                        <Typography variant="body2" sx={{ fontWeight: 500 }}>{section.sectionName}</Typography>
                        <Typography variant="body2" sx={{ color: loadColour(share) }}>{formatNumber(share * 100, 0)} %</Typography>
                      </Stack>
                      <LinearProgress variant="determinate" value={Math.min(100, share * 100)}
                        sx={{ height: 8, borderRadius: 4, my: 0.5, '& .MuiLinearProgress-bar': { bgcolor: loadColour(share) } }} />
                      <Typography variant="caption" color="text.secondary">{sectionLabel(section)}</Typography>
                    </Box>
                  );
                })}
              </Stack>
            </Section>
            <Section title="Pedestals">
              <Typography variant="body2">
                {(campsite?.pedestals ?? []).length} pedestals, {(campsite?.pedestals ?? []).reduce((n, p) => n + (p.sockets ?? []).length, 0)} sockets,
                {' '}{(campsite?.pedestals ?? []).reduce((n, p) => n + (p.sockets ?? []).filter((s) => s.activeSessionId).length, 0)} supplying.
                Click a pedestal for its sockets and remote commands.
              </Typography>
            </Section>
          </Stack>
        </Stack>
      </QueryState>
      <Dialog open={openCode !== null} onClose={() => setOpenCode(null)} maxWidth="lg" fullWidth>
        <DialogTitle sx={{ pr: 6 }}>
          Pedestal {openCode}
          <IconButton aria-label="Close" onClick={() => setOpenCode(null)} sx={{ position: 'absolute', right: 8, top: 8 }}>
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent>{openCode && <PedestalDetail pedestalCode={openCode} onRemoved={() => setOpenCode(null)} />}</DialogContent>
      </Dialog>
    </Page>
  );
}

function sectionLabel(section: MapSectionLoad | undefined): string {
  if (!section) return 'no load data';
  return `${section.allocatedCurrentA ?? 0} A allocated of ${formatNumber(section.maxCurrentA, 0)} A, ${section.activeSessions ?? 0} supplies`;
}
