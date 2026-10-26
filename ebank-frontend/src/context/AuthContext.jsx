import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import api, { setUnauthorizedHandler } from "../api/axiosClient";

const AuthContext = createContext(null);

// Most privileged role first: it decides the home page and the navigation
const ROLE_ORDER = ["PLATFORM_ADMIN", "ORG_ADMIN", "AGENT", "CLIENT"];
const primaryRole = (roles = []) => ROLE_ORDER.find((r) => roles.includes(r)) ?? null;

/**
 * status: "loading" while the session is being restored, then "authenticated" or "anonymous".
 * Nothing is stored in the browser: the session lives in an HttpOnly cookie and is restored with /api/auth/me.
 */
const toAuthState = (user, status = user ? "authenticated" : "anonymous") => ({
  status,
  isAuthenticated: Boolean(user),
  user,
  username: user?.username ?? null,
  roles: user?.roles ?? [],
  role: primaryRole(user?.roles),
  passwordChangeRequired: Boolean(user?.passwordChangeRequired),
});

export const AuthProvider = ({ children }) => {
  const [auth, setAuth] = useState(() => toAuthState(null, "loading"));

  // Startup: obtain the CSRF cookie, then ask the backend whether the session cookie is still valid
  useEffect(() => {
    let cancelled = false;
    api
      .get("/api/auth/csrf")
      .catch(() => {})
      .then(() => api.get("/api/auth/me"))
      .then((res) => !cancelled && setAuth(toAuthState(res.data)))
      .catch(() => !cancelled && setAuth(toAuthState(null)));
    return () => {
      cancelled = true;
    };
  }, []);

  /** After a successful login or password change: the backend has set the cookie, we keep the user. */
  const login = useCallback((user) => setAuth(toAuthState(user)), []);

  const logout = useCallback(async () => {
    try {
      await api.post("/api/auth/logout"); // revokes the session server-side and clears the cookie
    } catch {
      // The local state is cleared anyway
    }
    setAuth(toAuthState(null));
  }, []);

  // Expired or revoked session: forget the user; ProtectedRoute then redirects to /login
  useEffect(() => {
    setUnauthorizedHandler(() => setAuth(toAuthState(null)));
    return () => setUnauthorizedHandler(() => {});
  }, []);

  const value = useMemo(() => ({ auth, login, logout }), [auth, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => useContext(AuthContext);
