import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MonitoringPage from './MonitoringPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const map = {
  map: {
    campsites: [{
      groupId: 5, displayName: 'Camp Istria Bay', pedestals: [{
        id: 1, code: 'ISTRIABAY-P02', linkVersion: 'V2', remoteControl: true, lastIncoming: '2026-10-17T07:00:00',
        sockets: [
          { id: 21, socketNo: 1, name: 'ISTRIABAY-P02_1', pitchNo: 'A05', status: 'SUPPLYING' },
          { id: 24, socketNo: 4, name: 'ISTRIABAY-P02_4', pitchNo: 'A08', status: 'FAULTED' },
        ],
      }],
    }],
  },
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': map,
    '/api/console/monitoring/getStatusReport': {
      lines: [
        { socketId: 21, socketName: 'ISTRIABAY-P02_1', status: 'SUPPLYING', timeInSeconds: 36000 },
        { socketId: 21, socketName: 'ISTRIABAY-P02_1', status: 'SUPPLYING_WITHOUT_CONSUMPTION', timeInSeconds: 7200 },
        { socketId: 24, socketName: 'ISTRIABAY-P02_4', status: 'FAULTED', timeInSeconds: 3600 },
      ],
    },
    '/api/console/monitoring/findSocketStatusLogs': {
      logs: [{ id: 1, socketName: 'ISTRIABAY-P02_4', status: 'FAULTED', errors: ['RCD_TRIPPED'], remoteDate: '2026-10-17T01:57:00' }],
    },
    '/api/console/monitoring/requestStatus': { command: { accepted: true, message: 'Status requested from ISTRIABAY-P02' } },
    '/api/console/monitoring/resetFault': { command: { accepted: true, socketStatus: 'AVAILABLE' } },
    '/api/console/monitoring/resendFaultMail': { command: { accepted: true, message: 'Fault mail sent' } },
  });
}

describe('monitoring and service tab', () => {
  it('charts the hours per socket and status and counts sockets supplying without consumption', async () => {
    backend();
    renderWithProviders(<MonitoringPage />);

    await waitFor(() => expect(within(screen.getByTestId('status-report-chart')).getByTestId('echarts-option'))
      .toHaveTextContent('Supplying without consumption'));
    expect(await screen.findByTestId('kpi-faulted')).toHaveTextContent('1');
  });

  it('shows the error history of the selected pedestal', async () => {
    backend();
    renderWithProviders(<MonitoringPage />);

    expect(await screen.findByText('Rcd tripped')).toBeInTheDocument();
  });

  it('requests the status of a socket by pedestal and socket number', async () => {
    const calls = backend();
    renderWithProviders(<MonitoringPage />);

    await userEvent.click(await screen.findByTestId('request-status-21'));

    await waitFor(() => expect(calls.callsTo('/api/console/monitoring/requestStatus')[0]?.body)
      .toEqual({ pedestalCode: 'ISTRIABAY-P02', socketNo: 1 }));
  });

  it('resets the fault of a faulted socket and resends its fault mail; sockets without fault cannot be reset', async () => {
    const calls = backend();
    renderWithProviders(<MonitoringPage />);

    expect(await screen.findByTestId('reset-fault-21')).toBeDisabled();
    await userEvent.click(screen.getByTestId('reset-fault-24'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));
    await waitFor(() => expect(calls.callsTo('/api/console/monitoring/resetFault')[0]?.body).toEqual({ socketId: 24 }));

    await userEvent.click(screen.getByTestId('resend-mail-24'));
    await waitFor(() => expect(calls.callsTo('/api/console/monitoring/resendFaultMail')[0]?.body).toEqual({ socketId: 24 }));
  });
});
