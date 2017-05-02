import type { EChartsOption } from 'echarts';
import type { Schemas } from '../../api/client';
import { enumLabel, formatDateTime } from '../../components/format';

type Partner = Schemas['IssuerPartnerDTO'];
type SyncStatus = Schemas['IssuerSyncStatusDTO'];

export const DIRECTIONS = ['CARD_IMPORT', 'CARD_EXPORT', 'RECORD_EXPORT', 'RECORD_IMPORT'] as const;
const DIRECTION_LABELS: Record<string, string> = {
  CARD_IMPORT: 'Import partner cards', CARD_EXPORT: 'Export our cards',
  RECORD_EXPORT: 'Export consumption records', RECORD_IMPORT: 'Import partner records',
};
/** Cell states of the matrix; OFF = direction not switched on for the partner. */
export const CELL_STATES = ['OFF', 'NOT_RUN', 'ERROR', 'OK'] as const;
const CELL_COLOURS = ['#eeeeee', '#bdbdbd', '#d32f2f', '#2e7d32'];

export function directionSwitchedOn(partner: Partner, direction: string): boolean {
  switch (direction) {
    case 'CARD_IMPORT': return Boolean(partner.cardImport);
    case 'CARD_EXPORT': return Boolean(partner.cardExport);
    case 'RECORD_EXPORT': return Boolean(partner.recordExport);
    case 'RECORD_IMPORT': return Boolean(partner.recordImport);
    default: return false;
  }
}

export interface MatrixCell { partner: string; direction: string; state: string; status?: SyncStatus }

/** One cell per partner and direction: the sync status of a switched-on direction, OFF otherwise. */
export function syncMatrix(partners: Partner[], statuses: SyncStatus[]): MatrixCell[] {
  const names = [...new Set([...partners.map((partner) => partner.name ?? ''), ...statuses.map((status) => status.partnerName ?? '')])]
    .filter(Boolean);
  return names.flatMap((name) => DIRECTIONS.map((direction) => {
    const status = statuses.find((candidate) => candidate.partnerName === name && candidate.direction === direction);
    const partner = partners.find((candidate) => candidate.name === name);
    const on = status !== undefined || (partner !== undefined && directionSwitchedOn(partner, direction));
    const state = !on ? 'OFF' : status?.status ?? 'NOT_RUN';
    return { partner: name, direction, state, status };
  }));
}

export function syncMatrixOption(cells: MatrixCell[]): EChartsOption {
  const partners = [...new Set(cells.map((cell) => cell.partner))];
  return {
    grid: { left: 130, right: 20, top: 10, bottom: 70 },
    tooltip: {
      formatter: (params) => {
        const cell = cells[(params as { dataIndex: number }).dataIndex];
        const status = cell.status;
        return [`<b>${cell.partner}</b> - ${DIRECTION_LABELS[cell.direction]}`, `State: ${enumLabel(cell.state)}`,
          status?.lastRun ? `Last run: ${formatDateTime(status.lastRun)}` : '',
          status?.lastSuccess ? `Last success: ${formatDateTime(status.lastSuccess)}` : '',
          status ? `Accepted ${status.itemsAccepted ?? 0}, rejected ${status.itemsRejected ?? 0}` : '',
          status?.message ? `<span style="white-space:normal">${status.message}</span>` : '']
          .filter(Boolean).join('<br/>');
      },
      extraCssText: 'max-width: 420px; white-space: normal;',
    },
    xAxis: { type: 'category', data: DIRECTIONS.map((direction) => DIRECTION_LABELS[direction]), axisLabel: { interval: 0 } },
    yAxis: { type: 'category', data: partners },
    visualMap: {
      type: 'piecewise', orient: 'horizontal', left: 'center', bottom: 0,
      pieces: CELL_STATES.map((state, index) => ({ value: index, label: enumLabel(state), color: CELL_COLOURS[index] })),
    },
    series: [{
      type: 'heatmap',
      name: 'Sync state',
      data: cells.map((cell) => {
        const state = CELL_STATES.indexOf(cell.state as (typeof CELL_STATES)[number]);
        return {
          value: [DIRECTIONS.indexOf(cell.direction as (typeof DIRECTIONS)[number]), partners.indexOf(cell.partner), state],
          label: { color: state >= 2 ? '#fff' : '#424242' },
        };
      }),
      label: {
        show: true,
        textBorderWidth: 0,
        fontSize: 11,
        formatter: (params) => {
          const cell = cells[(params as { dataIndex: number }).dataIndex];
          return cell.status?.lastRun ? `${enumLabel(cell.state)}\n${formatDateTime(cell.status.lastRun)}` : enumLabel(cell.state);
        },
      },
      itemStyle: { borderColor: '#fff', borderWidth: 3 },
    }],
  };
}
