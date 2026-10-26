import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import BillingPage from "./BillingPage";
import * as adminServices from "../../services/adminServices";
import { renderAt } from "../../test/renderWithProviders";

const orgAdmin = { id: 5, username: "admin@demo.ebank.test", roles: ["ORG_ADMIN"], organizationId: 1, passwordChangeRequired: false };
const routes = <Route path="/org/billing" element={<BillingPage />} />;

const plans = [
  { code: "FREE", name: "Free", monthlyPrice: 0, currency: "EUR", maxStaff: 3, maxClients: 25, maxAccounts: 50, maxTransfersPerMonth: 200 },
  { code: "PRO", name: "Pro", monthlyPrice: 49, currency: "EUR", maxStaff: 25, maxClients: 1000, maxAccounts: 2500, maxTransfersPerMonth: 10000 },
  { code: "ENTERPRISE", name: "Enterprise", monthlyPrice: 199, currency: "EUR", maxStaff: null, maxClients: null, maxAccounts: null, maxTransfersPerMonth: null },
];
const subscription = (overrides = {}) => ({
  data: {
    plan: plans[0],
    status: "ACTIVE",
    currentPeriodStart: "2026-10-01T00:00:00Z",
    currentPeriodEnd: "2026-11-01T00:00:00Z",
    usage: [
      { quota: "STAFF", label: "staff members", used: 2, limit: 3 },
      { quota: "CLIENTS", label: "clients", used: 25, limit: 25 },
      { quota: "ACCOUNTS", label: "open accounts", used: 12, limit: 50 },
      { quota: "TRANSFERS_PER_MONTH", label: "transfers this month", used: 170, limit: 200 },
    ],
    invoices: [],
    ...overrides,
  },
});

describe("BillingPage", () => {
  it("shows usage against the plan's limits", async () => {
    vi.spyOn(adminServices, "getSubscription").mockResolvedValue(subscription());
    vi.spyOn(adminServices, "listPlans").mockResolvedValue({ data: plans });
    renderAt("/org/billing", routes, orgAdmin);

    expect(await screen.findByRole("meter", { name: "clients" })).toHaveAttribute("aria-valuetext", "25 of 25, limit reached");
    expect(screen.getByRole("meter", { name: "transfers this month" })).toHaveAttribute("aria-valuetext", "170 of 200, near limit");
    expect(screen.getByText(/no payment is taken/)).toBeInTheDocument();
    expect(screen.getByText("Current plan")).toBeInTheDocument();
    expect(screen.getAllByText("Unlimited", { exact: false }).length).toBeGreaterThan(0);
  });

  it("switches plan after confirmation and mentions the simulated invoice", async () => {
    vi.spyOn(adminServices, "getSubscription").mockResolvedValue(subscription());
    vi.spyOn(adminServices, "listPlans").mockResolvedValue({ data: plans });
    const change = vi.spyOn(adminServices, "changePlan").mockResolvedValue(subscription({ plan: plans[1] }));
    const user = userEvent.setup();
    renderAt("/org/billing", routes, orgAdmin);

    await user.click(await screen.findByRole("button", { name: "Switch to Pro" }));
    expect(change).not.toHaveBeenCalled();
    await user.click(within(screen.getByRole("group")).getByRole("button", { name: "Switch" }));

    expect(change).toHaveBeenCalledWith("PRO");
    expect(await screen.findByText(/simulated invoice of 49.00 EUR/)).toBeInTheDocument();
  });

  it("explains a refused downgrade", async () => {
    vi.spyOn(adminServices, "getSubscription").mockResolvedValue(subscription({ plan: plans[1] }));
    vi.spyOn(adminServices, "listPlans").mockResolvedValue({ data: plans });
    vi.spyOn(adminServices, "changePlan").mockRejectedValue({
      response: { status: 422, data: { code: "PLAN_LIMITS_EXCEEDED", detail: "Current usage is above the Free plan: 4 staff members (limit 3)" } },
    });
    const user = userEvent.setup();
    renderAt("/org/billing", routes, orgAdmin);

    await user.click(await screen.findByRole("button", { name: "Switch to Free" }));
    await user.click(within(screen.getByRole("group")).getByRole("button", { name: "Switch" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("4 staff members (limit 3)");
  });

  it("warns when the (simulated) payment is overdue", async () => {
    vi.spyOn(adminServices, "getSubscription").mockResolvedValue(subscription({ status: "PAST_DUE" }));
    vi.spyOn(adminServices, "listPlans").mockResolvedValue({ data: plans });
    renderAt("/org/billing", routes, orgAdmin);

    expect(await screen.findByText(/Payment overdue \(simulated\)/)).toBeInTheDocument();
    expect(screen.getByText(/Customers keep full access to their money/)).toBeInTheDocument();
  });
});
