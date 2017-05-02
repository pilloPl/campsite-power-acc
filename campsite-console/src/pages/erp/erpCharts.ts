import type { EChartsOption } from 'echarts';
import type { Schemas } from '../../api/client';
import { enumLabel } from '../../components/format';

type Run = Schemas['ErpBillingRunDTO'];

export const EXPORT_STATUSES = ['NEW', 'WORK', 'TRANSFERRED', 'PROCESSED', 'IGNORED', 'ERROR'];
const STATUS_COLOURS: Record<string, string> = {
  NEW: '#90caf9', WORK: '#42a5f5', TRANSFERRED: '#7cb342', PROCESSED: '#2e6b3f', IGNORED: '#bdbdbd', ERROR: '#d32f2f',
};

export function linesPerStatus(run: Run): Record<string, number> {
  return (run.linesPerExportStatus ?? {}) as Record<string, number>;
}

/** Billing runs per month: lines stacked by export status, amount as a line on the second axis. */
export function billingRunsOption(runs: Run[]): EChartsOption {
  const ordered = [...runs].sort((a, b) => (a.billingPeriod ?? '').localeCompare(b.billingPeriod ?? ''));
  const statuses = EXPORT_STATUSES.filter((status) => ordered.some((run) => linesPerStatus(run)[status]));
  return {
    grid: { left: 50, right: 60, top: 36, bottom: 56 },
    legend: { bottom: 0 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ordered.map((run) => (run.billingPeriod ?? '').replace('_', '/')) },
    yAxis: [{ type: 'value', name: 'lines' }, { type: 'value', name: 'amount', splitLine: { show: false } }],
    series: [
      ...statuses.map((status) => ({
        type: 'bar' as const, name: enumLabel(status), stack: 'lines', barMaxWidth: 60,
        itemStyle: { color: STATUS_COLOURS[status] }, data: ordered.map((run) => linesPerStatus(run)[status] ?? 0),
      })),
      {
        type: 'line' as const, name: 'Amount', yAxisIndex: 1, itemStyle: { color: '#c77700' },
        data: ordered.map((run) => run.amount ?? 0),
      },
    ],
  };
}
