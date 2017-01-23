import { Right } from './auth/rights';

export interface TabDefinition {
  no: number;
  /** Route path and screenshot name. */
  path: string;
  label: string;
  /** The tab is shown when the user holds every right of one of the alternatives. */
  visibleWith: string[][];
}

/**
 * The 16 tabs of the console. Visibility follows the rights of the backend roles: reception 1, 2, 5, 6, 7; campsite
 * host the reception tabs plus tariffs and reports; service 2, 4, 11, 12; back office 8-10, 13-15; market admin all.
 */
export const TABS: TabDefinition[] = [
  { no: 1, path: 'dashboard', label: 'Dashboard', visibleWith: [[Right.GUEST_EDIT, Right.PEDESTAL_VIEW], [Right.CONFIG_EDIT]] },
  { no: 2, path: 'map', label: 'Map', visibleWith: [[Right.PEDESTAL_VIEW, Right.GUEST_EDIT], [Right.PEDESTAL_VIEW, Right.PEDESTAL_EDIT]] },
  { no: 3, path: 'organisation', label: 'Organisation', visibleWith: [[Right.CONFIG_EDIT]] },
  { no: 4, path: 'pedestals', label: 'Pedestals & sockets', visibleWith: [[Right.PEDESTAL_COMMISSION]] },
  { no: 5, path: 'cards', label: 'Cards', visibleWith: [[Right.CARD_EDIT]] },
  { no: 6, path: 'guests', label: 'Guests & stays', visibleWith: [[Right.GUEST_EDIT]] },
  { no: 7, path: 'sessions', label: 'Supply sessions', visibleWith: [[Right.STAY_CHECKOUT, Right.PEDESTAL_VIEW]] },
  { no: 8, path: 'records', label: 'Consumption records', visibleWith: [[Right.CONSUMPTION_RECORD_REVIEW]] },
  { no: 9, path: 'tariffs', label: 'Tariffs', visibleWith: [[Right.TARIFF_EDIT]] },
  { no: 10, path: 'qr-payments', label: 'QR payments', visibleWith: [[Right.BILLING_EDIT, Right.REPORT_VIEW]] },
  { no: 11, path: 'power-budget', label: 'Power budget', visibleWith: [[Right.PEDESTAL_COMMISSION, Right.PEDESTAL_EDIT]] },
  { no: 12, path: 'monitoring', label: 'Monitoring & service', visibleWith: [[Right.PEDESTAL_COMMISSION, Right.PEDESTAL_EDIT]] },
  { no: 13, path: 'reports', label: 'Reports', visibleWith: [[Right.REPORT_VIEW]] },
  { no: 14, path: 'erp-billing', label: 'ERP billing', visibleWith: [[Right.BILLING_EDIT, Right.REPORT_VIEW]] },
  { no: 15, path: 'issuer-exchange', label: 'Issuer exchange', visibleWith: [[Right.BILLING_EDIT, Right.REPORT_VIEW]] },
  { no: 16, path: 'administration', label: 'Administration', visibleWith: [[Right.USER_ADMIN]] },
];

export function visibleTabs(rights: readonly string[] | undefined): TabDefinition[] {
  const held = new Set(rights ?? []);
  return TABS.filter((tab) => tab.visibleWith.some((alternative) => alternative.every((right) => held.has(right))));
}
