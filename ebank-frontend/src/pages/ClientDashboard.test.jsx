import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import { Route } from "react-router-dom";
import ClientDashboard from "./ClientDashboard";
import * as clientService from "../services/clientService";
import { clientUser, renderAt } from "../test/renderWithProviders";

const routes = <Route path="/client" element={<ClientDashboard />} />;

describe("ClientDashboard", () => {
  it("shows the accounts returned by the backend with totals per currency", async () => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({
      data: [
        { id: 1, rib: "EB3094712094552729193874", currency: "MAD", balance: 1500.5, status: "ACTIVE" },
        { id: 2, rib: "EB1111111111111111111111", currency: "EUR", balance: 20, status: "BLOCKED" },
      ],
    });
    renderAt("/client", routes, clientUser);

    expect(await screen.findByText("EB3094712094552729193874")).toBeInTheDocument();
    expect(screen.getByText("Blocked")).toBeInTheDocument();
    expect(screen.getByText("1,500.50 MAD")).toBeInTheDocument();
    expect(screen.getByText("20.00 EUR")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "History of account EB3094712094552729193874" }))
      .toHaveAttribute("href", "/client/accounts/1");
  });

  it("explains what to do when there is no account", async () => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({ data: [] });
    renderAt("/client", routes, clientUser);
    expect(await screen.findByText(/no account yet/)).toBeInTheDocument();
  });
});
