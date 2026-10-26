import { test, expect } from "../fixtures.js";
import { AGENT, ORG_ADMIN, PLATFORM_ADMIN } from "../credentials.js";
import { NEW_PASSWORD, onboardClient, openAccount } from "../journeys.js";

// Every page of every role, scanned with axe-core on a desktop and on a phone-sized viewport
const VIEWPORTS = [
  { name: "desktop", width: 1280, height: 800 },
  { name: "phone", width: 375, height: 740 },
];

async function scan(page, app, path, viewport) {
  await page.goto(path);
  await expect(page).toHaveURL((url) => url.pathname === path); // a redirect must not pass as a scan of `path`
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(page.getByText(/^Loading/)).toHaveCount(0);
  await app.expectAccessible(`${path} (${viewport.name})`);
  await app.expectNoHorizontalScroll();
}

for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.name} ${viewport.width}px`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test("sign-in page", async ({ page, app }) => {
      await scan(page, app, "/login", viewport);
    });

    test("agent and client pages", async ({ page, app }) => {
      // Ten page scans plus onboarding: observed past 90 s on a loaded machine, so this test gets 3x the budget
      test.slow();
      await app.login(AGENT.username, AGENT.password());
      await expect(page).toHaveURL(/\/admin$/);
      const client = await onboardClient(page, app, "Dora");
      await app.expectAccessible(`onboarding result (${viewport.name})`);
      await openAccount(page, "300");
      const clientPage = new URL(page.url()).pathname;
      for (const path of ["/admin", "/admin/clients/new", clientPage, "/admin/audit"]) {
        await scan(page, app, path, viewport);
      }
      await page.goto(clientPage);
      await page.getByRole("link", { name: "History" }).first().click();
      await scan(page, app, new URL(page.url()).pathname, viewport);
      await app.logout();

      await app.login(client.email, client.temporaryPassword);
      await expect(page).toHaveURL(/\/change-password$/);
      await scan(page, app, "/change-password", viewport);
      await app.replaceTemporaryPassword(client.temporaryPassword, NEW_PASSWORD);
      await expect(page).toHaveURL(/\/client$/);
      for (const path of ["/client", "/client/transfer"]) {
        await scan(page, app, path, viewport);
      }
    });

    test("bank admin pages", async ({ page, app }) => {
      await app.login(ORG_ADMIN.username, ORG_ADMIN.password());
      await expect(page).toHaveURL(/\/org$/);
      for (const path of ["/org", "/org/staff", "/org/billing", "/org/audit"]) {
        await scan(page, app, path, viewport);
      }
    });

    test("platform admin page", async ({ page, app }) => {
      await app.login(PLATFORM_ADMIN.username, PLATFORM_ADMIN.password());
      await expect(page).toHaveURL(/\/platform$/);
      await scan(page, app, "/platform", viewport);
    });
  });
}

test("security headers are served with the application", async ({ request }) => {
  const res = await request.get("/login");
  const headers = res.headers();
  expect(headers["content-security-policy"]).toContain("default-src 'self'");
  expect(headers["x-content-type-options"]).toBe("nosniff");
  expect(headers["referrer-policy"]).toBeTruthy();
});
