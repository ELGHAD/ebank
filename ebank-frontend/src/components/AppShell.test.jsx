import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import AppShell from "./AppShell";
import api from "../api/axiosClient";
import { agentUser, clientUser, renderAt } from "../test/renderWithProviders";

const shellRoutes = [
  <Route key="a" path="/admin" element={<AppShell />}>
    <Route index element={<p>Agent home</p>} />
  </Route>,
  <Route key="c" path="/client" element={<AppShell />}>
    <Route index element={<p>Client home</p>} />
  </Route>,
];

describe("AppShell", () => {
  it("offers a skip link and landmarks", async () => {
    renderAt("/admin", shellRoutes, agentUser);
    expect(await screen.findByText("Agent home")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Skip to content" })).toHaveAttribute("href", "#main");
    expect(screen.getByRole("main")).toHaveAttribute("id", "main");
    expect(screen.getByRole("navigation", { name: "Main" })).toBeInTheDocument();
    expect(screen.getByRole("contentinfo")).toHaveTextContent("synthetic data only");
  });

  it("shows the agent navigation with the current page marked", async () => {
    renderAt("/admin", shellRoutes, agentUser);
    const nav = await screen.findByRole("navigation", { name: "Main" });
    expect(within(nav).getByRole("link", { name: "Clients" })).toHaveAttribute("aria-current", "page");
    expect(within(nav).getByRole("link", { name: "Audit log" })).toBeInTheDocument();
    expect(within(nav).queryByRole("link", { name: "New transfer" })).not.toBeInTheDocument();
  });

  it("shows the client navigation only to clients", async () => {
    renderAt("/client", shellRoutes, clientUser);
    const nav = await screen.findByRole("navigation", { name: "Main" });
    expect(within(nav).getByRole("link", { name: "New transfer" })).toBeInTheDocument();
    expect(within(nav).queryByRole("link", { name: "Audit log" })).not.toBeInTheDocument();
  });

  // Regression for audit F-2, now server-side: logout must revoke the session, not just forget it locally
  it("logout revokes the session on the server and returns to the login page", async () => {
    const post = vi.spyOn(api, "post").mockResolvedValue({ status: 204 });
    renderAt("/client", shellRoutes, clientUser);

    await userEvent.setup().click(await screen.findByRole("button", { name: "Logout" }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(post).toHaveBeenCalledWith("/api/auth/logout");
  });
});
