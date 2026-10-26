import { test, expect } from "../fixtures.js";
import { NEW_PASSWORD } from "../journeys.js";
import { addStaff, createBankWithAdmin, platformAction } from "../tenancy.js";

test("plan limits, upgrade with a simulated invoice, and a failed payment blocking growth only", async ({ page, app }) => {
  // FREE allows 3 staff: the admin plus two agents
  const bank = await createBankWithAdmin(page, app);
  expect((await addStaff(page, app)).error).toBeUndefined();
  expect((await addStaff(page, app)).error).toBeUndefined();
  const refused = await addStaff(page, app);
  expect(refused.error).toContain("Free plan allows 3 staff members");

  await page.getByRole("link", { name: "Plan & billing" }).click();
  await expect(page.getByRole("meter", { name: "staff members" })).toHaveAttribute("aria-valuenow", "3");
  await expect(page.getByText("No invoices: the Free plan is not billed.")).toBeVisible();

  // Upgrade: confirmed inline, invoiced (simulated), the limit is lifted at once
  await page.getByRole("button", { name: "Switch to Pro" }).click();
  await page.getByRole("group").getByRole("button", { name: "Switch" }).click();
  await expect(page.getByText("You are now on Pro. A simulated invoice of 49.00 EUR was issued.")).toBeVisible();
  await expect(page.getByRole("region", { name: "Invoices", exact: true })).toContainText("49.00 EUR");
  expect((await addStaff(page, app)).error).toBeUndefined();
  await app.logout();

  // A failed payment blocks growth...
  await platformAction(page, app, bank.name, "Simulate failed payment", "Simulate");
  await app.login(bank.adminEmail, NEW_PASSWORD);
  await expect(page).toHaveURL(/\/org$/);
  const blocked = await addStaff(page, app);
  expect(blocked.error).toContain("subscription payment is overdue");
  await page.goto("/org/billing");
  await expect(page.getByRole("alert")).toContainText("Payment overdue (simulated)");
  await app.logout();

  // ...until the payment is recorded
  await platformAction(page, app, bank.name, "Record payment", "Record payment");
  await app.login(bank.adminEmail, NEW_PASSWORD);
  await expect(page).toHaveURL(/\/org$/);
  expect((await addStaff(page, app)).error).toBeUndefined();
});
