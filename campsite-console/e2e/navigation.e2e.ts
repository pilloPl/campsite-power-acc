import { expect, test } from '@playwright/test';
import { TABS } from '../src/tabs';
import { loginAs, openTab } from './support';

test.describe('login and navigation', () => {
  test('a wrong password is refused on the login page', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('not-the-password');
    await page.getByRole('button', { name: 'Log in' }).click();
    await expect(page.getByTestId('login-error')).toBeVisible();
  });

  for (const username of ['reception', 'host', 'service', 'backoffice', 'admin.es']) {
    test(`${username} opens each of its tabs without a server error`, async ({ page }) => {
      test.setTimeout(5 * 60_000);
      const serverErrors: string[] = [];
      page.on('response', (response) => {
        if (response.url().includes('/api/') && response.status() >= 500) {
          serverErrors.push(`${response.status()} ${new URL(response.url()).pathname}`);
        }
      });
      await loginAs(page, username);
      const labels = await page.getByRole('tab').evaluateAll((tabs) => tabs.map((tab) => tab.getAttribute('aria-label') ?? ''));
      expect(labels.length).toBeGreaterThan(0);
      for (const label of labels) {
        await openTab(page, label);
        await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      }
      expect(serverErrors, `no request of the tabs of ${username} answers 5xx`).toEqual([]);
    });
  }

  test('reception sees only its tabs', async ({ page }) => {
    await loginAs(page, 'reception');
    const tabs = page.getByRole('tab');
    await expect(tabs).toHaveCount(5);
    await expect(page.getByRole('tab', { name: 'Supply sessions' })).toBeVisible();
    await expect(page.getByRole('tab', { name: 'Administration' })).toHaveCount(0);
  });

  test('the market admin opens every tab and each shows its data without a fault', async ({ page }) => {
    test.setTimeout(5 * 60_000);
    const serverErrors: string[] = [];
    page.on('response', (response) => {
      if (response.url().includes('/api/') && response.status() >= 500) {
        serverErrors.push(`${response.status()} ${new URL(response.url()).pathname}`);
      }
    });
    await loginAs(page, 'admin');
    for (const tab of TABS) {
      await openTab(page, tab.label);
      await expect(page).toHaveURL(new RegExp(`/${tab.path}(\\?.*)?$`));
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('main .MuiAlert-standardError'), `no fault on tab ${tab.label}`).toHaveCount(0);
    }
    expect(serverErrors, 'no request of any tab answers 5xx').toEqual([]);
  });

  test('logging out returns to the login page', async ({ page }) => {
    await loginAs(page, 'service');
    await page.getByRole('button', { name: 'Log out' }).click();
    await expect(page.getByRole('button', { name: 'Log in' })).toBeVisible();
  });
});
