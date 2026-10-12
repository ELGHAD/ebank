import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import ClientsPage from "./ClientsPage";
import * as agentService from "../../services/agentService";
import { agentUser, renderAt } from "../../test/renderWithProviders";

const routes = <Route path="/admin" element={<ClientsPage />} />;

const page = (content, number = 0, totalPages = 1, totalElements = content.length) => ({
  data: { content, page: number, size: 10, totalPages, totalElements },
});
const zoe = { id: 7, firstName: "Zoe", lastName: "Alami", email: "zoe@test.ebank", identityRef: "CIN-1", accountCount: 2, createdAt: "2026-10-01T10:00:00Z" };

describe("ClientsPage", () => {
  it("lists clients with a link to each client", async () => {
    vi.spyOn(agentService, "searchClients").mockResolvedValue(page([zoe]));
    renderAt("/admin", routes, agentUser);

    expect(await screen.findByRole("link", { name: "Alami Zoe" })).toHaveAttribute("href", "/admin/clients/7");
    expect(screen.getByText("zoe@test.ebank")).toBeInTheDocument();
    expect(agentService.searchClients).toHaveBeenCalledWith({ q: undefined, page: 0, size: 10 });
  });

  it("searches on submit and starts again from the first page", async () => {
    const search = vi.spyOn(agentService, "searchClients").mockResolvedValue(page([zoe], 0, 3, 25));
    const user = userEvent.setup();
    renderAt("/admin", routes, agentUser);

    await user.click(await screen.findByRole("button", { name: "Next" }));
    expect(search).toHaveBeenLastCalledWith({ q: undefined, page: 1, size: 10 });

    await user.type(screen.getByLabelText("Search clients"), "  alami ");
    await user.click(screen.getByRole("button", { name: "Search" }));
    expect(search).toHaveBeenLastCalledWith({ q: "alami", page: 0, size: 10 });
  });

  it("says when nothing matches", async () => {
    vi.spyOn(agentService, "searchClients").mockResolvedValueOnce(page([zoe])).mockResolvedValue(page([]));
    const user = userEvent.setup();
    renderAt("/admin", routes, agentUser);

    await user.type(await screen.findByLabelText("Search clients"), "nobody");
    await user.click(screen.getByRole("button", { name: "Search" }));
    expect(await screen.findByText("No client matches “nobody”.")).toBeInTheDocument();
  });
});
