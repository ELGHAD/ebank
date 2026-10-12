import React from "react";
import { describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import LoginPage from "./LoginPage";
import api from "../api/axiosClient";
import { agentUser, clientUser, renderAt } from "../test/renderWithProviders";

const routes = [
  <Route key="l" path="/" element={<LoginPage />} />,
  <Route key="a" path="/admin" element={<p>Admin area</p>} />,
  <Route key="c" path="/client" element={<p>Client area</p>} />,
];

const fillAndSubmit = async (email, password) => {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Login" }));
};

describe("LoginPage", () => {
  it("sends an agent to the admin area; the token stays in the HttpOnly cookie", async () => {
    vi.spyOn(api, "post").mockResolvedValue({ data: { expiresIn: 3600, user: agentUser } });
    renderAt("/", routes);

    await fillAndSubmit("agent@demo.ebank.test", "secret-password");

    expect(await screen.findByText("Admin area")).toBeInTheDocument();
    expect(api.post).toHaveBeenCalledWith("/api/auth/login", {
      username: "agent@demo.ebank.test",
      password: "secret-password",
    });
    expect(localStorage.length).toBe(0);
  });

  it("sends a client with a temporary password to the password change page", async () => {
    vi.spyOn(api, "post").mockResolvedValue({
      data: { expiresIn: 3600, user: { ...clientUser, passwordChangeRequired: true } },
    });
    renderAt("/", routes);

    await fillAndSubmit("client@demo.ebank.test", "temporary-pass");

    expect(await screen.findByText("Change password page")).toBeInTheDocument();
  });

  it("shows the server's problem detail on failure", async () => {
    vi.spyOn(api, "post").mockRejectedValue({
      response: { status: 401, data: { detail: "Invalid username or password", code: "INVALID_CREDENTIALS" } },
    });
    renderAt("/", routes);

    await fillAndSubmit("agent@demo.ebank.test", "wrong");

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password");
  });

  it("shows the throttling message", async () => {
    vi.spyOn(api, "post").mockRejectedValue({
      response: { status: 429, data: { detail: "Too many failed attempts. Try again later.", code: "TOO_MANY_ATTEMPTS" } },
    });
    renderAt("/", routes);

    await fillAndSubmit("agent@demo.ebank.test", "wrong");

    expect(await screen.findByRole("alert")).toHaveTextContent("Too many failed attempts");
  });
});
