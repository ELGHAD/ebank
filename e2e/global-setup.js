import { request } from "@playwright/test";
import { AGENT } from "./credentials.js";

/**
 * Waits for the stack and warms the backend up. Observed: the first login after a cold JVM start can take
 * well over 10 seconds, which would otherwise make the first test fail for reasons unrelated to the product.
 */
export default async function globalSetup(config) {
  const baseURL = config.projects[0].use.baseURL;
  const deadline = Date.now() + 5 * 60_000;
  const api = await request.newContext({ baseURL });
  try {
    for (;;) {
      const res = await api.get("/api/auth/csrf").catch(() => null);
      if (res?.status() === 204) break;
      if (Date.now() > deadline) throw new Error(`Stack not reachable at ${baseURL}/api/auth/csrf`);
      await new Promise((r) => setTimeout(r, 2000));
    }
    const xsrf = (await api.storageState()).cookies.find((c) => c.name === "XSRF-TOKEN")?.value;
    for (let i = 0; i < 3; i++) {
      const res = await api.post("/api/auth/login", {
        headers: { "X-XSRF-TOKEN": xsrf, Origin: baseURL },
        data: { username: AGENT.username, password: AGENT.password() },
        timeout: 120_000,
      });
      if (res.status() !== 200) throw new Error(`Warm-up login failed with status ${res.status()}`);
    }
  } finally {
    await api.dispose();
  }
}
