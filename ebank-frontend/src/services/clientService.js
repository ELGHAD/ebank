import api from "../api/axiosClient";

export const getMyAccounts = () => api.get("/api/client/accounts");

// The backend transfer endpoint arrives with the transfer engine (Phase 2)
export const createTransfer = (payload) =>
  api.post("/api/client/transfers", payload);
