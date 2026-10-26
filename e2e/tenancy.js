import { expect, unique } from "./fixtures.js";
import { PLATFORM_ADMIN } from "./credentials.js";
import { firstSignIn } from "./journeys.js";

/** Platform admin creates a new bank (FREE plan by default); its first admin signs in and lands on /org. */
export async function createBankWithAdmin(page, app) {
  const name = unique("E2E Bank");
  const adminEmail = `${unique("admin")}@e2e.ebank.test`;
  await app.login(PLATFORM_ADMIN.username, PLATFORM_ADMIN.password());
  await expect(page).toHaveURL(/\/platform$/);
  await page.getByLabel("Organization name").fill(name);
  await page.getByLabel("First administrator's email").fill(adminEmail);
  await page.getByRole("button", { name: "Create organization" }).click();
  const temporaryPassword = await app.readOneTimePassword();
  await expect(orgRow(page, name)).toContainText("FREE");
  await app.logout();

  await firstSignIn(page, app, adminEmail, temporaryPassword, "/org");
  await expect(page.getByRole("heading", { level: 1, name })).toBeVisible();
  return { name, adminEmail };
}

export const orgRow = (page, name) => page.getByRole("region", { name: "Organizations" }).getByRole("row").filter({ hasText: name });

/** Org admin adds a staff member; returns the one-time password, or null when the UI shows an error instead. */
export async function addStaff(page, app, role = "AGENT") {
  const email = `${unique("staff")}@e2e.ebank.test`;
  await page.goto("/org/staff");
  const another = page.getByRole("button", { name: "Add another staff member" });
  if (await another.isVisible()) await another.click();
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Role").selectOption(role);
  await page.getByRole("button", { name: "Add staff member" }).click();
  const outcome = page.getByRole("status").filter({ hasText: "Temporary password" }).or(page.locator("#staff-error"));
  await expect(outcome).toBeVisible();
  if (await page.locator("#staff-error").isVisible()) {
    return { email, error: (await page.locator("#staff-error").textContent()).trim() };
  }
  return { email, temporaryPassword: await app.readOneTimePassword() };
}

/** Platform admin runs one inline-confirmed action on a bank's row. */
export async function platformAction(page, app, orgName, label, confirmLabel) {
  await app.login(PLATFORM_ADMIN.username, PLATFORM_ADMIN.password());
  const row = orgRow(page, orgName);
  await row.getByRole("button", { name: label }).click();
  await row.getByRole("group").getByRole("button", { name: confirmLabel }).click();
  await expect(page.locator(".alertOk")).toBeVisible();
  await app.logout();
}
