import type { ComponentType } from 'react';
import DashboardPage from './DashboardPage';
import MapPage from './MapPage';
import OrganisationPage from './OrganisationPage';
import PedestalsPage from './PedestalsPage';
import CardsPage from './CardsPage';
import GuestsPage from './GuestsPage';
import SupplySessionsPage from './SupplySessionsPage';
import ConsumptionRecordsPage from './ConsumptionRecordsPage';
import TariffsPage from './TariffsPage';
import QrPaymentsPage from './QrPaymentsPage';
import PowerBudgetPage from './PowerBudgetPage';
import MonitoringPage from './MonitoringPage';
import ReportsPage from './ReportsPage';
import ErpBillingPage from './ErpBillingPage';
import IssuerExchangePage from './IssuerExchangePage';
import AdministrationPage from './AdministrationPage';

/** Page component of every tab path (see tabs.ts). */
export const PAGES: Record<string, ComponentType> = {
  'dashboard': DashboardPage,
  'map': MapPage,
  'organisation': OrganisationPage,
  'pedestals': PedestalsPage,
  'cards': CardsPage,
  'guests': GuestsPage,
  'sessions': SupplySessionsPage,
  'records': ConsumptionRecordsPage,
  'tariffs': TariffsPage,
  'qr-payments': QrPaymentsPage,
  'power-budget': PowerBudgetPage,
  'monitoring': MonitoringPage,
  'reports': ReportsPage,
  'erp-billing': ErpBillingPage,
  'issuer-exchange': IssuerExchangePage,
  'administration': AdministrationPage,
};
