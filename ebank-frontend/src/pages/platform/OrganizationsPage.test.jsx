import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import OrganizationsPage from "./OrganizationsPage";
import * as adminServices from "../../services/adminServices";
import { renderAt } from "../../test/renderWithProviders";

const platformUser = { id: 9, username: "platform@ebank.test", roles: ["PLATFORM_ADMIN"], organizationId: null, passwordChangeRequired: false };
const routes = <Route path="/platform" element={<OrganizationsPage />} />;
const page = (content) => ({ data: { content, page: 0, size: 20, totalPages: 1, totalElements: content.length } });
const atlas = { id: 4, name: "Atlas Bank", status: "ACTIVE", users: 3, clients: 12, accounts: 20, createdAt: "2026-10-01T10:00:00Z" };

describe("OrganizationsPage", () => {
  it("lists organizations with counts only", async () => {
    vi.spyOn(adminServices, "listOrganizations").mockResolvedValue(page([atlas]));
    renderAt("/platform", routes, platformUser);

    const row = (await screen.findByText("Atlas Bank")).closest("tr");
    expect(within(row).getByText("Active")).toBeInTheDocument();
    expect(within(row).getByText("12")).toBeInTheDocument();
  });

  it("creates an organization and shows its administrator's one-time password", async () => {
    vi.spyOn(adminServices, "listOrganizations").mockResolvedValue(page([]));
    const create = vi.spyOn(adminServices, "createOrganization").mockResolvedValue({
      data: { organization: { ...atlas, name: "Nova Bank" }, adminUsername: "boss@nova.test", temporaryPassword: "Tmp-123-abc" },
    });
    const user = userEvent.setup();
    renderAt("/platform", routes, platformUser);

    await user.type(await screen.findByLabelText("Organization name"), " Nova Bank ");
    await user.type(screen.getByLabelText("First administrator's email"), "boss@nova.test");
    await user.click(screen.getByRole("button", { name: "Create organization" }));

    expect(create).toHaveBeenCalledWith({ name: "Nova Bank", adminEmail: "boss@nova.test" });
    expect(await screen.findByText("Tmp-123-abc")).toBeInTheDocument();
    expect(screen.getByText(/shown only once/)).toBeInTheDocument();
  });

  it("suspends an organization after confirmation", async () => {
    vi.spyOn(adminServices, "listOrganizations").mockResolvedValue(page([atlas]));
    const change = vi.spyOn(adminServices, "changeOrganizationStatus").mockResolvedValue({ data: { ...atlas, status: "SUSPENDED" } });
    const user = userEvent.setup();
    renderAt("/platform", routes, platformUser);

    await user.click(await screen.findByRole("button", { name: "Suspend" }));
    expect(change).not.toHaveBeenCalled();
    await user.click(within(screen.getByRole("group")).getByRole("button", { name: "Suspend" }));

    expect(change).toHaveBeenCalledWith(4, "SUSPENDED");
    expect(await screen.findByText(/Atlas Bank is suspended/)).toBeInTheDocument();
  });

  it("shows conflicts from the server", async () => {
    vi.spyOn(adminServices, "listOrganizations").mockResolvedValue(page([]));
    vi.spyOn(adminServices, "createOrganization").mockRejectedValue({
      response: { status: 409, data: { code: "ORGANIZATION_NAME_TAKEN", detail: "An organization with this name already exists" } },
    });
    const user = userEvent.setup();
    renderAt("/platform", routes, platformUser);

    await user.type(await screen.findByLabelText("Organization name"), "Atlas Bank");
    await user.type(screen.getByLabelText("First administrator's email"), "x@atlas.test");
    await user.click(screen.getByRole("button", { name: "Create organization" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("already exists");
  });
});
