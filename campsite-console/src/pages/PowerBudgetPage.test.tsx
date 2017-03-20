import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PowerBudgetPage from './PowerBudgetPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const sectionA = {
  id: 1, sectionName: 'ISTRIA BAY SECTION A', maxCurrentA: 80, algorithmBeanName: 'nominalCurrentSum', algorithmAvailable: true,
  socketCount: 2, allocatedA: 96, activeSessions: 6,
  sockets: [
    { socketId: 11, socketCode: 'ISTRIABAY*P01*1', pedestalCode: 'ISTRIABAY-P01', socketNo: 1, pitchNo: 'A01', status: 'SUPPLYING', ratedCurrentA: 16, allocatedA: 16 },
    { socketId: 12, socketCode: 'ISTRIABAY*P01*2', pedestalCode: 'ISTRIABAY-P01', socketNo: 2, pitchNo: 'A02', status: 'AVAILABLE', ratedCurrentA: 16, allocatedA: 0 },
  ],
};
const sectionB = { ...sectionA, id: 2, sectionName: 'ISTRIA BAY SECTION B', maxCurrentA: 63, allocatedA: 32, sockets: [] };

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/powerBudget/findPowerSections': { sections: [sectionA, sectionB], availableAlgorithms: ['nominalCurrentSum', 'measuredCurrent'] },
    '/api/console/powerBudget/getSectionLoad': {
      load: { sectionId: 1, sectionName: 'ISTRIA BAY SECTION A', maxCurrentA: 80, points: [
        { hour: '2026-10-16T00:00:00', allocatedA: 64, measuredA: 6.2, sessions: 4 },
        { hour: '2026-10-16T01:00:00', allocatedA: 96, measuredA: 7.5, sessions: 6 },
      ] },
    },
    '/api/console/powerBudget/changeSectionCapacity': { section: { ...sectionA, maxCurrentA: 100 } },
    '/api/console/powerBudget/selectAllocationAlgorithm': { section: { ...sectionA, algorithmBeanName: 'measuredCurrent' } },
  });
}

describe('power budget tab', () => {
  it('shows the sections, the load against the capacity and the allocation per socket', async () => {
    const calls = backend();
    renderWithProviders(<PowerBudgetPage />);

    expect(await screen.findByTestId('section-kpis')).toHaveTextContent('80 A');
    expect(screen.getByTestId('section-kpis')).toHaveTextContent('120 % of the capacity');
    const option = within(await screen.findByTestId('load-chart')).getByTestId('echarts-option');
    expect(option).toHaveTextContent('Capacity A');
    expect(option).toHaveTextContent('"2026-10-16T01:00:00",96');
    expect(within(screen.getByTestId('allocation')).getByText('ISTRIABAY*P01*2')).toBeInTheDocument();
    expect(calls.callsTo('/api/console/powerBudget/getSectionLoad')[0].body).toMatchObject({ sectionId: 1 });
  });

  it('loads the other section when it is selected', async () => {
    const calls = backend();
    renderWithProviders(<PowerBudgetPage />);

    await userEvent.click(await screen.findByText('ISTRIA BAY SECTION B'));

    await waitFor(() => expect(calls.callsTo('/api/console/powerBudget/getSectionLoad').some((c) => (c.body as { sectionId: number }).sectionId === 2)).toBe(true));
  });

  it('changes the capacity of a section', async () => {
    const calls = backend();
    renderWithProviders(<PowerBudgetPage />);

    await userEvent.click(await screen.findByTestId('change-capacity'));
    const field = screen.getByLabelText(/Capacity \(A\)/);
    await userEvent.clear(field);
    await userEvent.type(field, '100');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(calls.callsTo('/api/console/powerBudget/changeSectionCapacity')[0]?.body).toEqual({ sectionId: 1, maxCurrentA: 100 }));
  });

  it('selects another allocation algorithm', async () => {
    const calls = backend();
    renderWithProviders(<PowerBudgetPage />);

    await userEvent.click(await screen.findByLabelText('Allocation algorithm'));
    await userEvent.click(await screen.findByRole('option', { name: 'measuredCurrent' }));
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => expect(calls.callsTo('/api/console/powerBudget/selectAllocationAlgorithm')[0]?.body)
      .toEqual({ sectionId: 1, algorithmBeanName: 'measuredCurrent' }));
  });
});
