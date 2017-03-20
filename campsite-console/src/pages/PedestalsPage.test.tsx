import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PedestalsPage from './PedestalsPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const pedestal = {
  id: 101, code: 'ISTRIABAY-P01', groupName: 'Camp Istria Bay', serialNumber: 'SN2024101', firmwareVersion: '2.4.0',
  modemIccid: '8938501000000000101', remoteControl: true, linkTimeout: false, lastIncoming: '2026-10-17T07:31:00',
  type: { id: 1, manufacturer: 'ADRIATEC', name: 'AQ-400', linkVersion: 'V2', localCardList: true, localCardListCapacity: 1000 },
  location: { id: 9, name: 'Camp Istria Bay P01', street: 'Petalon', houseNumber: '1', zipcode: '52450', city: 'Vrsar', countryIso: 'HR' },
  sockets: [
    { id: 11, name: 'ISTRIABAY-P01_1', socketNo: 1, pitchNo: 'A01', socketType: 'CEE blue 16A', meterKwh: 1302.8, status: 'SUPPLYING', freeSocket: false },
    { id: 12, name: 'ISTRIABAY-P01_2', socketNo: 2, pitchNo: 'A02', socketType: 'CEE blue 16A', meterKwh: 1322.4, status: 'AVAILABLE', freeSocket: false },
  ],
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/pedestal/findPedestalsByState': {
      pedestals: [
        { id: 101, code: 'ISTRIABAY-P01', type: 'AQ-400', groupName: 'Camp Istria Bay', numberOfSockets: 2, socketInError: false },
        { id: 102, code: 'ISTRIABAY-P02', type: 'Campus 3', groupName: 'Camp Istria Bay', numberOfSockets: 4, socketInError: true },
      ],
      total: 2,
    },
    '/api/console/pedestal/getPedestal': { pedestal },
    '/api/console/commissioning/getSupplierStock': {
      pedestals: [{ id: 300, pedestalCode: 'STOCK-7', pedestalType: 'AQ-400', numberOfSockets: 4, serialNumber: 'SN7' }],
    },
    '/api/console/commissioning/findCommissionings': { commissionings: [] },
    '/api/console/pedestal/assignPitch': { socket: { ...pedestal.sockets[1], pitchNo: 'B07' } },
    '/api/console/pedestal/setFreeSocket': { socket: { ...pedestal.sockets[0], freeSocket: true } },
    '/api/console/card/sendLocalCardList': { pedestalCode: 'ISTRIABAY-P01', sent: true },
    '/api/console/commissioning/decommissionPedestal': { pedestalId: 101, decommissioned: true },
    '/api/console/commissioning/commissionPedestal': { pedestalCode: 'STOCK-7', commissioned: true },
  });
}

describe('pedestals and sockets tab', () => {
  it('lists the pedestals and shows the first one with its sockets, firmware and heartbeat', async () => {
    backend();
    renderWithProviders(<PedestalsPage />, { path: '/pedestals' });

    const detail = await screen.findByTestId('pedestal-detail');
    expect(detail).toHaveTextContent('ADRIATEC AQ-400');
    expect(detail).toHaveTextContent('2.4.0');
    expect(within(screen.getByTestId('socket-table')).getByText('ISTRIABAY-P01_2')).toBeInTheDocument();
    expect(screen.getByTestId('pedestal-list')).toHaveTextContent('ISTRIABAY-P02');
  });

  it('assigns a socket to another pitch', async () => {
    const calls = backend();
    renderWithProviders(<PedestalsPage />, { path: '/pedestals' });

    await userEvent.click(await screen.findByTestId('assign-pitch-12'));
    const pitch = screen.getByLabelText('Pitch number');
    await userEvent.clear(pitch);
    await userEvent.type(pitch, 'B07');
    await userEvent.click(screen.getByRole('button', { name: 'Assign' }));

    await waitFor(() => expect(calls.callsTo('/api/console/pedestal/assignPitch')[0]?.body).toEqual({ socketId: 12, pitchNo: 'B07' }));
  });

  it('sends the local card list now and uninstalls the pedestal back to the stock', async () => {
    const calls = backend();
    renderWithProviders(<PedestalsPage />, { path: '/pedestals' });

    await userEvent.click(await screen.findByTestId('send-card-list'));
    await waitFor(() => expect(calls.callsTo('/api/console/card/sendLocalCardList')[0]?.body).toEqual({ pedestalCode: 'ISTRIABAY-P01' }));

    await userEvent.click(screen.getByTestId('uninstall'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));
    await waitFor(() => expect(calls.callsTo('/api/console/commissioning/decommissionPedestal')[0]?.body).toEqual({ pedestalId: 101 }));
  });

  it('shows the firmware update disabled because the backend does not offer it', async () => {
    backend();
    renderWithProviders(<PedestalsPage />, { path: '/pedestals' });

    expect(await screen.findByRole('button', { name: 'Request firmware update' })).toBeDisabled();
  });

  it('installs a pedestal from the supplier stock with the wizard', async () => {
    const calls = backend();
    renderWithProviders(<PedestalsPage />, { path: '/pedestals' });

    await screen.findByTestId('pedestal-detail');
    await userEvent.click(screen.getByTestId('install-wizard'));
    const wizard = screen.getByRole('dialog');
    expect(within(wizard).getByText('STOCK-7')).toBeInTheDocument();
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    await userEvent.type(within(wizard).getByLabelText(/Location name/), 'Pitch row C');
    await userEvent.type(within(wizard).getByLabelText(/City/), 'Vrsar');
    await userEvent.type(within(wizard).getByLabelText(/Country/), 'HR');
    await userEvent.click(within(wizard).getByRole('button', { name: 'Next' }));
    await userEvent.click(within(wizard).getByRole('button', { name: 'Install' }));

    await waitFor(() => expect(calls.callsTo('/api/console/commissioning/commissionPedestal')).toHaveLength(1));
    expect(calls.callsTo('/api/console/commissioning/commissionPedestal')[0].body).toMatchObject({
      pedestalCode: 'STOCK-7', ownerUsername: 'admin', accessMode: 'HOUSE_CARDS_ONLY', listedInApp: true,
      location: { name: 'Pitch row C', city: 'Vrsar', countryIso: 'HR' },
    });
  });
});
