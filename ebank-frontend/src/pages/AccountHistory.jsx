import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getAccountEntries, getMyAccounts } from "../services/clientService";
import { errorMessage } from "../api/errors";
import { formatDateTime, formatMoney } from "../utils/format";
import "../styles/client.css";

const PAGE_SIZE = 10;
const TYPE_LABELS = { DEPOSIT: "Deposit", DEBIT: "Transfer out", CREDIT: "Transfer in" };

export default function AccountHistory() {
  const { accountId } = useParams();
  const [account, setAccount] = useState(null);
  const [type, setType] = useState("");
  const [page, setPage] = useState(0);
  // Result of the last completed request, tagged with the query it answers
  const [result, setResult] = useState({ query: null, data: null, error: "" });
  const query = `${accountId}|${page}|${type}`;
  const loading = result.query !== query;
  const { data, error } = result;

  useEffect(() => {
    getMyAccounts()
      .then((res) => setAccount((res.data || []).find((a) => String(a.id) === accountId) || null))
      .catch(() => {});
  }, [accountId]);

  useEffect(() => {
    let cancelled = false;
    getAccountEntries(accountId, { page, size: PAGE_SIZE, type: type || undefined })
      .then((res) => !cancelled && setResult({ query, data: res.data, error: "" }))
      .catch(
        (e) =>
          !cancelled &&
          setResult((previous) => ({
            query,
            data: previous.data,
            error: errorMessage(e, "Cannot load the account history."),
          })),
      );
    return () => {
      cancelled = true;
    };
  }, [accountId, page, type, query]);

  const onTypeChange = (e) => {
    setType(e.target.value);
    setPage(0);
  };

  const currency = account?.currency ?? "";

  return (
    <div className="client-page">
      <div className="container">
        <div className="topbar">
          <div>
            <h1>Account history</h1>
            <div className="subtitle mono">{account ? account.rib : `Account #${accountId}`}</div>
          </div>
          <div className="actions">
            <Link className="btn btnPrimary" to="/client/transfer">
              New transfer
            </Link>
            <Link className="btn" to="/client">
              <span aria-hidden="true">←</span> Back
            </Link>
          </div>
        </div>

        <div className="card">
          {account && (
            <p style={{ marginTop: 0 }}>
              Current balance:{" "}
              <strong>
                {formatMoney(account.balance)} {account.currency}
              </strong>
            </p>
          )}

          <div style={{ maxWidth: 240, marginBottom: 12 }}>
            <label htmlFor="history-type">Show</label>
            <select id="history-type" value={type} onChange={onTypeChange}>
              <option value="">All movements</option>
              <option value="CREDIT">Transfers in</option>
              <option value="DEBIT">Transfers out</option>
              <option value="DEPOSIT">Deposits</option>
            </select>
          </div>

          {error && (
            <div className="alert alertError" role="alert">
              {error}
            </div>
          )}

          {loading && !data ? (
            <p className="empty">Loading history…</p>
          ) : data && data.content.length === 0 ? (
            <p className="empty">No movements to show.</p>
          ) : data ? (
            <>
              <div className="tableWrap" aria-busy={loading}>
                <table>
                  <caption className="sr-only">
                    Movements, newest first
                  </caption>
                  <thead>
                    <tr>
                      <th scope="col">Date</th>
                      <th scope="col">Type</th>
                      <th scope="col">Counterparty</th>
                      <th scope="col">Description</th>
                      <th scope="col">Amount</th>
                      <th scope="col">Balance after</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((e) => (
                      <tr key={e.id}>
                        <td>{formatDateTime(e.createdAt)}</td>
                        <td>{TYPE_LABELS[e.type] ?? e.type}</td>
                        <td className="mono">{e.counterpartyRib ?? "-"}</td>
                        <td>{e.description ?? "-"}</td>
                        <td className={e.type === "DEBIT" ? "amountOut" : "amountIn"}>
                          {e.type === "DEBIT" ? "−" : "+"}
                          {formatMoney(e.amount)} {currency}
                        </td>
                        <td>
                          {formatMoney(e.balanceAfter)} {currency}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <nav className="actions" aria-label="Pagination" style={{ marginTop: 12, alignItems: "center" }}>
                <button type="button" className="btn" disabled={page === 0 || loading} onClick={() => setPage(page - 1)}>
                  Previous
                </button>
                <span style={{ fontSize: 13 }}>
                  Page {data.page + 1} of {Math.max(data.totalPages, 1)} ({data.totalElements} movements)
                </span>
                <button
                  type="button"
                  className="btn"
                  disabled={page + 1 >= data.totalPages || loading}
                  onClick={() => setPage(page + 1)}
                >
                  Next
                </button>
              </nav>
            </>
          ) : null}
        </div>

        <div className="footer">Banking simulation · synthetic data only</div>
      </div>
    </div>
  );
}
