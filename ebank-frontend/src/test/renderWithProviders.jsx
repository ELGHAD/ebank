import React from "react";
import { render } from "@testing-library/react";
import { vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "../context/AuthContext";
import api from "../api/axiosClient";

export const agentUser = {
  id: 1, username: "agent@demo.ebank.test", roles: ["AGENT"], organizationId: 1, passwordChangeRequired: false,
};

export const clientUser = {
  id: 2, username: "client@demo.ebank.test", roles: ["CLIENT"], organizationId: 1, passwordChangeRequired: false,
};

/**
 * Renders `routes` inside the real AuthProvider, as if the browser loaded `path` with a session cookie
 * belonging to `user` (or no session when null). Only the session-restore calls are faked here.
 */
export const renderAt = (path, routes, user = null) => {
  const realGet = api.get.bind(api);
  vi.spyOn(api, "get").mockImplementation((url, config) => {
    if (url === "/api/auth/csrf") return Promise.resolve({ status: 204 });
    if (url === "/api/auth/me") {
      return user ? Promise.resolve({ data: user }) : Promise.reject({ response: { status: 401, data: {} } });
    }
    return realGet(url, config);
  });
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          {routes}
          <Route path="/login" element={<p>Login page</p>} />
          <Route path="/change-password" element={<p>Change password page</p>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
};
