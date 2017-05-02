import { expect, type Page } from '@playwright/test';
import { openStoppablePedestalPopup, openTab, settle } from './support';

/**
 * Key actions of the console, shared by the end-to-end tests and the screenshots. Each leaves the page in the state
 * the screenshot shows.
 */

/** Map popup: stop the supply of a socket remotely; the pedestal accepts and the popup refreshes. */
export async function stopSupplyFromMapPopup(page: Page, beforeStop?: () => Promise<void>) {
  await openTab(page, 'Map');
  const stop = await openStoppablePedestalPopup(page);
  if (beforeStop) await beforeStop();
  await stop.click();
  await page.getByRole('button', { name: 'Confirm' }).click();
  await expect(page.getByTestId('notification')).toContainText(/accepted/i);
  await expect(page.getByTestId('pedestal-popup')).toBeVisible();
  // the pedestal reports the end of the session a moment later; the popup reloads
  await page.waitForTimeout(3500);
}

/** Tariffs: price of a hypothetical session at a socket. */
export async function simulatePrice(page: Page) {
  await openTab(page, 'Tariffs');
  const simulator = page.getByTestId('price-simulator');
  await simulator.scrollIntoViewIfNeeded();
  await simulator.getByRole('button', { name: 'Simulate' }).click();
  await expect(page.getByTestId('simulation-result')).toBeVisible();
  await expect(page.getByTestId('simulation-cost')).toContainText(/\d+\.\d{2}/);
  await page.getByTestId('simulation-result').scrollIntoViewIfNeeded();
}

/** Guests & stays: check-out of a stay with a running supply; the check-out closes the open sessions. */
export async function checkOutStayWithOpenSession(page: Page) {
  await openTab(page, 'Guests & stays');
  await page.locator('[data-testid^="checkout-"]').first().click();
  await expect(page.getByTestId('checkout-open-sessions')).toBeVisible();
  await page.getByTestId('checkout-confirm').click();
  await expect(page.getByTestId('checkout-result')).toBeVisible();
  const runJob = page.getByTestId('checkout-run-job');
  if (await runJob.isVisible()) {
    await runJob.click();
    await expect(page.getByTestId('notification')).toBeVisible();
  }
  await settle(page);
}

/** Consumption records: approve a SUSPECT record of the review queue. */
export async function reviewSuspectRecord(page: Page, decision: 'Approve' | 'Reject' = 'Approve') {
  await openTab(page, 'Consumption records');
  await page.getByTestId('review-queue').getByRole('button', { name: 'Review' }).first().click();
  const dialog = page.getByTestId('review-dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByLabel('Comment').fill('Pitch has a fridge and an air conditioner, consumption is plausible');
  return {
    dialog,
    decide: async () => {
      await dialog.getByRole('button', { name: decision }).click();
      await expect(page.getByTestId('notification')).toContainText(decision === 'Approve' ? /approved/i : /rejected/i);
    },
  };
}

/** Cards: a card of the welcome card pool is handed to a guest staying now. */
export async function assignWelcomePoolCard(page: Page, beforeAssign?: () => Promise<void>) {
  await openTab(page, 'Cards');
  await page.getByTestId('assign-welcome').click();
  const dialog = page.getByTestId('welcome-pool-dialog');
  await expect(dialog.getByTestId('welcome-pool-cards')).toBeVisible();
  await dialog.getByLabel('Guest staying now').click();
  await page.getByRole('option').first().click();
  if (beforeAssign) await beforeAssign();
  await dialog.getByTestId('welcome-assign').click();
  await expect(page.getByTestId('notification')).toContainText(/handed to/);
}

/** Issuer exchange: a whitelist filter keeps the cards of an issuer group out of the card import. */
export async function addWhitelistFilter(page: Page, beforeSave?: () => Promise<void>) {
  await openTab(page, 'Issuer exchange');
  const section = page.getByTestId('whitelist-filters');
  await section.scrollIntoViewIfNeeded();
  await page.getByTestId('add-filter').click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel(/^Card issuer group/).click();
  await page.getByRole('option', { name: 'Network card' }).click();
  await dialog.getByLabel(/^Direction/).click();
  await page.getByRole('option', { name: 'Card import only' }).click();
  if (beforeSave) await beforeSave();
  await dialog.getByRole('button', { name: 'Save filter' }).click();
  await expect(page.getByTestId('notification')).toContainText(/saved/);
  await expect(section.getByText('Kept out').first()).toBeVisible();
  await section.scrollIntoViewIfNeeded();
}
