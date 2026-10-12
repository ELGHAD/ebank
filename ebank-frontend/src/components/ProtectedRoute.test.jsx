import React from "react";
import { describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import { agentUser, clientUser, renderAt } from "../test/renderWithProviders";

const adminRoute = (
  <Route
    path="/admin"
    element={
      <ProtectedRoute requiredRole="AGENT">
        <p>Admin area</p>
      </ProtectedRoute>
    }
  />
);

describe("ProtectedRoute", () => {
  it("redirects anonymous visitors to the login page", async () => {
    renderAt("/admin", adminRoute);
    expect(await screen.findByText("Login page")).toBeInTheDocument();
  });

  // Regression for audit F-1: a page refresh must restore the session (now from the cookie, via /api/auth/me)
  it("waits for the session to be restored instead of bouncing to login", async () => {
    renderAt("/admin", adminRoute, agentUser);
    expect(screen.getByRole("status")).toHaveTextContent("Loading");
    expect(await screen.findByText("Admin area")).toBeInTheDocument();
  });

  it("shows a not-authorized message for the wrong role", async () => {
    renderAt("/admin", adminRoute, clientUser);
    expect(await screen.findByRole("alert")).toHaveTextContent("not authorized");
    expect(screen.queryByText("Admin area")).not.toBeInTheDocument();
  });

  it("sends a user with a temporary password to the password change page", async () => {
    renderAt("/admin", adminRoute, { ...agentUser, passwordChangeRequired: true });
    expect(await screen.findByText("Change password page")).toBeInTheDocument();
  });
});
