import type { Schemas } from '../../api/client';

type MapPedestal = Schemas['MapPedestalDTO'];

/** Section of the map load overlay: allowed current of the active sessions against the capacity. */
export type MapSectionLoad = Schemas['MapSectionLoadDTO'];

export const STATUS_COLOURS: Record<string, string> = {
  AVAILABLE: '#2e7d32',
  SUPPLYING: '#1565c0',
  OCCUPIED: '#0288d1',
  FAULTED: '#c62828',
  BREAKER_TRIPPED: '#ad1457',
  UNAVAILABLE: '#6d4c41',
  TIMEOUT: '#757575',
};

const SEVERITY = ['AVAILABLE', 'OCCUPIED', 'SUPPLYING', 'UNAVAILABLE', 'BREAKER_TRIPPED', 'FAULTED'];

/** The status a pedestal marker shows: heartbeat timeout first, otherwise the worst socket status. */
export function pedestalStatus(pedestal: MapPedestal): string {
  if (pedestal.timeout) return 'TIMEOUT';
  let worst = 'AVAILABLE';
  for (const socket of pedestal.sockets ?? []) {
    const status = socket.status ?? 'UNAVAILABLE';
    if (SEVERITY.indexOf(status) > SEVERITY.indexOf(worst)) worst = status;
  }
  return worst;
}

export function statusColour(status: string): string {
  return STATUS_COLOURS[status] ?? '#757575';
}

/** Colour of a section by its load: allocated current of the running supplies against the capacity. */
export function loadColour(share: number | undefined): string {
  if (share === undefined || Number.isNaN(share)) return '#9e9e9e';
  if (share >= 1) return '#d32f2f';
  if (share >= 0.8) return '#f57c00';
  return '#43a047';
}

export function sectionShare(section: MapSectionLoad): number | undefined {
  if (section.loadShare !== undefined && section.loadShare !== null) return section.loadShare;
  if (!section.maxCurrentA) return undefined;
  return (section.allocatedCurrentA ?? 0) / section.maxCurrentA;
}
