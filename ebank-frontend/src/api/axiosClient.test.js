import { afterEach, describe, expect, it, vi } from "vitest";
import api, { setUnauthorizedHandler } from "./axiosClient";
import { clearSession, writeSession } from "../context/sessionStorage";

// A fake transport so requests never leave the test
const respondWith = (status) => (config) =>
  status < 400
    ? Promise.resolve({ data: {}, status, statusText: "OK", headers: {}, config })
    : Promise.reject(
        Object.assign(new Error(`HTTP ${status}`), { config, response: { status, data: {} } }),
      );

describe("axiosClient", () => {
  afterEach(() => {
    clearSession();
    setUnauthorizedHandler(() => {});
  });

  it("uses same-origin relative URLs and attaches the stored token", async () => {
    writeSession({ token: "abc", user: { username: "u", roles: [] } });
    let seen;
    await api.get("/api/client/accounts", {
      adapter: (config) => {
        seen = config;
        return respondWith(200)(config);
      },
    });
    expect(seen.baseURL).toBe("");
    expect(seen.url).toBe("/api/client/accounts");
    expect(seen.headers.Authorization).toBe("Bearer abc");
  });

  // Regression for audit F-3: an expired token left the UI "logged in"
  it("notifies the auth layer when an authenticated request gets 401", async () => {
    writeSession({ token: "expired", user: { username: "u", roles: [] } });
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(api.get("/api/client/accounts", { adapter: respondWith(401) })).rejects.toThrow();
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("does not treat a failed login (no token sent) as a session expiry", async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(api.post("/api/auth/login", {}, { adapter: respondWith(401) })).rejects.toThrow();
    expect(handler).not.toHaveBeenCalled();
  });
});
