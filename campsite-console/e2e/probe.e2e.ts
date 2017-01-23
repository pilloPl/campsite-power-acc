import { test } from '@playwright/test';
import { loginAs, settle } from './support';

// Developer probe: PROBE_USER=admin PROBE_TAB=dashboard npx playwright test --project=e2e probe
test('probe', async ({ page }) => {
  test.skip(!process.env.PROBE_TAB, 'developer probe only');
  await loginAs(page, process.env.PROBE_USER ?? 'admin');
  await page.goto(`/${process.env.PROBE_TAB}`);
  await settle(page);
  await page.waitForTimeout(Number(process.env.PROBE_WAIT ?? 800));
  await page.screenshot({ path: process.env.PROBE_OUT ?? `test-results/probe-${process.env.PROBE_TAB}.png`, fullPage: process.env.PROBE_FULL === '1' });
});
