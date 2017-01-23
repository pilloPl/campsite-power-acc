import ReactECharts from 'echarts-for-react';
import type { EChartsOption } from 'echarts';

/** Apache ECharts chart or diagram (line, bar, heatmap, tree, graph) of the given height. */
export function Chart({ option, height = 360, testId, onEvents }: {
  option: EChartsOption;
  height?: number;
  testId?: string;
  onEvents?: Record<string, (params: unknown) => void>;
}) {
  return (
    <div data-testid={testId}>
      <ReactECharts option={option} style={{ height, width: '100%' }} notMerge lazyUpdate onEvents={onEvents} />
    </div>
  );
}
