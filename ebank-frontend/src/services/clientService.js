import api from "../api/axiosClient";

export const getMyAccounts = () => api.get("/api/client/accounts");

/**
 * @param idempotencyKey one random key per transfer attempt, reused if that same attempt is retried,
 *                       so a retry after a network error can never execute the transfer twice
 */
export const createTransfer = (payload, idempotencyKey) =>
  api.post("/api/client/transfers", payload, {
    headers: { "Idempotency-Key": idempotencyKey },
  });

/** @param params { page, size, type } */
export const getAccountEntries = (accountId, params) =>
  api.get(`/api/client/accounts/${accountId}/entries`, { params });
