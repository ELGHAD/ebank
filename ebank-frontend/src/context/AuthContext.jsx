import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { setUnauthorizedHandler } from "../api/axiosClient";
import { clearSession, readSession, writeSession } from "./sessionStorage";

const AuthContext = createContext(null);

const primaryRole = (roles = []) => (roles.includes("AGENT") ? "AGENT" : roles[0] || null);

const toAuthState = (session) =>
  session?.token && session?.user
    ? {
        isAuthenticated: true,
        token: session.token,
        username: session.user.username,
        roles: session.user.roles,
        role: primaryRole(session.user.roles),
      }
    : { isAuthenticated: false, token: null, username: null, roles: [], role: null };

export const AuthProvider = ({ children }) => {
  // Read synchronously on first render so a page refresh does not bounce protected routes to /login
  const [auth, setAuth] = useState(() => toAuthState(readSession()));

  const login = useCallback(({ token, user }) => {
    const session = { token, user };
    writeSession(session);
    setAuth(toAuthState(session));
  }, []);

  const logout = useCallback(() => {
    clearSession();
    setAuth(toAuthState(null));
  }, []);

  // Expired or revoked token: drop the session; ProtectedRoute then redirects to /login
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(() => {});
  }, [logout]);

  const value = useMemo(() => ({ auth, login, logout }), [auth, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);
