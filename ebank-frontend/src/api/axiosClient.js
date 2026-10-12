import axios from "axios";

// Empty base URL = same origin: Nginx (Docker) or the Vite dev proxy forwards /api to the backend.
// Authentication travels in the HttpOnly session cookie, which the browser sends automatically and
// page scripts cannot read. For state-changing requests axios copies the XSRF-TOKEN cookie into the
// X-XSRF-TOKEN header (same-origin requests only), which the backend checks (CSRF protection).
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  headers: {
    "Content-Type": "application/json",
  },
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

let onUnauthorized = () => {};

/** Registered by AuthProvider: called when the backend says the session is gone (expired, revoked). */
export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler;
};

// A 401 on these is an expected answer, not a lost session
const SESSION_PROBES = ["/api/auth/login", "/api/auth/me", "/api/auth/logout"];

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !SESSION_PROBES.includes(error.config?.url)) {
      onUnauthorized();
    }
    return Promise.reject(error);
  },
);

export default api;
