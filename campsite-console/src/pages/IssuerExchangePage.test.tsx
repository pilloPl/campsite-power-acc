import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import IssuerExchangePage from './IssuerExchangePage';
import { syncMatrix } from './issuer/syncMatrix';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const partner = {
  id: 1, name: 'ADRIA CLUB', displayName: 'Adria Camping Club', issuerGroupId: 11, issuerGroupName: 'ADRIA CAMPING CLUB',
  interfaceVersion: 'V2', endpoint: 'https://exchange.adria-club.example/api/v2', cardImport: true, cardExport: false,
  recordExport: true, recordImport: false, active: true,
};
const statuses = [
  { partnerName: 'ADRIA CLUB', direction: 'CARD_IMPORT', status: 'ERROR', lastRun: '2026-10-17T07:40:00', message: 'Cards not fetched' },
  { partnerName: 'ADRIA CLUB', direction: 'RECORD_EXPORT', status: 'OK', lastRun: '2026-10-17T07:40:00', itemsAccepted: 12 },
];

const filter = {
  id: 71, groupId: 11, groupName: 'ADRIA CAMPING CLUB', groupDisplayName: 'Adria Camping Club', synchronizable: true,
  filterType: 'IMPORT_EXPORT', issuerPartnerId: 1, issuerPartnerName: 'ADRIA CLUB',
};

function backend(user = adminUser) {
  return fakeBackend({
    '/api/console/auth/currentUser': { user },
    '/api/console/issuer/findIssuerPartners': { partners: [partner] },
    '/api/console/issuer/findIssuerSyncStatuses': { syncStatuses: statuses },
    '/api/console/group/findManageableGroups': (body: { groupType: string }) => ({
      groups: body.groupType === 'ISSUER' ? [{ id: 11, name: 'ADRIA CAMPING CLUB', displayName: 'Adria Camping Club', type: 'ISSUER' }]
        : [{ id: 21, name: 'ISTRIA BAY CAMP', displayName: 'Camp Istria Bay', type: 'SITE' }],
    }),
    '/api/console/issuer/saveIssuerPartner': (body: { partner: { name: string } }) => ({ partner: body.partner }),
    '/api/console/issuer/runIssuerExchange': { syncStatuses: statuses },
    '/api/console/card/findCards': { cards: [{ id: 900000001, cardNumber: 'HR-100001', cardRfid: '4000000001', cardStatus: 'VALID' }] },
    '/api/console/issuer/resyncCard': { card: { id: 900000001, cardNumber: 'HR-100001' } },
    '/api/console/issuer/findWhitelistFilters': { filters: [filter] },
    '/api/console/issuer/saveWhitelistFilter': (body: { filter: object }) => ({ filter: { ...filter, ...body.filter } }),
    '/api/console/issuer/deleteWhitelistFilter': { deletedFilterId: 71 },
  });
}

describe('issuer exchange tab', () => {
  it('fills the sync matrix with the state of every switched-on direction and OFF for the others', () => {
    expect(syncMatrix([partner], statuses).map((cell) => `${cell.direction}=${cell.state}`))
      .toEqual(['CARD_IMPORT=ERROR', 'CARD_EXPORT=OFF', 'RECORD_EXPORT=OK', 'RECORD_IMPORT=OFF']);
    expect(syncMatrix([{ ...partner, recordImport: true }], []).map((cell) => cell.state))
      .toEqual(['NOT_RUN', 'OFF', 'NOT_RUN', 'NOT_RUN']);
  });

  it('shows the partners and the last runs with their messages', async () => {
    backend();
    renderWithProviders(<IssuerExchangePage />);

    expect(await within(screen.getByTestId('partners')).findByText('Adria Camping Club')).toBeInTheDocument();
    expect(await within(screen.getByTestId('sync-statuses')).findByText('Cards not fetched')).toBeInTheDocument();
    expect(within(screen.getByTestId('sync-matrix')).getByTestId('echarts-option')).toHaveTextContent('Import partner cards');
  });

  it('saves a new partner with its groups and directions', async () => {
    const calls = backend();
    renderWithProviders(<IssuerExchangePage />);

    await waitFor(() => expect(screen.getByTestId('new-partner')).toBeEnabled());
    await userEvent.click(screen.getByTestId('new-partner'));
    const dialog = screen.getByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText(/^Name/), 'ALPINE CLUB');
    await userEvent.type(within(dialog).getByLabelText(/^Endpoint/), '/data/exchange/alpine');
    await userEvent.click(within(dialog).getByLabelText(/^ISSUER group/));
    await userEvent.click(await screen.findByRole('option', { name: 'Adria Camping Club' }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(calls.callsTo('/api/console/issuer/saveIssuerPartner')).toHaveLength(1));
    expect(calls.callsTo('/api/console/issuer/saveIssuerPartner')[0].body).toEqual({
      partner: {
        name: 'ALPINE CLUB', interfaceVersion: 'V2', endpoint: '/data/exchange/alpine', issuerGroupId: 11,
        cardImport: true, cardExport: false, recordExport: true, recordImport: false, active: true,
      },
    });
    expect(await screen.findByTestId('notification')).toHaveTextContent('Partner ALPINE CLUB saved');
  });

  it('runs the exchange with every active partner after confirmation', async () => {
    const calls = backend();
    renderWithProviders(<IssuerExchangePage />);

    await waitFor(() => expect(screen.getByTestId('run-exchange')).toBeEnabled());
    await userEvent.click(screen.getByTestId('run-exchange'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/issuer/runIssuerExchange')).toHaveLength(1));
    expect(await screen.findByTestId('notification')).toHaveTextContent('1 direction(s) failed');
  });

  it('finds a card and sends it to the partners again', async () => {
    const calls = backend();
    renderWithProviders(<IssuerExchangePage />);

    await userEvent.type(screen.getByTestId('card-search'), 'HR-100001');
    await userEvent.click(screen.getByTestId('find-card'));
    await waitFor(() => expect(screen.getByTestId('resync-900000001')).toBeEnabled());
    await userEvent.click(screen.getByTestId('resync-900000001'));

    await waitFor(() => expect(calls.callsTo('/api/console/issuer/resyncCard')).toHaveLength(1));
    expect(calls.callsTo('/api/console/card/findCards')[0].body).toEqual({ rfidOrNumber: 'HR-100001' });
    expect(calls.callsTo('/api/console/issuer/resyncCard')[0].body).toEqual({ cardId: 900000001 });
  });

  it('keeps partner editing and the exchange run disabled without CONFIG_EDIT', async () => {
    backend({ ...adminUser, rights: ['REPORT_VIEW', 'BILLING_EDIT', 'CARD_VIEW'] });
    renderWithProviders(<IssuerExchangePage />);

    await waitFor(() => expect(screen.getByTestId('run-exchange')).toBeDisabled());
    expect(screen.getByTestId('new-partner')).toBeDisabled();
    expect(screen.getByTestId('add-filter')).toBeDisabled();
    expect(await screen.findByTestId('edit-filter-71')).toBeDisabled();
  });

  it('lists the whitelist filters with direction, partner and whether the cards are exchanged', async () => {
    backend();
    renderWithProviders(<IssuerExchangePage />);

    const section = screen.getByTestId('whitelist-filters');
    expect(await within(section).findByText('Adria Camping Club')).toBeInTheDocument();
    expect(within(section).getByText('Import and export')).toBeInTheDocument();
    expect(within(section).getByText('Exchanged')).toBeInTheDocument();
  });

  it('changes a whitelist filter to keep the cards of the group out of the import of all partners', async () => {
    const calls = backend();
    renderWithProviders(<IssuerExchangePage />);

    await userEvent.click(await screen.findByTestId('edit-filter-71'));
    const dialog = screen.getByRole('dialog');
    await userEvent.click(within(dialog).getByLabelText(/^Direction/));
    await userEvent.click(await screen.findByRole('option', { name: 'Card import only' }));
    await userEvent.click(within(dialog).getByLabelText(/^Partner/));
    await userEvent.click(await screen.findByRole('option', { name: 'All partners' }));
    await userEvent.click(within(dialog).getByLabelText('Cards of the group are exchanged'));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save filter' }));

    await waitFor(() => expect(calls.callsTo('/api/console/issuer/saveWhitelistFilter')).toHaveLength(1));
    expect(calls.callsTo('/api/console/issuer/saveWhitelistFilter')[0].body).toEqual({
      filter: { id: 71, groupId: 11, filterType: 'IMPORT', synchronizable: false },
    });
  });

  it('removes a whitelist filter after confirmation', async () => {
    const calls = backend();
    renderWithProviders(<IssuerExchangePage />);

    await userEvent.click(await screen.findByTestId('delete-filter-71'));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/issuer/deleteWhitelistFilter')).toHaveLength(1));
    expect(calls.callsTo('/api/console/issuer/deleteWhitelistFilter')[0].body).toEqual({ filterId: 71 });
  });
});
