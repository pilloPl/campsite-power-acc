import { expect, test, type Page } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { TABS } from '../src/tabs';
import { addWhitelistFilter, assignWelcomePoolCard, checkOutStayWithOpenSession, reviewSuspectRecord, simulatePrice, stopSupplyFromMapPopup } from './actions';
import { loginAs, openTab, settle, waitForMap } from './support';

/**
 * `npm run screenshots`: docs/screenshots/<nn>-<tab>.png of all 16 tabs (as the market admin, who sees all) and of
 * the key actions, at 1440x900, against the backend started with the demo data.
 */
const here = path.dirname(fileURLToPath(import.meta.url));
const target = path.resolve(here, '../../docs/screenshots');

async function shoot(page: Page, name: string) {
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(target, `${name}.png`) });
}

test.describe.configure({ mode: 'serial' });

test('screenshots of all tabs and the key actions', async ({ page }) => {
  test.setTimeout(5 * 60_000);
  await page.goto('/login');
  await expect(page.getByRole('button', { name: 'Log in' })).toBeVisible();
  await shoot(page, '00-login');

  await loginAs(page, 'admin');
  for (const tab of TABS) {
    await openTab(page, tab.label);
    if (tab.path === 'map') await waitForMap(page);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await shoot(page, `${String(tab.no).padStart(2, '0')}-${tab.path}`);
  }

  await stopSupplyFromMapPopup(page, () => shoot(page, '17-map-popup-remote-stop'));
  await shoot(page, '18-map-popup-stop-accepted');

  await simulatePrice(page);
  await shoot(page, '19-tariff-price-simulator');

  await checkOutStayWithOpenSession(page);
  await shoot(page, '20-check-out-closes-sessions');

  const review = await reviewSuspectRecord(page);
  await shoot(page, '21-suspect-review');
  await review.decide();
  await shoot(page, '22-suspect-review-approved');

  await assignWelcomePoolCard(page, () => shoot(page, '23-cards-welcome-pool-assignment'));
  await shoot(page, '24-cards-welcome-pool-assigned');

  await addWhitelistFilter(page, () => shoot(page, '25-issuer-whitelist-filter-dialog'));
  await shoot(page, '26-issuer-whitelist-filters');

  await openTab(page, 'Organisation');
  await page.getByRole('button', { name: 'Card issuers' }).click();
  await settle(page);
  await shoot(page, '27-organisation-issuer-tree');
  await page.getByRole('button', { name: 'Staff' }).click();
  await settle(page);
  await shoot(page, '28-organisation-staff-tree');
});
