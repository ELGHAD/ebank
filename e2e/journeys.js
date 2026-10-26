import { expect, unique } from "./fixtures.js";

export const NEW_PASSWORD = "E2e-chosen-password-1";

/** Agent onboards a client (synthetic identity) and returns its login and one-time password. */
export async function onboardClient(page, app, firstName) {
  const email = `${unique(firstName.toLowerCase())}@e2e.ebank.test`;
  await page.goto("/admin/clients/new");
  await page.getByLabel("First name").fill(firstName);
  await page.getByLabel("Last name").fill("Tester");
  await page.getByLabel("Email (used as login)").fill(email);
  await page.getByLabel("Identity reference").fill(unique("ID").toUpperCase());
  await page.getByRole("button", { name: "Create client" }).click();
  await expect(page.getByRole("heading", { level: 1, name: "Client onboarded" })).toBeVisible();
  return { email, temporaryPassword: await app.readOneTimePassword() };
}

/** From the onboarding result, opens an account with an optional deposit and returns its RIB. */
export async function openAccount(page, deposit) {
  await page.getByRole("link", { name: "Open an account for this client" }).click();
  if (deposit) await page.getByLabel("Initial deposit (simulated funds)").fill(deposit);
  await page.getByRole("button", { name: "Open account" }).click();
  await expect(page.getByText(/^Account EB\d{22} opened with/)).toBeVisible();
  const rib = (await page.getByRole("region", { name: "Client accounts" }).locator("td.mono").first().textContent()).trim();
  expect(rib).toMatch(/^EB\d{22}$/);
  return rib;
}

/** Signs in with a one-time password, replaces it, and lands on the role's home page. */
export async function firstSignIn(page, app, username, temporaryPassword, home) {
  await app.login(username, temporaryPassword);
  await app.replaceTemporaryPassword(temporaryPassword, NEW_PASSWORD);
  await expect(page).toHaveURL(new RegExp(`${home}$`));
}
