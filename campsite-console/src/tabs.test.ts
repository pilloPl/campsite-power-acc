import { visibleTabs } from './tabs';

const rightsOf = {
  reception: ['CARD_EDIT', 'CARD_VIEW', 'GUEST_EDIT', 'GUEST_VIEW', 'PEDESTAL_VIEW', 'STAY_CHECKOUT'],
  host: ['CARD_EDIT', 'CARD_VIEW', 'GUEST_EDIT', 'GUEST_VIEW', 'PEDESTAL_EDIT', 'PEDESTAL_VIEW', 'REPORT_VIEW',
    'SOCKET_REMOTE_CONTROL', 'STAY_CHECKOUT', 'TARIFF_EDIT'],
  service: ['PEDESTAL_COMMISSION', 'PEDESTAL_EDIT', 'PEDESTAL_VIEW', 'SOCKET_REMOTE_CONTROL'],
  backoffice: ['BILLING_EDIT', 'CARD_VIEW', 'CONSUMPTION_RECORD_REVIEW', 'GUEST_VIEW', 'PEDESTAL_VIEW',
    'PRICE_REPORT_VIEW', 'REPORT_VIEW', 'TARIFF_EDIT'],
  admin: ['AUDIT_VIEW', 'BILLING_EDIT', 'CARD_EDIT', 'CARD_VIEW', 'CONFIG_EDIT', 'CONSUMPTION_RECORD_REVIEW',
    'GUEST_EDIT', 'GUEST_VIEW', 'PEDESTAL_COMMISSION', 'PEDESTAL_EDIT', 'PEDESTAL_VIEW', 'PRICE_REPORT_VIEW',
    'REPORT_VIEW', 'SOCKET_REMOTE_CONTROL', 'STAY_CHECKOUT', 'TARIFF_EDIT', 'USER_ADMIN'],
};

const numbers = (rights: string[]) => visibleTabs(rights).map((tab) => tab.no);

describe('tabs follow the rights of the logged-in user', () => {
  it('reception sees dashboard, map, cards, guests and supply sessions', () => {
    expect(numbers(rightsOf.reception)).toEqual([1, 2, 5, 6, 7]);
  });

  it('a campsite host sees the reception tabs plus tariffs and reports', () => {
    expect(numbers(rightsOf.host)).toEqual([1, 2, 5, 6, 7, 9, 13]);
  });

  it('a service technician sees map, pedestals, power budget and monitoring', () => {
    expect(numbers(rightsOf.service)).toEqual([2, 4, 11, 12]);
  });

  it('back office sees records, tariffs, QR payments, reports, ERP billing and issuer exchange', () => {
    expect(numbers(rightsOf.backoffice)).toEqual([8, 9, 10, 13, 14, 15]);
  });

  it('a market admin sees all 16 tabs', () => {
    expect(numbers(rightsOf.admin)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]);
  });

  it('a user without rights sees no tab', () => {
    expect(numbers([])).toEqual([]);
  });
});
