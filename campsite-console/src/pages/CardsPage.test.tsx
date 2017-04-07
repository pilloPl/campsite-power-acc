import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CardsPage from './CardsPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const card = {
  id: 900000153, cardRfid: '4000000153', cardNumber: 'HR-100153', activation: 'ACTIVATED', guestId: 900000153,
  guestName: 'Sara Smit', issuerGroupName: 'Network card', validFrom: '2026-10-16', validTo: '2026-10-22',
  partnerSyncState: 'ADD', cardStatus: 'VALID',
};

const welcome = {
  id: 900090121, cardRfid: '8000000101', cardNumber: 'HR-W001', activation: 'ACTIVATED', guestId: 900090001,
  guestName: 'HR Welcome card pool', issuerGroupName: 'Croatia welcome card pool', partnerSyncState: 'NOT',
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/guest/findStays': { stays: [{ id: 1, guestId: 900000153, guestName: 'Sara Smit', cardId: 900000153,
      campsiteName: 'Camp Istria Bay', pitchNo: 'B11', departureDate: '2026-10-22' }] },
    '/api/console/guest/getGuest': { guest: { guest: { id: 900000153 }, cards: [card] } },
    '/api/console/card/findCards': { cards: [{ ...card, id: 900000001, cardNumber: 'HR-100001', guestName: 'Emma Martin' }] },
    '/api/console/guest/findGuests': { guests: [] },
    '/api/console/houseCard/findHouseCards': {
      houseCards: [{ id: 11, houseCardType: 'STAFF', rfid: '771000001', cardNumber: 'HRB-STAFF-1', active: true, groupName: 'Camp Istria Bay' }],
    },
    '/api/console/map/getMapData': {
      map: { campsites: [{ groupId: 1, displayName: 'Camp Istria Bay', pedestals: [{ code: 'ISTRIABAY-P01', sockets: [{ socketNo: 1 }] }] }] },
    },
    '/api/console/card/getLocalCardList': {
      localCardList: { pedestalCode: 'ISTRIABAY-P01', capacity: 1000, houseCards: 1, cards: 1, droppedEntries: 0,
        entries: [{ rfid: '771000001', validTo: null, houseCard: true }] },
    },
    '/api/console/card/changeCardStatus': { card: { ...card, activation: 'DEACTIVATED' } },
    '/api/console/card/sendLocalCardList': { pedestalCode: 'ISTRIABAY-P01', sent: true },
    '/api/console/houseCard/changeHouseCardStatus': { houseCardId: 11, active: false },
    '/api/console/card/findWelcomePoolCards': { cards: [welcome] },
    '/api/console/card/assignWelcomeCard': { card: { ...welcome, guestId: 900000153, guestName: 'Sara Smit' } },
  });
}

describe('cards tab', () => {
  it('lists the cards of the guests staying now, the house cards and the local card list of a pedestal', async () => {
    backend();
    renderWithProviders(<CardsPage />);

    expect(await screen.findByText('HR-100153')).toBeInTheDocument();
    expect(await screen.findByText('HRB-STAFF-1')).toBeInTheDocument();
    expect(await screen.findByTestId('list-summary')).toHaveTextContent('capacity 1000');
  });

  it('blocks a card after confirmation', async () => {
    const calls = backend();
    renderWithProviders(<CardsPage />);

    await userEvent.click(await screen.findByTestId('block-900000153'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/card/changeCardStatus')).toHaveLength(1));
    expect(calls.callsTo('/api/console/card/changeCardStatus')[0].body).toEqual({ cardId: 900000153, activation: 'DEACTIVATED' });
    expect(await screen.findByTestId('notification')).toHaveTextContent('Card HR-100153 blocked');
  });

  it('searches a card by its number', async () => {
    const calls = backend();
    renderWithProviders(<CardsPage />);

    await userEvent.type(screen.getByLabelText('Search cards'), 'HR-100001{Enter}');

    expect(await screen.findByText('Emma Martin')).toBeInTheDocument();
    expect(calls.callsTo('/api/console/card/findCards')[0].body).toEqual({ rfidOrNumber: 'HR-100001' });
  });

  it('pushes the local card list to the pedestal now', async () => {
    const calls = backend();
    renderWithProviders(<CardsPage />);

    await waitFor(() => expect(screen.getByTestId('send-card-list')).toBeEnabled());
    await userEvent.click(screen.getByTestId('send-card-list'));

    await waitFor(() => expect(calls.callsTo('/api/console/card/sendLocalCardList')).toHaveLength(1));
    expect(calls.callsTo('/api/console/card/sendLocalCardList')[0].body).toEqual({ pedestalCode: 'ISTRIABAY-P01' });
  });

  it('deactivates a house card of the owner', async () => {
    const calls = backend();
    renderWithProviders(<CardsPage />);

    await userEvent.click(await screen.findByTestId('house-status-11'));

    await waitFor(() => expect(calls.callsTo('/api/console/houseCard/changeHouseCardStatus')).toHaveLength(1));
    expect(calls.callsTo('/api/console/houseCard/changeHouseCardStatus')[0].body)
      .toEqual({ ownerUsername: 'admin', houseCardId: 11, active: false });
  });

  it('hands a welcome pool card to a guest staying now for the rest of the stay', async () => {
    const calls = backend();
    renderWithProviders(<CardsPage />);

    await waitFor(() => expect(screen.getByTestId('assign-welcome')).toBeEnabled());
    await userEvent.click(screen.getByTestId('assign-welcome'));
    const dialog = await screen.findByTestId('welcome-pool-dialog');
    expect(await within(dialog).findByText('HR-W001')).toBeInTheDocument();
    await userEvent.click(within(dialog).getByLabelText(/^Guest staying now/));
    await userEvent.click(await screen.findByRole('option', { name: /Sara Smit/ }));
    await userEvent.click(within(dialog).getByTestId('welcome-assign'));

    await waitFor(() => expect(calls.callsTo('/api/console/card/assignWelcomeCard')).toHaveLength(1));
    expect(calls.callsTo('/api/console/card/assignWelcomeCard')[0].body).toMatchObject({
      cardId: 900090121, guestId: 900000153, validTo: '2026-10-22',
    });
    expect(await screen.findByTestId('notification')).toHaveTextContent('Card HR-W001 handed to Sara Smit');
  });
});
