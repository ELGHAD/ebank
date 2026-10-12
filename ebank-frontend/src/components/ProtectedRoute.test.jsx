import React from "react";
import { afterEach, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import { Route } from "react-router-dom";
import ProtectedRoute from "./ProtectedRoute";
import { agentSession, clientSession, renderAt } from "../test/renderWithProviders";

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
  afterEach(() => localStorage.clear());

  it("redirects anonymous visitors to the login page", () => {
    renderAt("/admin", adminRoute);
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  // Regression for audit F-1: a page refresh on a protected route bounced to /login
  it("restores a stored session on first render (page refresh)", () => {
    renderAt("/admin", adminRoute, agentSession);
    expect(screen.getByText("Admin area")).toBeInTheDocument();
  });

  it("shows a not-authorized message for the wrong role", () => {
    renderAt("/admin", adminRoute, clientSession);
    expect(screen.getByRole("alert")).toHaveTextContent("not authorized");
    expect(screen.queryByText("Admin area")).not.toBeInTheDocument();
  });
});
