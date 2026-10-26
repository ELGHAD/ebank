import { defineConfig, devices } from "@playwright/test";

/**
 * Runs against an already started stack (docker compose up). Tests create their own uniquely named
 * organizations and clients, but they share the demo bank and the platform, so they run one at a time.
 */
export default defineConfig({
  testDir: "./tests",
  globalSetup: "./global-setup.js",
  fullyParallel: false,
  workers: 1,
  retries: 0, // a flaky end-to-end test is a defect to fix, not to retry away
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: process.env.CI ? [["list"], ["github"], ["html", { open: "never" }]] : [["list"]], // "github": failures as annotations on the run
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    // Locally, set PW_CHANNEL=chrome to use the installed Chrome instead of downloading Chromium
    ...(process.env.PW_CHANNEL ? { channel: process.env.PW_CHANNEL } : {}),
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
