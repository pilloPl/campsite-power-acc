import { Autocomplete, MenuItem, Stack, TextField } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { useMemo, useState } from 'react';
import { useApiMutation, useApiQuery } from '../api/hooks';
import { ActionButton } from '../components/ActionButton';
import { CampsiteSelect } from '../components/CampsiteSelect';
import { isoDay } from '../components/format';
import { Page } from '../components/Page';
import { LINE_COLUMNS, ReportSection, type ReportLine } from './reports/ReportSection';
import {
  accumulatedOption, averagePerDayOption, averagePerHourOption, consumptionOption, topPitchesOption, utilizationOption,
  utilizationShare,
} from './reports/reportCharts';
import type { ExportColumn } from './reports/exportTable';

const UTILIZATION_COLUMNS: ExportColumn<ReportLine>[] = [
  { header: 'Day of week', value: (line) => line.period },
  { header: 'Hour', value: (line) => line.series },
  { header: 'Supplying hours', value: (line) => line.hours },
  { header: 'Monitored socket minutes', value: (line) => line.count },
  { header: 'Utilization %', value: (line) => utilizationShare(line) },
];

const RESOLUTIONS = [{ value: 'MONTH', label: 'Month' }, { value: 'QUARTER', label: 'Quarter' }, { value: 'YEAR', label: 'Year' }];

/**
 * Tab 13: consumption reports of the campsites in scope (averages per hour of day and day of week, utilization
 * heatmap, monthly and quarterly consumption, accumulated kWh per pedestal, top pitches), each exportable.
 */
export default function ReportsPage() {
  const [campsiteGroupId, setCampsiteGroupId] = useState<number | undefined>();
  const [from, setFrom] = useState(isoDay(-30));
  const [to, setTo] = useState(isoDay(0));
  const [timeResolution, setTimeResolution] = useState('MONTH');
  const [chosenPedestals, setChosenPedestals] = useState<string[] | null>(null);

  const map = useApiQuery('/api/console/map/getMapData', {}, { staleTime: 5 * 60_000 });
  const pedestalCodes = useMemo(() => (map.data?.map?.campsites ?? [])
    .filter((campsite) => campsiteGroupId === undefined || campsite.groupId === campsiteGroupId)
    .flatMap((campsite) => (campsite.pedestals ?? []).map((pedestal) => pedestal.code ?? ''))
    .filter(Boolean), [map.data, campsiteGroupId]);
  const pedestals = chosenPedestals ?? pedestalCodes.slice(0, 6);

  const refresh = useApiMutation('/api/console/report/refreshUtilization', {
    invalidates: ['/api/console/report/'],
    success: 'Utilization refreshed from the monitoring log',
  });

  const period = { campsiteGroupId, from, to };
  const stamp = `${from}_${to}`;

  return (
    <Page
      title="Reports"
      toolbar={(
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
          <CampsiteSelect value={campsiteGroupId} onChange={(value) => { setCampsiteGroupId(value); setChosenPedestals(null); }} />
          <TextField size="small" type="date" label="From" value={from} onChange={(e) => setFrom(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} />
          <TextField size="small" type="date" label="To" value={to} onChange={(e) => setTo(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }} />
          <ActionButton label="Refresh views" icon={<RefreshIcon />} busy={refresh.isPending}
            onClick={() => refresh.mutate({})} testId="refresh-utilization" />
        </Stack>
      )}
    >
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <ReportSection title="Average kWh per hour of day" flex={1} fileName={`average-per-hour-${stamp}`}
          request={{ reportType: 'AVERAGE_PER_HOUR_OF_DAY', ...period }} option={averagePerHourOption} height={240}
          testId="report-hour" />
        <ReportSection title="Average kWh per day of week" flex={1} fileName={`average-per-day-${stamp}`}
          request={{ reportType: 'AVERAGE_PER_DAY_OF_WEEK', ...period }} option={averagePerDayOption} height={240}
          testId="report-day" />
      </Stack>
      <ReportSection title="Socket utilization (share of time supplying, per day of week and hour)"
        fileName={`socket-utilization-${stamp}`} request={{ reportType: 'SOCKET_UTILIZATION', ...period }}
        option={utilizationOption} columns={UTILIZATION_COLUMNS} height={280} testId="report-utilization" />
      <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2}>
        <ReportSection title="Monthly consumption" flex={1} fileName="monthly-consumption"
          request={{ reportType: 'MONTHLY_CONSUMPTION', campsiteGroupId }} option={consumptionOption} height={260}
          testId="report-monthly" />
        <ReportSection title="Quarterly consumption" flex={1} fileName="quarterly-consumption"
          request={{ reportType: 'QUARTERLY_CONSUMPTION', campsiteGroupId }} option={consumptionOption} height={260}
          testId="report-quarterly" />
        <ReportSection title="Top pitches by kWh" flex={1} fileName={`top-pitches-${stamp}`}
          request={{ reportType: 'TOP_PITCHES', ...period }} option={topPitchesOption} columns={LINE_COLUMNS}
          height={260} testId="report-top-pitches" />
      </Stack>
      <ReportSection
        title="Accumulated kWh per pedestal"
        fileName={`accumulated-kwh-${stamp}`}
        request={{ reportType: 'ACCUMULATED_KWH_PER_PEDESTAL', ...period, timeResolution, pedestalCodes: pedestals }}
        enabled={pedestals.length > 0}
        option={accumulatedOption}
        height={300}
        testId="report-accumulated"
        actions={(
          <>
            <Autocomplete
              multiple
              size="small"
              limitTags={2}
              options={pedestalCodes}
              value={pedestals}
              onChange={(_, value) => setChosenPedestals(value)}
              renderInput={(params) => <TextField {...params} label="Pedestals" />}
              sx={{ minWidth: 300 }}
            />
            <TextField select size="small" label="Resolution" value={timeResolution}
              onChange={(e) => setTimeResolution(e.target.value)} sx={{ minWidth: 120 }}>
              {RESOLUTIONS.map((resolution) => (
                <MenuItem key={resolution.value} value={resolution.value}>{resolution.label}</MenuItem>
              ))}
            </TextField>
          </>
        )}
      />
    </Page>
  );
}
