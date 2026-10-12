import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "../context/AuthContext";
import { writeSession } from "../context/sessionStorage";

export const agentSession = {
  token: "agent-token",
  user: { id: 1, username: "agent@demo.ebank.test", roles: ["AGENT"], organizationId: 1 },
};

export const clientSession = {
  token: "client-token",
  user: { id: 2, username: "client@demo.ebank.test", roles: ["CLIENT"], organizationId: 1 },
};

/** Renders `routes` inside the real AuthProvider, as if the browser had loaded `path` with `session` stored. */
export const renderAt = (path, routes, session = null) => {
  if (session) writeSession(session);
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          {routes}
          <Route path="/login" element={<p>Login page</p>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
};
