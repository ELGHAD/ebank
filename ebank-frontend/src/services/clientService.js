import api from "../api/axiosClient";

export const getMyAccounts = () => api.get("/api/client/accounts");

export const getAccountDashboard = (accountId, page = 0, size = 10) =>
  api.get(
    `/api/client/accounts/${accountId}/dashboard?page=${page}&size=${size}`
  );

export const createTransfer = (payload) =>
  api.post("/api/client/transfers", payload);
