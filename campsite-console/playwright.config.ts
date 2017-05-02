import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests and screenshots run against the backend started with the profile `demo` (README) and the Vite
 * dev server of the console, which proxies /api to the backend (CAMPSITE_BACKEND_URL, default http://localhost:8080).
 */
const baseURL = process.env.CONSOLE_URL ?? 'http://localhost:5173';

export default defineConfig({
  testDir: './e2e',
  timeout: 120_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    ...devices['Desktop Chrome'],
    baseURL,
    viewport: { width: 1440, height: 900 },
    trace: 'retain-on-failure',
    // the full Chromium build in the new headless mode (npx playwright install chromium)
    channel: 'chromium',
  },
  projects: [
    { name: 'e2e', testMatch: /.*\.e2e\.ts/ },
    { name: 'screenshots', testMatch: /screenshots\.ts/ },
  ],
  webServer: process.env.CONSOLE_URL
    ? undefined
    : { command: 'npm run dev', url: baseURL, reuseExistingServer: true, timeout: 60_000 },
});
