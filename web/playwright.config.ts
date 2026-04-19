import { defineConfig, devices } from '@playwright/test'

// Runs Playwright against the Firebase Hosting emulator (port 5002).
// Assumes `firebase emulators:start` is running manually OR CI starts emulators before `npm run e2e`.
// The web app must have been built (`npm run build`) before running e2e, since emulator serves from hosting/public.
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  reporter: 'list',
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5002',
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
