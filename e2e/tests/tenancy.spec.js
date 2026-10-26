import { test, expect } from "../fixtures.js";
import { addStaff, createBankWithAdmin, platformAction } from "../tenancy.js";
import { NEW_PASSWORD, firstSignIn } from "../journeys.js";

test("a new bank is isolated from the others and can be suspended by the platform", async ({ page, app }) => {
  const bank = await createBankWithAdmin(page, app);

  const agent = await addStaff(page, app);
  expect(agent.error).toBeUndefined();
  await app.logout();

  // The new bank's agent sees none of the demo bank's clients (created by the other journeys)
  await firstSignIn(page, app, agent.email, agent.temporaryPassword, "/admin");
  await expect(page.getByText("No clients yet. Start by onboarding one.")).toBeVisible();
  await page.getByLabel("Search clients").fill("e2e.ebank.test");
  await page.getByRole("button", { name: "Search" }).click();
  await expect(page.getByText(/No client matches/)).toBeVisible();
  await app.logout();

  // Suspension locks every user of that bank out, with the same generic message as a wrong password
  await platformAction(page, app, bank.name, "Suspend", "Suspend");
  await app.login(agent.email, NEW_PASSWORD);
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page).toHaveURL(/\/login$/);

  await platformAction(page, app, bank.name, "Reactivate", "Reactivate");
  await app.login(agent.email, NEW_PASSWORD);
  await expect(page).toHaveURL(/\/admin$/);
});
