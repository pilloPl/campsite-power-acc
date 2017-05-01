import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ReportsPage from './ReportsPage';
import { utilizationShare } from './reports/reportCharts';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const LINES: Record<string, unknown[]> = {
  AVERAGE_PER_HOUR_OF_DAY: [{ period: '07', kwh: 20.5, count: 24 }],
  AVERAGE_PER_DAY_OF_WEEK: [{ period: 'MONDAY', kwh: 55.2, count: 24 }],
  SOCKET_UTILIZATION: [{ period: 'MONDAY', series: '07', count: 6000, hours: 50 }],
  MONTHLY_CONSUMPTION: [{ period: '2026_09', kwh: 3109.96, count: 303 }],
  QUARTERLY_CONSUMPTION: [{ period: '2026_Q3', kwh: 3109.96, count: 303 }],
  ACCUMULATED_KWH_PER_PEDESTAL: [
    { period: '2026_09', series: 'P01', kwh: 400 }, { period: '2026_10', series: 'P01', kwh: 100 },
    { period: '2026_09', series: 'ALL', kwh: 400 },
  ],
  TOP_PITCHES: [{ period: 'B02', series: 'ISTRIABAY', kwh: 1060.4, count: 20 }],
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': {
      map: { campsites: [{ groupId: 5, displayName: 'Camp Istria Bay', pedestals: [{ code: 'P01' }, { code: 'P02' }] }] },
    },
    '/api/console/report/getReport': (body: { reportType: string }) => ({ reportType: body.reportType, lines: LINES[body.reportType] ?? [] }),
    '/api/console/report/refreshUtilization': { refreshed: true },
  });
}

const readBlob = (blob: Blob) => new Promise<string>((resolve) => {
  const reader = new FileReader();
  reader.onload = () => resolve(String(reader.result));
  reader.readAsText(blob);
});

const optionOf = (testId: string) => JSON.parse(within(screen.getByTestId(testId)).getByTestId('echarts-option').textContent ?? '{}');

describe('reports tab', () => {
  it('draws every report from the lines of the backend', async () => {
    backend();
    renderWithProviders(<ReportsPage />);

    await waitFor(() => expect(within(screen.getByTestId('report-monthly')).getByTestId('echarts-option')).toBeInTheDocument());
    expect(optionOf('report-monthly').xAxis.data).toEqual(['09/2026']);
    expect(optionOf('report-quarterly').xAxis.data).toEqual(['Q3 2026']);
    await waitFor(() => expect(within(screen.getByTestId('report-accumulated')).getByTestId('echarts-option')).toBeInTheDocument());
    expect(optionOf('report-accumulated').series).toEqual([expect.objectContaining({ name: 'P01', data: [400, 500] })]);
  });

  it('colours the utilization heatmap by the share of time supplying (hours * 60 / socket minutes)', async () => {
    backend();
    renderWithProviders(<ReportsPage />);

    await waitFor(() => expect(within(screen.getByTestId('report-utilization')).getByTestId('echarts-option')).toBeInTheDocument());
    expect(optionOf('report-utilization').series[0].data).toEqual([[7, 0, 50]]);
    expect(utilizationShare({ hours: 50, count: 6000 })).toBe(50);
  });

  it('asks the accumulated kWh for the pedestals of the scope', async () => {
    const calls = backend();
    renderWithProviders(<ReportsPage />);

    await waitFor(() => expect(calls.callsTo('/api/console/report/getReport')
      .some((call) => (call.body as { reportType: string }).reportType === 'ACCUMULATED_KWH_PER_PEDESTAL')).toBe(true));
    const accumulated = calls.callsTo('/api/console/report/getReport')
      .find((call) => (call.body as { reportType: string }).reportType === 'ACCUMULATED_KWH_PER_PEDESTAL');
    expect(accumulated?.body).toMatchObject({ timeResolution: 'MONTH', pedestalCodes: ['P01', 'P02'] });
  });

  it('exports the lines of a report as CSV', async () => {
    backend();
    const createObjectURL = vi.fn(() => 'blob:report');
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL, revokeObjectURL: vi.fn() }));
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    renderWithProviders(<ReportsPage />);

    const monthly = await screen.findByTestId('report-monthly');
    await waitFor(() => expect(within(monthly).getByRole('button', { name: 'Export the lines as CSV' })).toBeEnabled());
    await userEvent.click(within(monthly).getByRole('button', { name: 'Export the lines as CSV' }));

    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(click).toHaveBeenCalledTimes(1);
    const blob = (createObjectURL.mock.calls[0] as unknown as [Blob])[0];
    expect(await readBlob(blob)).toBe('Period,Series,kWh,Count,Hours\r\n2026_09,,3109.96,303,\r\n');
  });

  it('refreshes the materialized utilization view', async () => {
    const calls = backend();
    renderWithProviders(<ReportsPage />);

    await userEvent.click(screen.getByTestId('refresh-utilization'));

    await waitFor(() => expect(calls.callsTo('/api/console/report/refreshUtilization')).toHaveLength(1));
    expect(await screen.findByTestId('notification')).toHaveTextContent('Utilization refreshed');
  });
});
