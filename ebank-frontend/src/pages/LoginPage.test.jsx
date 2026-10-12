import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Route } from "react-router-dom";
import LoginPage from "./LoginPage";
import api from "../api/axiosClient";
import { readSession } from "../context/sessionStorage";
import { renderAt } from "../test/renderWithProviders";

const routes = [
  <Route key="l" path="/" element={<LoginPage />} />,
  <Route key="a" path="/admin" element={<p>Admin area</p>} />,
  <Route key="c" path="/client" element={<p>Client area</p>} />,
];

const fillAndSubmit = async (email, password) => {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Email"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Login" }));
};

describe("LoginPage", () => {
  afterEach(() => localStorage.clear());

  it("stores the session and sends an agent to the admin area", async () => {
    vi.spyOn(api, "post").mockResolvedValue({
      data: {
        accessToken: "jwt",
        tokenType: "Bearer",
        expiresIn: 3600,
        user: { id: 1, username: "agent@demo.ebank.test", roles: ["AGENT"], organizationId: 1 },
      },
    });
    renderAt("/", routes);

    await fillAndSubmit("agent@demo.ebank.test", "secret-password");

    expect(await screen.findByText("Admin area")).toBeInTheDocument();
    expect(api.post).toHaveBeenCalledWith("/api/auth/login", {
      username: "agent@demo.ebank.test",
      password: "secret-password",
    });
    expect(readSession().token).toBe("jwt");
  });

  it("shows the server's problem detail on failure", async () => {
    vi.spyOn(api, "post").mockRejectedValue({
      response: { status: 401, data: { detail: "Invalid username or password", code: "INVALID_CREDENTIALS" } },
    });
    renderAt("/", routes);

    await fillAndSubmit("agent@demo.ebank.test", "wrong");

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password");
    expect(readSession()).toBeNull();
  });
});
