import { test as base, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

/**
 * Shared fixtures:
 * - every test fails on a Content-Security-Policy violation or an unexpected console error;
 * - `app` offers the few actions every journey needs, written against accessible names (labels, roles).
 */
export const test = base.extend({
  consoleGuard: [
    async ({ page }, use) => {
      const errors = [];
      page.on("console", (m) => m.type() === "error" && errors.push(m.text()));
      await use(errors);
      // 4xx answers are expected in negative scenarios; the browser logs them as errors
      const unexpected = errors.filter((e) => !/status of 4\d\d/.test(e));
      expect(unexpected, "console errors / CSP violations").toEqual([]);
    },
    { auto: true },
  ],

  app: async ({ page }, use) => {
    const app = {
      async login(username, password) {
        await page.goto("/login");
        await page.getByLabel("Email").fill(username);
        await page.getByLabel("Password", { exact: true }).fill(password);
        await page.getByRole("button", { name: "Login" }).click();
      },
      async logout() {
        await page.getByRole("button", { name: "Logout" }).click();
        await expect(page).toHaveURL(/\/login$/);
      },
      /** First sign-in with a one-time password: choose a new one. */
      async replaceTemporaryPassword(temporary, chosen) {
        await expect(page).toHaveURL(/\/change-password$/);
        await page.getByLabel(/Temporary password|Current password/).fill(temporary);
        await page.getByLabel(/New password/).fill(chosen);
        await page.getByLabel("Confirm new password").fill(chosen);
        await page.getByRole("button", { name: "Save password" }).click();
      },
      /** The one-time password shown once after creating an account for someone else. */
      async readOneTimePassword() {
        const box = page.getByRole("status").filter({ hasText: "Temporary password" });
        await expect(box).toBeVisible();
        return (await box.locator("dd.mono").textContent()).trim();
      },
      /** axe-core scan; any violation fails (all impacts, not only serious). */
      async expectAccessible(label) {
        const results = await new AxeBuilder({ page }).analyze();
        const summary = results.violations.map((v) => `${v.id} [${v.impact}] x${v.nodes.length}`);
        expect(summary, `axe-core violations on ${label}`).toEqual([]);
      },
      async expectNoHorizontalScroll() {
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
        expect(overflow, "horizontal page overflow (px)").toBeLessThanOrEqual(0);
      },
    };
    await use(app);
  },
});

export { expect };

export const unique = (prefix) => `${prefix}-${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
