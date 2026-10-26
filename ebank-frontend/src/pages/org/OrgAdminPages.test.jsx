import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import OverviewPage from "./OverviewPage";
import StaffPage from "./StaffPage";
import * as adminServices from "../../services/adminServices";
import { renderAt } from "../../test/renderWithProviders";

const orgAdmin = { id: 5, username: "admin@demo.ebank.test", roles: ["ORG_ADMIN"], organizationId: 1, passwordChangeRequired: false };
const staff = [
  { id: 5, username: "admin@demo.ebank.test", roles: ["ORG_ADMIN"], enabled: true, passwordChangeRequired: false, createdAt: "2026-10-01T10:00:00Z" },
  { id: 6, username: "agent@demo.ebank.test", roles: ["AGENT"], enabled: true, passwordChangeRequired: false, createdAt: "2026-10-01T10:00:00Z" },
  { id: 7, username: "new@demo.ebank.test", roles: ["AGENT"], enabled: false, passwordChangeRequired: true, createdAt: "2026-10-02T10:00:00Z" },
];

describe("OverviewPage", () => {
  it("shows the organization's figures per currency", async () => {
    vi.spyOn(adminServices, "getOverview").mockResolvedValue({
      data: {
        organizationName: "Demo Bank", clients: 12, accounts: 20, activeAccounts: 18, blockedAccounts: 1, closedAccounts: 1,
        balances: [{ currency: "EUR", amount: 20 }, { currency: "MAD", amount: 1500.5 }],
        transfersLast30Days: 7, transferVolumeLast30Days: [{ currency: "MAD", amount: 300 }], staff: 3,
      },
    });
    renderAt("/org", <Route path="/org" element={<OverviewPage />} />, orgAdmin);

    expect(await screen.findByRole("heading", { level: 1, name: "Demo Bank" })).toBeInTheDocument();
    expect(screen.getByText("18 / 1 / 1")).toBeInTheDocument();
    expect(screen.getByText("1,500.50 MAD")).toBeInTheDocument();
    expect(screen.getByText("300.00 MAD")).toBeInTheDocument();
  });
});

describe("StaffPage", () => {
  const routes = <Route path="/org/staff" element={<StaffPage />} />;
  const rowOf = (name) => screen.getByText(name).closest("tr");

  it("lists staff with their state and never offers to disable yourself", async () => {
    vi.spyOn(adminServices, "listStaff").mockResolvedValue({ data: staff });
    renderAt("/org/staff", routes, orgAdmin);

    expect(await screen.findByText("agent@demo.ebank.test")).toBeInTheDocument();
    expect(within(rowOf("admin@demo.ebank.test")).getByText("You")).toBeInTheDocument();
    expect(within(rowOf("admin@demo.ebank.test")).queryByRole("button")).not.toBeInTheDocument();
    expect(within(rowOf("new@demo.ebank.test")).getByText("Disabled")).toBeInTheDocument();
    expect(within(rowOf("new@demo.ebank.test")).getByRole("button", { name: "Enable" })).toBeInTheDocument();
  });

  it("disables a staff member after confirmation", async () => {
    vi.spyOn(adminServices, "listStaff").mockResolvedValue({ data: staff });
    const toggle = vi.spyOn(adminServices, "setStaffEnabled").mockResolvedValue({ data: { ...staff[1], enabled: false } });
    const user = userEvent.setup();
    renderAt("/org/staff", routes, orgAdmin);

    await user.click(within(await screen.findByText("agent@demo.ebank.test").then((el) => el.closest("tr"))).getByRole("button", { name: "Disable" }));
    await user.click(within(screen.getByRole("group")).getByRole("button", { name: "Disable" }));

    expect(toggle).toHaveBeenCalledWith(6, false);
    expect(await screen.findByText("agent@demo.ebank.test is now disabled.")).toBeInTheDocument();
  });

  it("adds an agent and shows the one-time password", async () => {
    vi.spyOn(adminServices, "listStaff").mockResolvedValue({ data: staff });
    const create = vi.spyOn(adminServices, "createStaff").mockResolvedValue({
      data: { member: { ...staff[2], username: "rita@demo.ebank.test", enabled: true }, temporaryPassword: "Tmp-Rita-1" },
    });
    const user = userEvent.setup();
    renderAt("/org/staff", routes, orgAdmin);

    await user.type(await screen.findByLabelText("Email"), "rita@demo.ebank.test");
    await user.click(screen.getByRole("button", { name: "Add staff member" }));

    expect(create).toHaveBeenCalledWith({ email: "rita@demo.ebank.test", role: "AGENT" });
    expect(await screen.findByText("Tmp-Rita-1")).toBeInTheDocument();
  });
});
