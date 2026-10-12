import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getMyAccounts } from "../services/clientService";
import { useAuth } from "../context/AuthContext";
import { errorMessage } from "../api/errors";
import { formatMoney } from "../utils/format";
import "../styles/client.css";

const statusBadgeClass = (status) =>
  status === "ACTIVE" ? "badge badge-ok" : status === "BLOCKED" || status === "CLOSED" ? "badge badge-bad" : "badge";

export default function ClientDashboard() {
  const [accounts, setAccounts] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();
  const { auth, logout } = useAuth();

  useEffect(() => {
    getMyAccounts()
      .then((res) => setAccounts(res.data || []))
      .catch((e) => setError(errorMessage(e, "Cannot load your accounts.")))
      .finally(() => setLoading(false));
  }, []);

  // Display-only aggregation per currency; amounts are never computed client-side for business purposes
  const totalsByCurrency = useMemo(() => {
    const totals = {};
    for (const a of accounts) {
      totals[a.currency] = (totals[a.currency] || 0) + Number(a.balance);
    }
    return Object.entries(totals);
  }, [accounts]);

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="client-page">
      <div className="container">
        <div className="topbar">
          <div>
            <h1>Client Dashboard</h1>
            <div className="subtitle">Signed in as {auth.username}</div>
          </div>
          <div className="actions">
            <Link to="/client/transfer" className="btn btnPrimary">
              <span aria-hidden="true">↗</span> New transfer
            </Link>
            <Link to="/change-password" className="btn">
              Change password
            </Link>
            <button type="button" onClick={handleLogout} className="btn">
              Logout
            </button>
          </div>
        </div>

        <div className="card" style={{ marginBottom: 14 }}>
          <div className="kpis">
            <div className="kpi">
              <div className="kpiLabel">Accounts</div>
              <div className="kpiValue">{loading ? "…" : accounts.length}</div>
            </div>
            <div className="kpi">
              <div className="kpiLabel">Total balance</div>
              <div className="kpiValue">
                {totalsByCurrency.length === 0
                  ? "-"
                  : totalsByCurrency.map(([currency, total]) => (
                      <div key={currency}>
                        {formatMoney(total)} {currency}
                      </div>
                    ))}
              </div>
            </div>
          </div>
          {error && (
            <div className="alert alertError" role="alert" style={{ marginTop: 12 }}>
              {error}
            </div>
          )}
        </div>

        <div className="card">
          <h2 className="cardTitle">My accounts</h2>
          {loading ? (
            <p className="empty">Loading accounts…</p>
          ) : accounts.length === 0 ? (
            <p className="empty">No accounts yet.</p>
          ) : (
            <div className="tableWrap">
              <table>
                <thead>
                  <tr>
                    <th scope="col">RIB</th>
                    <th scope="col">Currency</th>
                    <th scope="col">Balance</th>
                    <th scope="col">Status</th>
                    <th scope="col">
                      <span className="sr-only">Actions</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {accounts.map((a) => (
                    <tr key={a.id}>
                      <td className="mono">{a.rib}</td>
                      <td>{a.currency}</td>
                      <td className="money">{formatMoney(a.balance)}</td>
                      <td>
                        <span className={statusBadgeClass(a.status)}>
                          <span aria-hidden="true">●</span> {a.status}
                        </span>
                      </td>
                      <td>
                        <Link className="link" to={`/client/accounts/${a.id}`} aria-label={`History of account ${a.rib}`}>
                          History
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="footer">
          Banking simulation for demonstration purposes · all accounts and balances are synthetic
        </div>
      </div>
    </div>
  );
}
