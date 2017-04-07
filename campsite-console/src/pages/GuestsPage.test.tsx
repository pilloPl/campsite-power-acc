import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GuestsPage from './GuestsPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const stay = {
  id: 501, guestId: 7, guestName: 'Sara Smit', siteGroupId: 5, campsiteName: 'Camp Istria Bay', pitchNo: 'B12',
  cardId: 9, cardNumber: 'HR-100153', unitPlate: 'DK-XF-253', arrivalDate: '2026-10-16', departureDate: '2026-10-22',
  nights: 6, checkInDate: '2026-10-16T15:00:00', checkOutDate: null,
};
const session = {
  id: 9001, active: true, stale: false, stayId: 501, socketName: 'P06_4', start: '2026-10-16T14:47:00', kwh: 2.01,
};
const guest = {
  id: 7, issuerGroupId: 31, issuerName: 'Network card', guestType: 'TRANSIT', fullName: 'Sara Smit', status: 'ACTIVE',
  city: 'Aarhus', countryIso: 'DK', paymentMethod: 'RECEPTION',
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': { map: { campsites: [{ groupId: 5, displayName: 'Camp Istria Bay' }] } },
    '/api/console/guest/findStays': { stays: [stay] },
    '/api/console/supply/findSupplySessions': { sessions: [session], total: 1 },
    '/api/console/guest/checkOut': { stay: { ...stay, checkOutDate: '2026-10-17T08:00:00' } },
    '/api/console/job/runJobNow': { job: { name: 'closeSessionsOnCheckoutJob' } },
    '/api/console/guest/findGuests': { guests: [guest] },
    '/api/console/guest/getGuest': {
      guest: { guest, units: [{ id: 3, unitType: 'CARAVAN', plate: 'DK-XF-253' }], cards: [], billingAccounts: [],
        seasonalContracts: [], stays: [], orders: [{ id: 44, orderNumber: 'O-44', status: 'NEW', items: ['Power package'] }] },
    },
    '/api/console/guest/cancelOrder': { order: { id: 44, status: 'CANCELLED' } },
    '/api/console/guest/changeDeparture': { stay: { ...stay, departureDate: '2026-10-25' } },
  });
}

describe('guests and stays tab', () => {
  it('lists the open stays with their running supply sessions', async () => {
    backend();
    renderWithProviders(<GuestsPage />);

    const table = await screen.findByTestId('stays-table');
    expect(await within(table).findByText('Sara Smit')).toBeInTheDocument();
    expect(await within(table).findByText('1 running')).toBeInTheDocument();
  });

  it('checks a guest out, names the session that will be closed and runs the session close job', async () => {
    const calls = backend();
    renderWithProviders(<GuestsPage />);

    await userEvent.click(await screen.findByTestId('checkout-501'));
    expect(await screen.findByTestId('checkout-open-sessions')).toHaveTextContent('1 open supply session will be closed');
    await userEvent.click(screen.getByTestId('checkout-confirm'));

    expect(await screen.findByTestId('checkout-result')).toHaveTextContent('closed by the session close job');
    expect(calls.callsTo('/api/console/guest/checkOut')[0].body).toEqual({ stayId: 501 });
    await userEvent.click(screen.getByTestId('checkout-run-job'));
    await waitFor(() => expect(calls.callsTo('/api/console/job/runJobNow')[0]?.body)
      .toEqual({ jobName: 'closeSessionsOnCheckoutJob' }));
  });

  it('extends a stay with a new departure', async () => {
    const calls = backend();
    renderWithProviders(<GuestsPage />);

    await userEvent.click(await screen.findByTestId('extend-501'));
    const dialog = screen.getByRole('dialog');
    const departure = within(dialog).getByLabelText(/New departure/);
    await userEvent.clear(departure);
    await userEvent.type(departure, '2026-10-25');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Change departure' }));

    await waitFor(() => expect(calls.callsTo('/api/console/guest/changeDeparture')[0]?.body)
      .toEqual({ stayId: 501, departure: '2026-10-25' }));
  });

  it('opens a guest and cancels a booking that is not active yet', async () => {
    const calls = backend();
    renderWithProviders(<GuestsPage />);

    await userEvent.click(await screen.findByRole('tab', { name: 'Guests' }));
    await userEvent.click(await screen.findByText('Aarhus'));
    const detail = await screen.findByRole('dialog');
    expect(await within(detail).findByText('O-44')).toBeInTheDocument();
    await userEvent.click(within(detail).getByRole('button', { name: 'Cancel' }));
    await userEvent.type(screen.getByLabelText('Reason'), 'guest stays at home');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel booking' }));

    await waitFor(() => expect(calls.callsTo('/api/console/guest/cancelOrder')[0]?.body)
      .toEqual({ orderId: 44, reason: 'guest stays at home' }));
  });
});
