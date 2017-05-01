import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import QrPaymentsPage, { cancelReason } from './QrPaymentsPage';
import { stateDiagramOption, visitedTransitions } from './qrpay/stateDiagram';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const stuck = {
  processId: 11, orderReference: 'QR-700040', state: 'SOCKET_START_WAITING', socketCode: 'ISTRIABAY*P01*4',
  campsiteName: 'Camp Istria Bay', appUserId: 'app-user-40', requestDate: '2026-10-17T06:00:00', kwh: 0, cost: 0,
  currency: 'EUR', archived: false, stuck: true,
};
const closed = { ...stuck, processId: 12, orderReference: 'QR-700008', state: 'CLOSED', stuck: false, archived: true, kwh: 3.8, cost: 1.73 };
const states = ['START_REQUESTED', 'SOCKET_START_WAITING', 'SOCKET_START_RECEIVED', 'ERROR'];
const timeline = {
  processId: 11, orderReference: 'QR-700040', currentState: 'SOCKET_START_WAITING', archived: false, cancellable: true,
  steps: [
    { rowId: 1, state: 'START_REQUESTED', since: '2026-10-17T06:00:00' },
    { rowId: 2, state: 'SOCKET_START_WAITING', since: '2026-10-17T06:00:01' },
  ],
  states,
  transitions: [
    { from: 'START_REQUESTED', to: 'SOCKET_START_WAITING' }, { from: 'SOCKET_START_WAITING', to: 'SOCKET_START_RECEIVED' },
    { from: 'SOCKET_START_WAITING', to: 'ERROR' },
  ],
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': { map: { campsites: [] } },
    '/api/console/qrPay/findQrPayProcesses': { processes: [stuck, closed], total: 2, page: 0, rowsPerPage: 25 },
    '/api/console/qrPay/getQrPayTimeline': { timeline },
    '/api/console/qrPay/cancelQrPayProcess': { timeline: { ...timeline, currentState: 'ERROR', cancellable: false } },
    '/api/console/qrPay/runQrPayArchive': { archivedRows: 4 },
  });
}

describe('QR payments tab', () => {
  it('lists the processes and marks stuck ones', async () => {
    backend();
    renderWithProviders(<QrPaymentsPage />);

    const table = await screen.findByTestId('qr-processes');
    expect(await within(table).findByText('QR-700040')).toBeInTheDocument();
    expect(within(table).getByText('Stuck')).toBeInTheDocument();
    expect(within(table).getByText('1 stuck')).toBeInTheDocument();
  });

  it('draws the state diagram of the first process with its visited path', async () => {
    const calls = backend();
    renderWithProviders(<QrPaymentsPage />);

    const diagram = await screen.findByTestId('qr-state-diagram');
    expect(within(diagram).getByTestId('echarts-option')).toHaveTextContent('Socket start waiting');
    expect(calls.callsTo('/api/console/qrPay/getQrPayTimeline')[0].body).toEqual({ processId: 11 });
  });

  it('cancels a stuck process after a confirmation', async () => {
    const calls = backend();
    renderWithProviders(<QrPaymentsPage />);

    await screen.findByTestId('cancel-11');
    await waitFor(() => expect(screen.getByTestId('cancel-11')).toBeEnabled());
    await userEvent.click(screen.getByTestId('cancel-11'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/qrPay/cancelQrPayProcess')[0]?.body).toEqual({ processId: 11 }));
    expect(await screen.findByTestId('notification')).toHaveTextContent('QR-700040 is Error');
  });

  it('runs the archive', async () => {
    const calls = backend();
    renderWithProviders(<QrPaymentsPage />);

    await waitFor(() => expect(screen.getByTestId('run-archive')).toBeEnabled());
    await userEvent.click(screen.getByTestId('run-archive'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/qrPay/runQrPayArchive')).toHaveLength(1));
    expect(await screen.findByTestId('notification')).toHaveTextContent('4 process row(s) archived');
  });

  it('offers no refund and cancels only stuck or failed live processes', async () => {
    backend();
    renderWithProviders(<QrPaymentsPage />);

    const refunds = await screen.findAllByRole('button', { name: 'Refund' });
    refunds.forEach((button) => expect(button).toBeDisabled());
    expect(cancelReason(stuck)).toBeNull();
    expect(cancelReason({ ...stuck, stuck: false, state: 'ERROR' })).toBeNull();
    expect(cancelReason(closed)).not.toBeNull();
    expect(cancelReason({ ...stuck, stuck: false })).not.toBeNull();
  });

  it('highlights the transitions the process went through', () => {
    expect([...visitedTransitions(timeline)]).toEqual(['START_REQUESTED>SOCKET_START_WAITING']);
    const option = JSON.stringify(stateDiagramOption(timeline));
    expect(option).toContain('"color":"#1b5e20"');
  });
});
