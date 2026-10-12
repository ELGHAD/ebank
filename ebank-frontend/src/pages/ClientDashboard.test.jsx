import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import ClientDashboard from "./ClientDashboard";
import * as clientService from "../services/clientService";
import api from "../api/axiosClient";
import { clientUser, renderAt } from "../test/renderWithProviders";

const routes = <Route path="/client" element={<ClientDashboard />} />;

describe("ClientDashboard", () => {
  it("shows the accounts returned by the backend with their own currency", async () => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({
      data: [
        { id: 1, rib: "EB3094712094552729193874", currency: "MAD", balance: 1500.5, status: "ACTIVE" },
        { id: 2, rib: "EB1111111111111111111111", currency: "EUR", balance: 20, status: "BLOCKED" },
      ],
    });
    renderAt("/client", routes, clientUser);

    expect(await screen.findByText("EB3094712094552729193874")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE", { exact: false })).toBeInTheDocument();
    expect(screen.getByText("1,500.50 MAD")).toBeInTheDocument();
    expect(screen.getByText("20.00 EUR")).toBeInTheDocument();
  });

  // Regression for audit F-2, now server-side: logout must revoke the session, not just forget it locally
  it("logout revokes the session on the server and returns to the login page", async () => {
    vi.spyOn(clientService, "getMyAccounts").mockResolvedValue({ data: [] });
    const post = vi.spyOn(api, "post").mockResolvedValue({ status: 204 });
    renderAt("/client", routes, clientUser);

    await userEvent.setup().click(await screen.findByRole("button", { name: /logout/i }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(post).toHaveBeenCalledWith("/api/auth/logout");
  });
});
