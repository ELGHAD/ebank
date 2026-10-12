// Persistence of the login session (token + user summary).
// Phase 3 replaces localStorage with an HttpOnly cookie; keeping access in one module makes that change local.
const KEY = "ebank.session";

export const readSession = () => {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

export const writeSession = (session) => {
  localStorage.setItem(KEY, JSON.stringify(session));
};

export const clearSession = () => {
  localStorage.removeItem(KEY);
};
