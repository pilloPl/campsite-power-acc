import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import MapPage from './MapPage';
import { busiestCampsite } from '../components/CampsiteSelect';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';
import { mapData } from './map/testData';

vi.mock('react-leaflet', async () => (await import('./map/leafletMock')).leafletMock);

function backend(rights = adminUser.rights) {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: { ...adminUser, rights } },
    '/api/console/map/getMapData': mapData,
    '/api/console/supply/stopSupply': { command: { command: 'STOP', pedestalCode: 'ISTRIABAY-P01', accepted: true } },
    '/api/console/supply/startSupply': { command: { command: 'START', pedestalCode: 'ISTRIABAY-P01', accepted: true } },
    '/api/console/monitoring/resetFault': { command: { command: 'RESET', accepted: true, socketStatus: 'AVAILABLE' } },
  });
}

describe('map tab', () => {
  it('opens the campsite with the most supplying sockets first', () => {
    const quiet = { groupId: 1, pedestals: [{ sockets: [{}, {}] }] };
    const busy = { groupId: 2, pedestals: [{ sockets: [{ activeSessionId: 7 }] }, { sockets: [{ activeSessionId: 8 }] }] };
    expect(busiestCampsite([quiet, busy])?.groupId).toBe(2);
    expect(busiestCampsite([])).toBeUndefined();
  });

  it('draws the pedestals of the campsite and the section load overlay', async () => {
    backend();
    renderWithProviders(<MapPage />);

    expect(await screen.findAllByTestId('pedestal-marker')).toHaveLength(2);
    expect(screen.getByTestId('section-load')).toHaveTextContent('ISTRIA BAY SECTION A');
    expect(screen.getByTestId('section-load')).toHaveTextContent('120 %');
    expect(screen.getByTestId('section-load')).toHaveTextContent('96 A allocated of 80 A, 6 supplies');
  });

  it('stops a running supply from the pedestal popup after confirmation', async () => {
    const calls = backend();
    renderWithProviders(<MapPage />);

    await userEvent.click(await screen.findByTestId('popup-stop-11'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/supply/stopSupply')).toHaveLength(1));
    expect(calls.callsTo('/api/console/supply/stopSupply')[0].body).toEqual({ sessionId: 5001 });
    expect(await screen.findByTestId('notification')).toHaveTextContent('Stop accepted by the pedestal');
  });

  it('starts a supply for a card on an available socket', async () => {
    const calls = backend();
    renderWithProviders(<MapPage />);

    await userEvent.click(await screen.findByTestId('popup-start-12'));
    await userEvent.type(screen.getByLabelText(/Card RFID/), '04A1B2C3');
    await userEvent.click(screen.getByRole('button', { name: 'Start supply' }));

    await waitFor(() => expect(calls.callsTo('/api/console/supply/startSupply')[0]?.body).toEqual({ socketId: 12, cardRfid: '04A1B2C3' }));
  });

  it('resets the fault (RCD) of a faulted socket', async () => {
    const calls = backend();
    renderWithProviders(<MapPage />);

    await userEvent.click(await screen.findByTestId('popup-reset-24'));

    await waitFor(() => expect(calls.callsTo('/api/console/monitoring/resetFault')[0]?.body)
      .toEqual({ pedestalCode: 'ISTRIABAY-P02', socketNo: 4, socketId: 24 }));
  });

  it('offers no remote stop without the right of remote control', async () => {
    backend(adminUser.rights.filter((right) => right !== 'SOCKET_REMOTE_CONTROL'));
    renderWithProviders(<MapPage />);

    await waitFor(() => expect(screen.getByTestId('popup-stop-11')).toBeDisabled());
  });
});
