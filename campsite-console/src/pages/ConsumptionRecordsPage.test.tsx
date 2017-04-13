import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ConsumptionRecordsPage, { canReprice } from './ConsumptionRecordsPage';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

async function enabled(testId: string) {
  await waitFor(() => expect(screen.getByTestId(testId)).toBeEnabled());
  return screen.getByTestId(testId);
}

const record = {
  id: 400, recordNo: 'ISTRIABAY-20261017-000400', pedestalCode: 'ISTRIABAY-P05', socketCode: 'ISTRIABAY*P05*2',
  pitchNo: 'B06', campsiteName: 'Camp Istria Bay', guestName: 'Mila Kowalski', cardNumber: 'HR-100123',
  paymentChannel: 'CARD', plugIn: '2026-10-15T16:51:25', plugOut: '2026-10-17T06:17:17', nights: 2, kwh: 712.414,
  cost: 320.59, currency: 'EUR', pricingStatus: 'PRICED', tariffName: 'HR NIGHT SAVER', validationState: 'SUSPECT',
  suspectReason: 'Consumption 712.414 kWh in 2 night(s) is above 300 kWh per night', exportStatus: 'REVIEW',
};
const exportError = {
  ...record, id: 401, recordNo: 'ISTRIABAY-20261017-000401', validationState: 'VALID', suspectReason: undefined,
  exportStatus: 'ERROR', kwh: 12, cost: 5.4,
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': { map: { campsites: [{ groupId: 1, displayName: 'Camp Istria Bay' }] } },
    '/api/console/consumptionRecord/findConsumptionRecords': (body: { validationState?: string }) => ({
      consumptionRecords: body.validationState === 'SUSPECT' ? [record] : [record, exportError],
    }),
    '/api/console/consumptionRecord/reviewConsumptionRecord': (body: { confirmed: boolean }) => ({
      consumptionRecord: { ...record, validationState: body.confirmed ? 'CONFIRMED' : 'REJECTED' },
    }),
    '/api/console/consumptionRecord/requeueConsumptionRecordExport': { consumptionRecord: { ...exportError, exportStatus: 'NEW' } },
  });
}

describe('consumption records tab', () => {
  it('lists the SUSPECT review queue with the reason', async () => {
    backend();
    renderWithProviders(<ConsumptionRecordsPage />);

    const queue = await screen.findByTestId('review-queue');
    expect(await within(queue).findByText(/above 300 kWh per night/)).toBeInTheDocument();
    expect(within(queue).getByText('Mila Kowalski')).toBeInTheDocument();
  });

  it('approves a SUSPECT record with a comment', async () => {
    const calls = backend();
    renderWithProviders(<ConsumptionRecordsPage />);

    await screen.findByTestId('review-400');
    await userEvent.click(await enabled('review-400'));
    const dialog = screen.getByTestId('review-dialog');
    await userEvent.type(within(dialog).getByLabelText('Comment'), 'heater in the caravan');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Approve' }));

    await waitFor(() => expect(calls.callsTo('/api/console/consumptionRecord/reviewConsumptionRecord')).toHaveLength(1));
    expect(calls.callsTo('/api/console/consumptionRecord/reviewConsumptionRecord')[0].body)
      .toEqual({ consumptionRecordId: 400, confirmed: true, comment: 'heater in the caravan' });
    expect(await screen.findByTestId('notification')).toHaveTextContent('approved');
  });

  it('rejects a SUSPECT record', async () => {
    const calls = backend();
    renderWithProviders(<ConsumptionRecordsPage />);

    await screen.findByTestId('review-400');
    await userEvent.click(await enabled('review-400'));
    await userEvent.click(within(screen.getByTestId('review-dialog')).getByRole('button', { name: 'Reject' }));

    await waitFor(() => expect(calls.callsTo('/api/console/consumptionRecord/reviewConsumptionRecord')[0]?.body)
      .toEqual({ consumptionRecordId: 400, confirmed: false }));
  });

  it('queues a failed export again', async () => {
    const calls = backend();
    renderWithProviders(<ConsumptionRecordsPage />);

    await screen.findByTestId('requeue-401');
    await userEvent.click(await enabled('requeue-401'));

    await waitFor(() => expect(calls.callsTo('/api/console/consumptionRecord/requeueConsumptionRecordExport')[0]?.body)
      .toEqual({ consumptionRecordId: 401 }));
  });

  it('asks for the period of the records', async () => {
    const calls = backend();
    renderWithProviders(<ConsumptionRecordsPage />);

    await screen.findByTestId('review-400');
    const [first] = calls.callsTo('/api/console/consumptionRecord/findConsumptionRecords');
    expect(first.body).toMatchObject({ from: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/), rowsPerPage: 500 });
  });

  it('offers re-pricing only for records whose pricing failed', () => {
    expect(canReprice({ ...exportError, pricingStatus: 'PRICED', exportStatus: 'NEW' })).toBe(false);
    expect(canReprice({ ...exportError, pricingStatus: 'PRICING_FAILED', exportStatus: 'NEW' })).toBe(true);
    expect(canReprice({ ...exportError, pricingStatus: 'PRICING_FAILED', exportStatus: 'PROCESSED' })).toBe(false);
    expect(canReprice({ ...record, pricingStatus: 'PRICING_FAILED' })).toBe(false);
  });
});
