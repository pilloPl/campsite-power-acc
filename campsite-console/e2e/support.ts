import { expect, type Page } from '@playwright/test';

/** Logs in through the login page with a demo user (password demo-<username>). */
export async function loginAs(page: Page, username: string) {
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(`demo-${username}`);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page.getByTestId('current-user')).toBeVisible();
}

/** Opens a tab by its label and waits until the page has loaded its data. */
export async function openTab(page: Page, label: string) {
  await closeDialogs(page);
  await page.getByRole('tab', { name: label, exact: true }).click();
  await settle(page);
}

/** Waits until no request is running and no spinner is shown. */
export async function settle(page: Page) {
  // pages that poll (jobs, dashboard) may never be fully idle; the spinners decide
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => undefined);
  await expect(page.locator('.MuiCircularProgress-root')).toHaveCount(0, { timeout: 20_000 });
}

/** Waits for the OpenStreetMap tiles and the map shapes of the map tab. */
export async function waitForMap(page: Page) {
  // the tiles come from openstreetmap.org: a slow tile server must not fail the console's tests
  await expect(page.locator('.leaflet-tile-loaded').first()).toBeVisible({ timeout: 30_000 })
    .catch(() => console.warn('OpenStreetMap tiles did not load within 30 s'));
  await expect(page.locator('path.leaflet-interactive').first()).toBeAttached();
  await page.waitForTimeout(1500);
}

/** Supplying pedestals are drawn blue on the map. */
const SUPPLYING_MARKER = 'path.leaflet-interactive[fill="#1565c0"]';

/**
 * Opens the popup of a supplying pedestal that can be stopped remotely (link v2/v3 pedestals of the demo answer
 * through the demo pedestal simulator) and answers the enabled stop button.
 */
export async function openStoppablePedestalPopup(page: Page) {
  await waitForMap(page);
  const markers = page.locator(SUPPLYING_MARKER);
  const count = await markers.count();
  for (let i = 0; i < count; i++) {
    await markers.nth(i).click({ force: true });
    const popup = page.getByTestId('pedestal-popup');
    await expect(popup).toBeVisible();
    const stop = popup.locator('[data-testid^="popup-stop-"]:enabled');
    if ((await stop.count()) > 0) return stop.first();
    await page.keyboard.press('Escape');
    await page.locator('.leaflet-popup-close-button').click({ force: true }).catch(() => undefined);
  }
  throw new Error('No supplying pedestal with remote control on the map');
}

/** Closes an open dialog (it hides the rest of the page from the accessibility tree). */
export async function closeDialogs(page: Page) {
  for (let i = 0; i < 3 && (await page.getByRole('dialog').count()) > 0; i++) {
    await page.keyboard.press('Escape');
    await page.waitForTimeout(300);
  }
}
