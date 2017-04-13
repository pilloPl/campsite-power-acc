import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TariffsPage, { copyOf } from './TariffsPage';
import { chainOption } from './tariffs/AssignmentChain';
import { restrictionText } from './tariffs/TariffElements';
import { adminUser, fakeBackend } from '../test/backend';
import { renderWithProviders } from '../test/render';

const nightSaver = {
  id: 900000001, name: 'HR NIGHT SAVER', market: 'HR', currency: 'EUR', defaultTariff: false,
  priceText: '22:00-06:00: 0.32 EUR/kWh; 0.48 EUR/kWh + 0.50 EUR/session',
  elements: [
    { id: 1, position: 0, priceComponents: [{ id: 1, dimensionType: 'ENERGY', price: 0.32, stepSize: 1, priceUnit: 'PER_KWH' }],
      restriction: { startTime: '22:00:00', endTime: '06:00:00', daysOfWeek: [] } },
    { id: 2, position: 1, priceComponents: [
      { id: 2, dimensionType: 'ENERGY', price: 0.48, stepSize: 1, priceUnit: 'PER_KWH' },
      { id: 3, dimensionType: 'FLAT', price: 0.5, stepSize: 1, priceUnit: 'PER_SESSION' }], restriction: undefined },
  ],
};
const standard = {
  id: 1, name: 'HR STANDARD', market: 'HR', currency: 'EUR', defaultTariff: true, priceText: '0.45 EUR/kWh',
  elements: [{ id: 4, position: 0, priceComponents: [{ id: 4, dimensionType: 'ENERGY', price: 0.45, stepSize: 1, priceUnit: 'PER_KWH' }] }],
};

function backend() {
  return fakeBackend({
    '/api/console/auth/currentUser': { user: adminUser },
    '/api/console/map/getMapData': { map: { campsites: [{ groupId: 1, displayName: 'Camp Istria Bay', pedestals: [
      { code: 'ISTRIABAY-P04', sockets: [{ id: 41, socketCode: 'ISTRIABAY*P04*1', pitchNo: 'B01' }] }] }] } },
    '/api/console/tariff/findTariffs': { tariffs: [nightSaver, standard] },
    '/api/console/tariff/findSocketTariff': { socketTariff: {
      socketId: 41, socketCode: 'ISTRIABAY*P04*1', pitchNo: 'B01', source: 'PEDESTAL', tariff: nightSaver,
      assignments: [{ id: 7, tariffId: nightSaver.id, tariffName: 'HR NIGHT SAVER', pedestalCode: 'ISTRIABAY-P04', startDate: '2026-08-18T07:40:00' }],
    } },
    '/api/console/tariff/simulatePrice': { simulation: {
      status: 'PRICED', cost: 5.36, currency: 'EUR', costText: '5.36 EUR', tariffName: 'HR NIGHT SAVER', tariffSource: 'PEDESTAL',
      lines: [
        { elementPosition: 0, dimensionType: 'ENERGY', price: 0.32, priceUnit: 'PER_KWH', share: 0.57, billedQuantity: 7.143, amount: 2.29 },
        { elementPosition: 1, dimensionType: 'FLAT', price: 0.5, priceUnit: 'PER_SESSION', share: 1, billedQuantity: 1, amount: 0.5 },
      ],
    } },
    '/api/console/tariff/setMarketDefaultTariff': { tariff: { ...nightSaver, defaultTariff: true } },
    '/api/console/tariff/saveTariff': (body: { tariff: { name: string } }) => ({ tariff: { ...body.tariff, id: 99 } }),
  });
}

describe('tariffs tab', () => {
  it('shows the elements of the market default tariff in the builder', async () => {
    backend();
    renderWithProviders(<TariffsPage />);

    const detail = await screen.findByTestId('tariff-detail');
    expect(await within(detail).findByText('Tariff builder: HR STANDARD')).toBeInTheDocument();
    expect(within(detail).getByText('always (fallback)')).toBeInTheDocument();
  });

  it('simulates the price of a hypothetical supply at a socket', async () => {
    const calls = backend();
    renderWithProviders(<TariffsPage />);

    const simulator = await screen.findByTestId('price-simulator');
    await waitFor(() => expect(within(simulator).getByRole('button', { name: 'Simulate' })).toBeEnabled());
    await userEvent.clear(within(simulator).getByLabelText('kWh'));
    await userEvent.type(within(simulator).getByLabelText('kWh'), '12.5');
    await userEvent.click(within(simulator).getByRole('button', { name: 'Simulate' }));

    expect(await screen.findByTestId('simulation-cost')).toHaveTextContent('5.36 EUR');
    expect(screen.getByTestId('simulation-result')).toHaveTextContent('HR NIGHT SAVER from the pedestal');
    const [call] = calls.callsTo('/api/console/tariff/simulatePrice');
    expect(call.body).toMatchObject({ socketId: 41, kwh: 12.5, nights: 1, plugIn: expect.stringMatching(/T18:00:00$/) });
    expect(call.body).not.toHaveProperty('tariffId');
  });

  it('shows the assignment chain of a socket with the winning link', async () => {
    backend();
    renderWithProviders(<TariffsPage />);

    const chain = await screen.findByTestId('assignment-chain');
    expect(await within(chain).findByText('ISTRIABAY-P04')).toBeInTheDocument();
    expect(within(chain).getByTestId('echarts-option')).toHaveTextContent('Pedestal\\nHR NIGHT SAVER');
  });

  it('makes a tariff the market default after a confirmation', async () => {
    const calls = backend();
    renderWithProviders(<TariffsPage />);

    await userEvent.click(await screen.findByText('HR NIGHT SAVER'));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Set as market default' })).toBeEnabled());
    await userEvent.click(screen.getByRole('button', { name: 'Set as market default' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(calls.callsTo('/api/console/tariff/setMarketDefaultTariff')[0]?.body)
      .toEqual({ tariffId: 900000001 }));
  });

  it('saves a new tariff from the builder', async () => {
    const calls = backend();
    renderWithProviders(<TariffsPage />);

    await screen.findByText('HR NIGHT SAVER');
    await waitFor(() => expect(screen.getByTestId('new-tariff')).toBeEnabled());
    await userEvent.click(screen.getByTestId('new-tariff'));
    const editor = screen.getByTestId('tariff-editor');
    await userEvent.type(within(editor).getByLabelText(/Name/), 'HR WEEKEND');
    await userEvent.type(within(editor).getByLabelText(/Price/), '0.4');
    await userEvent.click(within(editor).getByRole('button', { name: 'Save tariff' }));

    await waitFor(() => expect(calls.callsTo('/api/console/tariff/saveTariff')).toHaveLength(1));
    expect(calls.callsTo('/api/console/tariff/saveTariff')[0].body).toMatchObject({ tariff: {
      name: 'HR WEEKEND', market: 'HR', currency: 'EUR',
      elements: [{ position: 0, priceComponents: [{ dimensionType: 'ENERGY', price: 0.4, priceUnit: 'PER_KWH' }] }],
    } });
  });

  it('shows publish disabled because a saved tariff is already live', async () => {
    backend();
    renderWithProviders(<TariffsPage />);

    expect(await screen.findByRole('button', { name: 'Publish' })).toBeDisabled();
  });

  it('describes restrictions and copies tariffs without ids', () => {
    expect(restrictionText({ startTime: '22:00:00', endTime: '06:00:00', daysOfWeek: ['SATURDAY'], maxKwh: 10 }))
      .toEqual(['22:00-06:00', 'Sat', 'up to 10 kWh']);
    const copy = copyOf(nightSaver);
    expect(copy.id).toBeUndefined();
    expect(copy.elements?.[1].priceComponents?.[1].id).toBeUndefined();
    expect(copy.name).toBe('HR NIGHT SAVER COPY');
    expect(JSON.stringify(chainOption('MARKET_DEFAULT', 'HR STANDARD'))).toContain('Socket\\nnone');
  });
});
