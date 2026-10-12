import { afterEach, describe, expect, it, vi } from "vitest";
import api, { setUnauthorizedHandler } from "./axiosClient";

// A fake transport so requests never leave the test
const respondWith = (status) => (config) =>
  status < 400
    ? Promise.resolve({ data: {}, status, statusText: "OK", headers: {}, config })
    : Promise.reject(Object.assign(new Error(`HTTP ${status}`), { config, response: { status, data: {} } }));

describe("axiosClient", () => {
  afterEach(() => {
    setUnauthorizedHandler(() => {});
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/";
  });

  it("uses same-origin URLs, sends no token itself and echoes the XSRF cookie on writes", async () => {
    // axios adds the CSRF header inside its real XHR adapter, so only the browser's XMLHttpRequest is faked
    const sent = {};
    class FakeXhr {
      open(method, url) {
        Object.assign(sent, { method, url, headers: {} });
      }
      setRequestHeader(name, value) {
        sent.headers[name] = value;
      }
      getAllResponseHeaders() {
        return "content-type: application/json";
      }
      send() {
        Object.assign(this, { readyState: 4, status: 201, statusText: "Created", responseText: "{}", response: "{}" });
        setTimeout(() => (this.onloadend ?? this.onreadystatechange).call(this));
      }
      abort() {}
    }
    vi.stubGlobal("XMLHttpRequest", FakeXhr);
    document.cookie = "XSRF-TOKEN=csrf-123; path=/";

    await api.post("/api/client/transfers", {}, { adapter: "xhr" });

    expect(sent.url).toBe("/api/client/transfers");
    expect(sent.headers.Authorization).toBeUndefined();
    expect(sent.headers["X-XSRF-TOKEN"]).toBe("csrf-123");
    vi.unstubAllGlobals();
  });

  // Regression for audit F-3: an expired session left the UI "logged in"
  it("notifies the auth layer when a request gets 401", async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(api.get("/api/client/accounts", { adapter: respondWith(401) })).rejects.toThrow();
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it("does not treat a failed login or the startup session probe as an expiry", async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);

    await expect(api.post("/api/auth/login", {}, { adapter: respondWith(401) })).rejects.toThrow();
    await expect(api.get("/api/auth/me", { adapter: respondWith(401) })).rejects.toThrow();
    expect(handler).not.toHaveBeenCalled();
  });
});
