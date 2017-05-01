import type { EChartsOption } from 'echarts';
import type { Schemas } from '../../api/client';
import { enumLabel } from '../../components/format';

type Timeline = Schemas['QrPayTimelineDTO'];

/** Places of the QR payment states: the start phase on top, the stop and calculation phase below, ERROR at the bottom. */
const POSITIONS: Record<string, [number, number]> = {
  START_REQUESTED: [0, 0],
  SOCKET_START_WAITING: [1, 0],
  SOCKET_START_RECEIVED: [2, 0],
  START_CONFIRMED: [3, 0],
  STOP_REQUESTED: [0, 1],
  SOCKET_STOP_WAITING: [1, 1],
  SOCKET_STOP_RECEIVED: [2, 1],
  CALCULATION_REQUESTED: [3, 1],
  CALCULATION_DONE: [4, 1],
  CLOSED: [5, 1],
  ERROR: [2.5, 2],
};

const VISITED = '#66bb6a';
const CURRENT = '#1b5e20';
const ERROR = '#d32f2f';
const IDLE = '#eeeeee';

/** Pairs of consecutive states the process went through (its path in the state machine). */
export function visitedTransitions(timeline: Timeline | undefined): Set<string> {
  const steps = timeline?.steps ?? [];
  const pairs = new Set<string>();
  for (let i = 1; i < steps.length; i += 1) pairs.add(`${steps[i - 1].state}>${steps[i].state}`);
  return pairs;
}

/** ECharts state diagram: all states and allowed transitions, the visited path green, the current state dark. */
export function stateDiagramOption(timeline: Timeline | undefined): EChartsOption {
  const states = timeline?.states ?? [];
  const visited = new Set((timeline?.steps ?? []).map((step) => step.state));
  const path = visitedTransitions(timeline);
  let extra = 0;
  const place = (state: string): [number, number] => POSITIONS[state] ?? [extra++, 3];
  const colour = (state: string) => {
    if (state === timeline?.currentState) return state === 'ERROR' ? ERROR : CURRENT;
    if (visited.has(state)) return state === 'ERROR' ? '#ef9a9a' : VISITED;
    return IDLE;
  };
  return {
    tooltip: { show: false },
    grid: { left: 75, right: 75, top: 25, bottom: 25 },
    xAxis: { type: 'value' as const, min: 0, max: 5, show: false },
    yAxis: { type: 'value' as const, min: -0.3, max: 2.3, inverse: true, show: false },
    series: [{
      type: 'graph' as const,
      coordinateSystem: 'cartesian2d',
      symbol: 'roundRect',
      symbolSize: [130, 36],
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 8,
      label: { show: true, fontSize: 11 },
      data: states.map((state) => {
        const dark = colour(state) === CURRENT || colour(state) === ERROR;
        return {
          name: state,
          value: place(state),
          itemStyle: { color: colour(state), borderColor: state === timeline?.currentState ? '#000' : '#9e9e9e', borderWidth: 1 },
          label: { formatter: enumLabel(state), color: dark ? '#fff' : '#212121', fontWeight: dark ? ('bold' as const) : ('normal' as const) },
        };
      }),
      links: (timeline?.transitions ?? []).map((transition) => {
        const taken = path.has(`${transition.from}>${transition.to}`);
        return {
          source: transition.from,
          target: transition.to,
          lineStyle: { color: taken ? '#2e7d32' : '#cfcfcf', width: taken ? 3 : 1, curveness: transition.to === 'ERROR' || transition.from === 'ERROR' ? 0.15 : 0.05 },
        };
      }),
    }],
  };
}
