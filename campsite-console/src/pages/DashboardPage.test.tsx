import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DashboardPage from './DashboardPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const alert = {
  alertKey: 'SOCKET_FAULT:11:1', alertType: 'SOCKET_FAULT', severity: 'ERROR', alertTime: '2026-07-01T10:00:00',
  text: 'Socket P02_4 of pedestal P02: FAULTED', pedestalCode: 'P02', acknowledged: false,
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': { map: { campsites: [{ groupId: 5, displayName: 'Camp Istria Bay' }] } },
    '/api/console/dashboard/getDashboard': {
      dashboard: { activeSessions: 12, kwhToday: 979.6, faultedSockets: 1, reviewQueue: 5, openAlerts: 1 },
    },
    '/api/console/dashboard/findAlerts': { alerts: [alert] },
    '/api/console/dashboard/acknowledgeAlert': { alert: { ...alert, acknowledged: true } },
  });
}

describe('dashboard tab', () => {
  it('shows the key figures of the network', async () => {
    backend();
    renderWithProviders(<DashboardPage />);

    expect(await screen.findByTestId('kpi-active')).toHaveTextContent('12');
    expect(screen.getByTestId('kpis')).toHaveTextContent('979.6');
  });

  it('lists the live alerts and charts the open ones by type', async () => {
    backend();
    renderWithProviders(<DashboardPage />);

    expect(await screen.findByText('Socket P02_4 of pedestal P02: FAULTED')).toBeInTheDocument();
    await waitFor(() => expect(within(screen.getByTestId('alerts-by-type')).getByTestId('echarts-option'))
      .toHaveTextContent('Socket fault'));
  });

  it('acknowledges an alert with a comment', async () => {
    const calls = backend();
    renderWithProviders(<DashboardPage />);

    await userEvent.click(await screen.findByTestId('ack-SOCKET_FAULT:11:1'));
    await userEvent.type(screen.getByLabelText('Comment'), 'technician on the way');
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Acknowledge' }));

    await waitFor(() => expect(calls.callsTo('/api/console/dashboard/acknowledgeAlert')).toHaveLength(1));
    expect(calls.callsTo('/api/console/dashboard/acknowledgeAlert')[0].body)
      .toEqual({ alertKey: 'SOCKET_FAULT:11:1', comment: 'technician on the way' });
    expect(await screen.findByTestId('notification')).toHaveTextContent('Alert acknowledged');
  });
});
