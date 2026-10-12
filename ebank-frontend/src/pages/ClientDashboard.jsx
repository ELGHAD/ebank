import { useMemo } from "react";
import { Link } from "react-router-dom";
import { getMyAccounts } from "../services/clientService";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import useQuery from "../hooks/useQuery";
import { formatMoney } from "../utils/format";

export default function ClientDashboard() {
  const { data, error, loading } = useQuery(getMyAccounts, "accounts", "Cannot load your accounts.");
  const accounts = useMemo(() => data ?? [], [data]);

  // Display-only aggregation per currency; amounts are never computed client-side for business purposes
  const totalsByCurrency = useMemo(() => {
    const totals = {};
    for (const a of accounts) {
      totals[a.currency] = (totals[a.currency] || 0) + Number(a.balance);
    }
    return Object.entries(totals);
  }, [accounts]);

  return (
    <>
      <PageHeader
        title="My accounts"
        actions={
          <Link to="/client/transfer" className="btn btnPrimary">
            New transfer
          </Link>
        }
      />

      <section className="card" aria-label="Summary">
        <div className="kpis">
          <div className="kpi">
            <div className="kpiLabel">Accounts</div>
            <div className="kpiValue">{loading ? "…" : accounts.length}</div>
          </div>
          <div className="kpi">
            <div className="kpiLabel">Total balance</div>
            <div className="kpiValue money">
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
      </section>

      <section className="card" aria-labelledby="accounts-title">
        <h2 id="accounts-title">Accounts</h2>
        {error && (
          <div className="alert alertError" role="alert">
            {error}
          </div>
        )}
        {loading ? (
          <p className="empty" role="status">
            Loading accounts…
          </p>
        ) : accounts.length === 0 ? (
          <p className="empty">You have no account yet. Your bank agent can open one for you.</p>
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
                      <StatusBadge status={a.status} />
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
      </section>
    </>
  );
}
