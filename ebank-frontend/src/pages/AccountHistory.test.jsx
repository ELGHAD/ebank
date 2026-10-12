import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import AccountHistory from "./AccountHistory";
import * as clientService from "../services/clientService";
import { clientSession, renderAt } from "../test/renderWithProviders";

const routes = <Route path="/client/accounts/:accountId" element={<AccountHistory />} />;

const page = (content, pageNumber, totalPages, totalElements) => ({
  data: { content, page: pageNumber, size: 10, totalElements, totalPages },
});

const debit = {
  id: 3, type: "DEBIT", amount: 30, balanceAfter: 70, description: "Rent",
  counterpartyRib: "EB3094712094552729193874", transferId: 9, createdAt: "2026-10-12T10:00:00Z",
};
const deposit = {
  id: 1, type: "DEPOSIT", amount: 100, balanceAfter: 100, description: "Opening deposit",
  counterpartyRib: null, transferId: null, createdAt: "2026-10-11T10:00:00Z",
};

describe("AccountHistory", () => {
  beforeEach(() => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({
      data: [{ id: 5, rib: "EB3777744714656519320252", currency: "MAD", balance: 70, status: "ACTIVE" }],
    });
  });

  it("shows the movements returned by the backend", async () => {
    vi.spyOn(clientService, "getAccountEntries").mockResolvedValue(page([debit, deposit], 0, 1, 2));
    renderAt("/client/accounts/5", routes, clientSession);

    const rows = await screen.findAllByRole("row");
    expect(rows).toHaveLength(3); // header + 2
    expect(within(rows[1]).getByText("Transfer out")).toBeInTheDocument();
    expect(within(rows[1]).getByText("−30.00 MAD")).toBeInTheDocument();
    expect(within(rows[2]).getByText("Deposit")).toBeInTheDocument();
    expect(clientService.getAccountEntries).toHaveBeenCalledWith("5", { page: 0, size: 10, type: undefined });
  });

  it("paginates and filters through the API", async () => {
    const getEntries = vi.spyOn(clientService, "getAccountEntries").mockResolvedValue(page([debit], 0, 2, 11));
    const user = userEvent.setup();
    renderAt("/client/accounts/5", routes, clientSession);

    await user.click(await screen.findByRole("button", { name: "Next" }));
    expect(getEntries).toHaveBeenLastCalledWith("5", { page: 1, size: 10, type: undefined });

    await user.selectOptions(screen.getByLabelText("Show"), "CREDIT");
    expect(getEntries).toHaveBeenLastCalledWith("5", { page: 0, size: 10, type: "CREDIT" });
  });

  it("shows an empty state", async () => {
    vi.spyOn(clientService, "getAccountEntries").mockResolvedValue(page([], 0, 0, 0));
    renderAt("/client/accounts/5", routes, clientSession);
    expect(await screen.findByText("No movements to show.")).toBeInTheDocument();
  });

  it("shows the server error, e.g. for someone else's account", async () => {
    vi.spyOn(clientService, "getAccountEntries").mockRejectedValue({
      response: { status: 404, data: { code: "ACCOUNT_NOT_FOUND", detail: "Account not found" } },
    });
    renderAt("/client/accounts/99", routes, clientSession);
    expect(await screen.findByRole("alert")).toHaveTextContent("Account not found");
  });
});
