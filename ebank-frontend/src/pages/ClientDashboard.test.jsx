import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import ClientDashboard from "./ClientDashboard";
import * as clientService from "../services/clientService";
import { readSession } from "../context/sessionStorage";
import { clientSession, renderAt } from "../test/renderWithProviders";

const routes = <Route path="/client" element={<ClientDashboard />} />;

describe("ClientDashboard", () => {
  afterEach(() => localStorage.clear());

  it("shows the accounts returned by the backend with their own currency", async () => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({
      data: [
        { id: 1, rib: "EB3094712094552729193874", currency: "MAD", balance: 1500.5, status: "ACTIVE" },
        { id: 2, rib: "EB1111111111111111111111", currency: "EUR", balance: 20, status: "BLOCKED" },
      ],
    });
    renderAt("/client", routes, clientSession);

    expect(await screen.findByText("EB3094712094552729193874")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE", { exact: false })).toBeInTheDocument();
    expect(screen.getAllByText("EUR", { exact: false }).length).toBeGreaterThan(0);
  });

  // Regression for audit F-2: logout removed the wrong storage key and kept the session
  it("logout clears the stored session and returns to the login page", async () => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({ data: [] });
    renderAt("/client", routes, clientSession);

    await userEvent.setup().click(await screen.findByRole("button", { name: /logout/i }));

    expect(screen.getByText("Login page")).toBeInTheDocument();
    expect(readSession()).toBeNull();
  });
});
