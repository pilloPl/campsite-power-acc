import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SupplySessionsPage from './SupplySessionsPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const running = {
  id: 11, active: true, stale: false, socketName: 'ISTRIABAY-P05_4', pitchNo: 'B08', campsiteName: 'Camp Istria Bay',
  guestName: 'Pierre Muller', start: '2026-10-17T07:08:00', startMeterKwh: 2856.2, stopMeterKwh: 2858.4, kwh: 2.2,
};
const stale = { ...running, id: 12, stale: true, socketName: 'ARDECHE-P06_1', guestName: 'Lea Martin' };
const incident = {
  id: 3, incidentType: 'NO_TRANSACTION_ID', pedestalCode: 'ISTRIABAY-P06', meterKwh: 3075.2,
  message: 'Meter reading without transaction id on socket 4', createDate: '2026-10-13T10:34:00',
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': { map: { campsites: [] } },
    '/api/console/supply/findSupplySessions': { sessions: [running, stale], total: 2, page: 0, rowsPerPage: 25 },
    '/api/console/supply/findSupplyIncidents': { incidents: [incident], total: 1 },
    '/api/console/supply/stopSupply': { command: { accepted: true, message: 'Supply stopped on socket 4' } },
    '/api/console/supply/closeStaleSession': { session: { ...stale, active: false, stopReason: 'TIMEOUT' } },
    '/api/console/supply/reviewSupplyIncident': { incident: { ...incident, reviewedBy: 'admin' } },
  });
}

describe('supply sessions tab', () => {
  it('lists the running sessions with meter readings and the unreviewed incidents', async () => {
    backend();
    renderWithProviders(<SupplySessionsPage />);

    const table = await screen.findByTestId('sessions-table');
    expect(await within(table).findByText('Pierre Muller')).toBeInTheDocument();
    expect(within(table).getByText('Stale')).toBeInTheDocument();
    expect(await screen.findByText('Meter reading without transaction id on socket 4')).toBeInTheDocument();
  });

  it('stops a running supply remotely after confirmation', async () => {
    const calls = backend();
    renderWithProviders(<SupplySessionsPage />);

    await userEvent.click(await screen.findByTestId('stop-11'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/supply/stopSupply')[0]?.body).toEqual({ sessionId: 11 }));
    expect(await screen.findByTestId('notification')).toHaveTextContent('Supply stopped on socket 4');
  });

  it('closes a stale session instead of stopping it', async () => {
    const calls = backend();
    renderWithProviders(<SupplySessionsPage />);

    await userEvent.click(await screen.findByTestId('close-stale-12'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/supply/closeStaleSession')[0]?.body).toEqual({ sessionId: 12 }));
  });

  it('marks an incident as reviewed with a comment', async () => {
    const calls = backend();
    renderWithProviders(<SupplySessionsPage />);

    await userEvent.click(await screen.findByTestId('review-incident-3'));
    await userEvent.type(screen.getByLabelText('Comment'), 'meter replaced');
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Mark reviewed' }));

    await waitFor(() => expect(calls.callsTo('/api/console/supply/reviewSupplyIncident')[0]?.body)
      .toEqual({ incidentId: 3, comment: 'meter replaced' }));
  });

  it('pages the sessions on the server', async () => {
    const calls = backend();
    renderWithProviders(<SupplySessionsPage />);

    await screen.findByTestId('sessions-table');
    const request = calls.callsTo('/api/console/supply/findSupplySessions').map((call) => call.body as Record<string, unknown>)
      .find((body) => body.activeOnly === true);
    expect(request).toMatchObject({ activeOnly: true, page: 0, rowsPerPage: 25 });
  });
});
