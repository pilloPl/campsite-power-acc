import type { EChartsOption } from 'echarts';
import { enumLabel } from '../../components/format';
import type { ReportLine } from './ReportSection';

export const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
export const HOURS = Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, '0'));

const GREEN = '#2e6b3f';
const AMBER = '#c77700';
const round = (value: number | null | undefined, digits = 1) => Number((value ?? 0).toFixed(digits));

/** 2026_09 -> 09/2026, 2026_Q3 -> Q3 2026. */
export function periodLabel(period: string | undefined): string {
  if (!period) return '';
  const [year, part] = period.split('_');
  if (!part) return period;
  return part.startsWith('Q') ? `${part} ${year}` : `${part}/${year}`;
}

export function averagePerHourOption(lines: ReportLine[]): EChartsOption {
  const byHour = new Map(lines.map((line) => [line.period, line.kwh]));
  return {
    grid: { left: 50, right: 16, top: 24, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: (value) => `${value} kWh` },
    xAxis: { type: 'category', data: HOURS.map((hour) => `${hour}h`) },
    yAxis: { type: 'value', name: 'avg kWh' },
    series: [{ type: 'bar', name: 'Average kWh', data: HOURS.map((hour) => round(byHour.get(hour), 2)), itemStyle: { color: GREEN } }],
  };
}

export function averagePerDayOption(lines: ReportLine[]): EChartsOption {
  const byDay = new Map(lines.map((line) => [line.period, line.kwh]));
  return {
    grid: { left: 50, right: 16, top: 24, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: (value) => `${value} kWh` },
    xAxis: { type: 'category', data: DAYS.map((day) => enumLabel(day).substring(0, 3)) },
    yAxis: { type: 'value', name: 'avg kWh' },
    series: [{ type: 'bar', name: 'Average kWh', data: DAYS.map((day) => round(byDay.get(day), 2)), itemStyle: { color: AMBER } }],
  };
}

/** Utilization heatmap: share of the monitored socket minutes in which a socket supplied, per day of week and hour. */
export function utilizationShare(line: ReportLine): number {
  return line.count ? round(((line.hours ?? 0) * 60 * 100) / line.count, 1) : 0;
}

export function utilizationOption(lines: ReportLine[]): EChartsOption {
  const data = lines
    .filter((line) => DAYS.includes(line.period ?? '') && HOURS.includes(line.series ?? ''))
    .map((line) => [HOURS.indexOf(line.series ?? ''), DAYS.indexOf(line.period ?? ''), utilizationShare(line)]);
  const max = Math.max(10, ...data.map((point) => point[2]));
  return {
    grid: { left: 60, right: 90, top: 10, bottom: 30 },
    tooltip: {
      formatter: (params) => {
        const value = (params as { value: number[] }).value;
        return `${enumLabel(DAYS[value[1]])} ${HOURS[value[0]]}:00<br/>${value[2]} % of socket time supplying`;
      },
    },
    xAxis: { type: 'category', data: HOURS.map((hour) => `${hour}h`), splitArea: { show: true } },
    yAxis: { type: 'category', data: DAYS.map((day) => enumLabel(day).substring(0, 3)), inverse: true, splitArea: { show: true } },
    visualMap: {
      min: 0, max: Math.ceil(max), calculable: true, orient: 'vertical', right: 0, top: 'center', text: ['%', ''],
      inRange: { color: ['#f1f8e9', '#aed581', '#558b2f', '#1b5e20'] },
    },
    series: [{ type: 'heatmap', name: 'Utilization', data, label: { show: true, fontSize: 9 } }],
  };
}

export function consumptionOption(lines: ReportLine[]): EChartsOption {
  return {
    grid: { left: 60, right: 60, top: 36, bottom: 30 },
    legend: { top: 0 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: lines.map((line) => periodLabel(line.period)) },
    yAxis: [{ type: 'value', name: 'kWh' }, { type: 'value', name: 'records', splitLine: { show: false } }],
    series: [
      { type: 'bar', name: 'kWh', data: lines.map((line) => round(line.kwh)), itemStyle: { color: GREEN }, barMaxWidth: 60 },
      { type: 'line', name: 'Records', yAxisIndex: 1, data: lines.map((line) => line.count ?? 0), itemStyle: { color: AMBER } },
    ],
  };
}

/** Accumulated kWh per pedestal: the running total of every pedestal over the periods (ALL = sum of the pedestals). */
export function accumulatedOption(lines: ReportLine[]): EChartsOption {
  const periods = [...new Set(lines.map((line) => line.period ?? ''))].sort();
  const pedestals = [...new Set(lines.map((line) => line.series ?? ''))].filter((series) => series !== 'ALL').sort();
  const series = pedestals.map((pedestal) => {
    let total = 0;
    return {
      type: 'line' as const,
      name: pedestal,
      smooth: true,
      data: periods.map((period) => {
        total += lines.find((line) => line.period === period && line.series === pedestal)?.kwh ?? 0;
        return round(total);
      }),
    };
  });
  return {
    grid: { left: 60, right: 30, top: 50, bottom: 30 },
    legend: { top: 0, type: 'scroll' },
    tooltip: { trigger: 'axis', valueFormatter: (value) => `${value} kWh` },
    xAxis: { type: 'category', data: periods.map(periodLabel), boundaryGap: false },
    yAxis: { type: 'value', name: 'kWh', nameGap: 12 },
    series,
  };
}

export function topPitchesOption(lines: ReportLine[]): EChartsOption {
  const top = [...lines].sort((a, b) => (b.kwh ?? 0) - (a.kwh ?? 0)).slice(0, 10).reverse();
  return {
    grid: { left: 110, right: 30, top: 10, bottom: 30 },
    tooltip: { trigger: 'axis', valueFormatter: (value) => `${value} kWh` },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: top.map((line) => `${line.series ?? ''} ${line.period ?? ''}`) },
    series: [{ type: 'bar', name: 'kWh', data: top.map((line) => round(line.kwh)), itemStyle: { color: GREEN } }],
  };
}
