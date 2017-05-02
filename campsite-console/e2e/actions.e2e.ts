import { expect, test } from '@playwright/test';
import { addWhitelistFilter, assignWelcomePoolCard, checkOutStayWithOpenSession, reviewSuspectRecord, simulatePrice, stopSupplyFromMapPopup } from './actions';
import { loginAs } from './support';

test.describe('key actions', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin');
  });

  test('the map popup stops a supply remotely and the pedestal ends the session', async ({ page }) => {
    await stopSupplyFromMapPopup(page);
  });

  test('the tariff price simulator prices a hypothetical session', async ({ page }) => {
    await simulatePrice(page);
  });

  test('check-out of a stay closes its open supply sessions', async ({ page }) => {
    await checkOutStayWithOpenSession(page);
    await expect(page.getByTestId('checkout-result')).toContainText(/session/i);
  });

  test('a card of the welcome pool is handed to a guest staying now', async ({ page }) => {
    await assignWelcomePoolCard(page);
  });

  test('a whitelist filter keeps the cards of an issuer group out of the card import', async ({ page }) => {
    await addWhitelistFilter(page);
  });

  test('a SUSPECT record is approved in the review queue', async ({ page }) => {
    const review = await reviewSuspectRecord(page);
    await review.decide();
  });
});
