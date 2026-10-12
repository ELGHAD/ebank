import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import AuditLogPage from "./AuditLogPage";
import AccountHistory from "../AccountHistory";
import * as agentService from "../../services/agentService";
import { agentUser, renderAt } from "../../test/renderWithProviders";

const event = {
  id: 1, type: "LOGIN_FAILED", actorUsername: "x@test.ebank", target: null, ipAddress: "172.18.0.1",
  details: "BadCredentialsException", createdAt: "2026-10-12T10:00:00Z",
};

describe("AuditLogPage", () => {
  it("lists events with readable labels and filters by type", async () => {
    const list = vi.spyOn(agentService, "getAuditEvents").mockResolvedValue({
      data: { content: [event], page: 0, size: 20, totalPages: 1, totalElements: 1 },
    });
    const user = userEvent.setup();
    renderAt("/admin/audit", <Route path="/admin/audit" element={<AuditLogPage />} />, agentUser);

    expect(await screen.findByText("Failed login", { selector: "td span" })).toBeInTheDocument();
    expect(screen.getByText("172.18.0.1")).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Event type"), "TRANSFER_COMPLETED");
    expect(list).toHaveBeenLastCalledWith({ page: 0, size: 20, type: "TRANSFER_COMPLETED" });
  });
});

describe("AccountHistory (agent)", () => {
  it("uses the agent endpoints and links back to the client", async () => {
    vi.spyOn(agentService, "getAccount").mockResolvedValue({
      data: { id: 10, rib: "EB3094712094552729193874", currency: "MAD", balance: 70, status: "ACTIVE", clientId: 3 },
    });
    const entries = vi.spyOn(agentService, "getAccountEntries").mockResolvedValue({
      data: { content: [], page: 0, size: 10, totalPages: 0, totalElements: 0 },
    });
    renderAt("/admin/accounts/10", <Route path="/admin/accounts/:accountId" element={<AccountHistory role="agent" />} />, agentUser);

    expect(await screen.findByRole("link", { name: "← Client" })).toHaveAttribute("href", "/admin/clients/3");
    expect(entries).toHaveBeenCalledWith("10", { page: 0, size: 10, type: undefined });
    expect(screen.queryByRole("link", { name: "New transfer" })).not.toBeInTheDocument();
  });
});
