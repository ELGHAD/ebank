import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import ClientDetailPage from "./ClientDetailPage";
import * as agentService from "../../services/agentService";
import { agentUser, renderAt } from "../../test/renderWithProviders";

const routes = <Route path="/admin/clients/:clientId" element={<ClientDetailPage />} />;

const details = (accounts) => ({
  data: {
    client: { id: 3, firstName: "Sara", lastName: "Demo", email: "sara@test.ebank", identityRef: "CIN-3", birthDate: null, address: null, createdAt: "2026-10-01T10:00:00Z" },
    passwordChangeRequired: true,
    accounts,
  },
});
const funded = { id: 10, rib: "EB3094712094552729193874", currency: "MAD", balance: 250, status: "ACTIVE", clientId: 3 };
const empty = { id: 11, rib: "EB3777744714656519320252", currency: "EUR", balance: 0, status: "BLOCKED", clientId: 3 };

const rowOf = (rib) => screen.getByText(rib).closest("tr");

describe("ClientDetailPage", () => {
  it("shows the profile, onboarding state and accounts", async () => {
    vi.spyOn(agentService, "getClient").mockResolvedValue(details([funded, empty]));
    renderAt("/admin/clients/3", routes, agentUser);

    expect(await screen.findByRole("heading", { level: 1, name: "Sara Demo" })).toBeInTheDocument();
    expect(screen.getByText("Temporary password not changed yet")).toBeInTheDocument();
    expect(within(rowOf(funded.rib)).getByText("Active")).toBeInTheDocument();
    expect(within(rowOf(empty.rib)).getByText("Blocked")).toBeInTheDocument();
    expect(within(rowOf(funded.rib)).getByRole("link", { name: "History" })).toHaveAttribute("href", "/admin/accounts/10");
  });

  it("asks for confirmation before blocking, then refreshes", async () => {
    const getClient = vi.spyOn(agentService, "getClient").mockResolvedValue(details([funded]));
    const change = vi.spyOn(agentService, "changeAccountStatus").mockResolvedValue({ data: { ...funded, status: "BLOCKED" } });
    const user = userEvent.setup();
    renderAt("/admin/clients/3", routes, agentUser);

    await user.click(await screen.findByRole("button", { name: "Block" }));
    expect(change).not.toHaveBeenCalled();
    expect(screen.getByRole("group", { name: "Block this account?" })).toBeInTheDocument();

    await user.click(within(screen.getByRole("group")).getByRole("button", { name: "Block" }));
    expect(change).toHaveBeenCalledWith(10, "BLOCKED");
    expect(await screen.findByText(`Account ${funded.rib} is now blocked.`)).toBeInTheDocument();
    expect(getClient).toHaveBeenCalledTimes(2);
  });

  it("only allows closing an account with a zero balance", async () => {
    vi.spyOn(agentService, "getClient").mockResolvedValue(details([funded, empty]));
    renderAt("/admin/clients/3", routes, agentUser);

    await screen.findByText(funded.rib);
    expect(within(rowOf(funded.rib)).getByRole("button", { name: "Close" })).toBeDisabled();
    expect(within(rowOf(empty.rib)).getByRole("button", { name: "Close" })).toBeEnabled();
    expect(within(rowOf(empty.rib)).getByRole("button", { name: "Unblock" })).toBeInTheDocument();
  });

  it("opens an account, sending the deposit as an exact decimal string", async () => {
    vi.spyOn(agentService, "getClient").mockResolvedValue(details([]));
    const open = vi.spyOn(agentService, "openAccount").mockResolvedValue({
      data: { id: 12, rib: "EB0000", currency: "EUR", balance: 99.9 },
    });
    const user = userEvent.setup();
    renderAt("/admin/clients/3", routes, agentUser);

    await user.selectOptions(await screen.findByLabelText("Currency"), "EUR");
    await user.type(screen.getByLabelText(/Initial deposit/), "99.90");
    await user.click(screen.getByRole("button", { name: "Open account" }));

    expect(open).toHaveBeenCalledWith({ clientId: 3, currency: "EUR", initialDeposit: "99.90" });
    expect(await screen.findByText("Account EB0000 opened with 99.90 EUR.")).toBeInTheDocument();
  });

  it("shows a not-found client", async () => {
    vi.spyOn(agentService, "getClient").mockRejectedValue({
      response: { status: 404, data: { code: "CLIENT_NOT_FOUND", detail: "Client not found" } },
    });
    renderAt("/admin/clients/999", routes, agentUser);
    expect(await screen.findByRole("alert")).toHaveTextContent("Client not found");
  });
});
