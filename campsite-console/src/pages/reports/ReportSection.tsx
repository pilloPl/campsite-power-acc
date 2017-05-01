import { Alert, Button, ButtonGroup, Tooltip } from '@mui/material';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import type { EChartsOption } from 'echarts';
import type { ReactNode } from 'react';
import type { RequestOf, Schemas } from '../../api/client';
import { useApiQuery } from '../../api/hooks';
import { Chart } from '../../components/Chart';
import { QueryState, Section } from '../../components/Page';
import { exportCsv, exportSpreadsheet, type ExportColumn } from './exportTable';

export type ReportLine = Schemas['ReportLineDTO'];

export const LINE_COLUMNS: ExportColumn<ReportLine>[] = [
  { header: 'Period', value: (line) => line.period },
  { header: 'Series', value: (line) => line.series },
  { header: 'kWh', value: (line) => line.kwh },
  { header: 'Count', value: (line) => line.count },
  { header: 'Hours', value: (line) => line.hours },
];

/** One report of tab 13: reads report/getReport, draws it and exports the shown lines as CSV or Excel. */
export function ReportSection({
  title, request, option, columns = LINE_COLUMNS, fileName, height = 300, flex, actions, enabled = true, testId,
}: {
  title: string;
  request: RequestOf<'/api/console/report/getReport'>;
  option: (lines: ReportLine[]) => EChartsOption;
  columns?: ExportColumn<ReportLine>[];
  fileName: string;
  height?: number;
  flex?: number;
  actions?: ReactNode;
  enabled?: boolean;
  testId?: string;
}) {
  const report = useApiQuery('/api/console/report/getReport', request, { enabled });
  const lines = report.data?.lines ?? [];
  const noData = !report.isLoading && !report.error && lines.length === 0;
  return (
    <Section
      title={title}
      flex={flex}
      testId={testId}
      actions={(
        <>
          {actions}
          <ButtonGroup size="small" variant="outlined" disabled={lines.length === 0}>
            <Tooltip title="Export the lines as CSV">
              <Button startIcon={<FileDownloadIcon />} onClick={() => exportCsv(lines, columns, fileName)}>CSV</Button>
            </Tooltip>
            <Tooltip title="Export the lines as an Excel workbook">
              <Button onClick={() => exportSpreadsheet(lines, columns, fileName)}>XLSX</Button>
            </Tooltip>
          </ButtonGroup>
        </>
      )}
    >
      <QueryState isLoading={report.isLoading} error={report.error}>
        {noData ? <Alert severity="info">No data in the selected period.</Alert> : (
          <Chart option={option(lines)} height={height} />
        )}
      </QueryState>
    </Section>
  );
}
