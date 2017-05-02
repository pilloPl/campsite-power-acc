import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ErpBillingPage from './ErpBillingPage';
import { Fault, adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const line = {
  id: 2, billingPeriod: '2026_09', billingAccountNo: '4000000004', erpPartnerNo: '70000004', lineType: 'POWER',
  description: 'Pitch power', quantity: 34.758, unitPrice: 0.45, amount: 15.64, currency: 'EUR', exportStatus: 'ERROR',
  errorText: 'Account 4000000004 blocked: dunning level 3',
};

function backend(runAnswer: unknown = { lines: [line] }) {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/erpBilling/findErpBillingRuns': {
      runs: [
        { billingPeriod: '2026_08', runDate: '2026-09-03T02:00:00', lines: 2, accounts: 2, amount: 111.42, linesPerExportStatus: { PROCESSED: 2 } },
        { billingPeriod: '2026_09', runDate: '2026-10-03T02:00:00', lines: 259, accounts: 257, amount: 4746.09,
          linesPerExportStatus: { PROCESSED: 199, TRANSFERRED: 27, ERROR: 33 } },
      ],
    },
    '/api/console/erpBilling/findErpBillingLines': { lines: [line] },
    '/api/console/erpBilling/findRecordsWithoutExport': { consumptionRecords: [{ id: 878, recordNo: 'GARDALAKE-20260904-000878', exportStatus: 'NEW' }] },
    '/api/console/erpBilling/previewErpBilling': { lines: [{ ...line, id: null, exportStatus: 'NEW', billingPeriod: '2026_10' }] },
    '/api/console/erpBilling/runErpBilling': runAnswer,
    '/api/console/erpBilling/retryErpExport': { resentLines: 33 },
  });
}

describe('ERP billing tab', () => {
  it('shows the runs with their lines per export status and the refusals of the latest month', async () => {
    const calls = backend();
    renderWithProviders(<ErpBillingPage />);

    const runs = await screen.findByTestId('billing-runs');
    expect(await within(runs).findByText('Error 33')).toBeInTheDocument();
    expect(await within(screen.getByTestId('billing-errors')).findByText('Account 4000000004 blocked: dunning level 3')).toBeInTheDocument();
    expect(calls.callsTo('/api/console/erpBilling/findErpBillingLines').map((call) => call.body))
      .toContainEqual({ billingPeriod: '2026_09', exportStatus: 'ERROR' });
    expect(await within(screen.getByTestId('records-without-export')).findByText('GARDALAKE-20260904-000878')).toBeInTheDocument();
  });

  it('sends the refused lines of a month to the ERP again after confirmation', async () => {
    const calls = backend();
    renderWithProviders(<ErpBillingPage />);

    await userEvent.click(await screen.findByTestId('retry-2026_09'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/erpBilling/retryErpExport')).toHaveLength(1));
    expect(calls.callsTo('/api/console/erpBilling/retryErpExport')[0].body).toEqual({ billingPeriod: '2026_09' });
    expect(await screen.findByTestId('notification')).toHaveTextContent('33 refused lines go to the ERP again');
  });

  it('previews the billing of the chosen month without storing it', async () => {
    const calls = backend();
    renderWithProviders(<ErpBillingPage />);

    const month = screen.getByTestId('billing-month');
    await userEvent.clear(month);
    await userEvent.type(month, '2026-10');
    await userEvent.click(screen.getByTestId('preview-billing'));

    await waitFor(() => expect(calls.callsTo('/api/console/erpBilling/previewErpBilling')).toHaveLength(1));
    expect(calls.callsTo('/api/console/erpBilling/previewErpBilling')[0].body).toEqual({ year: 2026, month: 10 });
    expect(await screen.findByText(/nothing is stored by the preview/)).toBeInTheDocument();
  });

  it('shows the refusal of a run for a month that is billed already', async () => {
    backend(new Fault(409, 'CONFLICT', 'The month 2026_09 is billed already'));
    renderWithProviders(<ErpBillingPage />);

    await userEvent.click(screen.getByTestId('run-billing'));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    expect(await screen.findByTestId('notification')).toHaveTextContent('The month 2026_09 is billed already');
  });
});
