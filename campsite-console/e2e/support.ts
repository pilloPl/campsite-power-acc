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
  await page.getByRole('tab', { name: label, exact: true }).click();
  await settle(page);
}

/** Waits until no request is running and no spinner is shown. */
export async function settle(page: Page) {
  await page.waitForLoadState('networkidle');
  await expect(page.locator('.MuiCircularProgress-root')).toHaveCount(0, { timeout: 20_000 });
}
