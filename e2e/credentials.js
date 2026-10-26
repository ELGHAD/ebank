// Demo accounts of the stack under test (created by the backend's "demo" profile). Passwords come from the
// environment, never from the repository: locally from .env, in CI from per-run generated secrets.
const required = (name) => {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Set ${name} (the matching DEMO_* password of the stack under test)`);
  }
  return value;
};

export const AGENT = { username: "agent@demo.ebank.test", password: () => required("E2E_AGENT_PASSWORD") };
export const ORG_ADMIN = { username: "admin@demo.ebank.test", password: () => required("E2E_ORG_ADMIN_PASSWORD") };
export const PLATFORM_ADMIN = { username: "platform@ebank.test", password: () => required("E2E_PLATFORM_PASSWORD") };
