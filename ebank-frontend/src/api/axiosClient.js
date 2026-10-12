import axios from "axios";
import { readSession } from "../context/sessionStorage";

// Empty base URL = same origin: Nginx (Docker) or the Vite dev proxy forwards /api to the backend.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  headers: {
    "Content-Type": "application/json",
  },
});

let onUnauthorized = () => {};

/** Registered by AuthProvider: called when the backend rejects our token (expired, revoked, user disabled). */
export const setUnauthorizedHandler = (handler) => {
  onUnauthorized = handler;
};

// Automatically attach JWT token if present
api.interceptors.request.use((config) => {
  const token = readSession()?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const sentToken = Boolean(error.config?.headers?.Authorization);
    if (error.response?.status === 401 && sentToken) {
      onUnauthorized();
    }
    return Promise.reject(error);
  },
);

export default api;
