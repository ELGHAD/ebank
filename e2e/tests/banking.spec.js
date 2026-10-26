import { test, expect } from "../fixtures.js";
import { AGENT } from "../credentials.js";
import { NEW_PASSWORD, firstSignIn, onboardClient, openAccount } from "../journeys.js";

test("agent onboards two clients, one transfers money to the other, both see it", async ({ page, app }) => {
  // Agent: two clients, one funded MAD account each
  await app.login(AGENT.username, AGENT.password());
  await expect(page).toHaveURL(/\/admin$/);

  const alice = await onboardClient(page, app, "Alice");
  const aliceRib = await openAccount(page, "1000");
  const bob = await onboardClient(page, app, "Bob");
  const bobRib = await openAccount(page, "");
  await app.logout();

  // Alice: first sign-in, then a transfer through the review step
  await firstSignIn(page, app, alice.email, alice.temporaryPassword, "/client");
  await expect(page.getByRole("region", { name: "Accounts table" })).toContainText(aliceRib);

  await page.getByRole("link", { name: "New transfer" }).first().click();
  await page.getByLabel("Recipient RIB").fill("EB00 1234");
  await page.getByLabel(/^Amount/).fill("250.50");
  await page.getByRole("button", { name: "Review transfer" }).click();
  await expect(page.getByText(/This RIB is not valid/)).toBeVisible(); // checked before anything is sent

  await page.getByLabel("Recipient RIB").fill(bobRib);
  await page.getByLabel(/^Amount/).fill("5000");
  await page.getByRole("button", { name: "Review transfer" }).click();
  await expect(page.getByText("The amount exceeds the available balance.")).toBeVisible();

  await page.getByLabel(/^Amount/).fill("250.50");
  await page.getByLabel("Description (optional)").fill("E2E rent");
  await page.getByRole("button", { name: "Review transfer" }).click();
  await expect(page.getByRole("heading", { name: "Confirm this transfer" })).toBeVisible();
  await page.getByRole("button", { name: "Confirm transfer" }).click();

  const receipt = page.getByRole("status").filter({ hasText: "Transfer completed." });
  await expect(receipt).toContainText("749.50 MAD");
  await receipt.getByRole("link", { name: "View account history" }).click();
  await expect(page.getByRole("region", { name: "Account movements" })).toContainText("E2E rent");
  await app.logout();

  // Bob: receives exactly the amount
  await firstSignIn(page, app, bob.email, bob.temporaryPassword, "/client");
  await expect(page.getByRole("region", { name: "Accounts table" })).toContainText("250.50");
  await app.logout();

  // The new password works and the temporary one no longer does
  await app.login(alice.email, alice.temporaryPassword);
  await expect(page.getByRole("alert")).toBeVisible();
  await app.login(alice.email, NEW_PASSWORD);
  await expect(page).toHaveURL(/\/client$/);
});

test("a client cannot use staff pages or staff APIs", async ({ page, app }) => {
  await app.login(AGENT.username, AGENT.password());
  await expect(page).toHaveURL(/\/admin$/);
  const carol = await onboardClient(page, app, "Carol");
  await app.logout();

  await firstSignIn(page, app, carol.email, carol.temporaryPassword, "/client");
  for (const path of ["/admin", "/org", "/platform"]) {
    await page.goto(path);
    await expect(page.getByRole("alert")).toHaveText("You are not authorized to access this page.");
  }
  // The UI guard is a convenience; the backend refuses on its own, with the browser's real session cookie
  for (const path of ["/api/admin/clients", "/api/org/staff", "/api/platform/organizations"]) {
    expect((await page.request.get(path)).status(), path).toBe(403);
  }
});
