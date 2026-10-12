import api from "../api/axiosClient";

export const searchClients = (params) => api.get("/api/admin/clients", { params });
export const getClient = (clientId) => api.get(`/api/admin/clients/${clientId}`);
export const onboardClient = (payload) => api.post("/api/admin/clients", payload);
export const openAccount = (payload) => api.post("/api/admin/accounts", payload);
export const getAccount = (accountId) => api.get(`/api/admin/accounts/${accountId}`);
export const changeAccountStatus = (accountId, status) =>
  api.patch(`/api/admin/accounts/${accountId}/status`, { status });
export const getAccountEntries = (accountId, params) =>
  api.get(`/api/admin/accounts/${accountId}/entries`, { params });
export const getAuditEvents = (params) => api.get("/api/admin/audit-events", { params });
