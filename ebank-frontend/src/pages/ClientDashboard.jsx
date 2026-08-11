import { useEffect, useMemo, useState } from "react";
import { getMyAccounts } from "../services/clientService";
import { Link, useNavigate } from "react-router-dom";

export default function ClientDashboard() {
  const [accounts, setAccounts] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        setError("");
        setLoading(true);
        const res = await getMyAccounts();
        setAccounts(res.data || []);
      } catch (e) {
        console.error(e);
        setError("Cannot load accounts (check token / backend).");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const totalBalance = useMemo(() => {
    return (accounts || []).reduce(
      (sum, a) => sum + Number(a?.balance || 0),
      0
    );
  }, [accounts]);

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };

  const formatMoney = (v) => {
    const n = Number(v || 0);
    return new Intl.NumberFormat("fr-MA", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(n);
  };

  const statusBadgeClass = (status) => {
    const s = String(status || "").toLowerCase();
    if (s.includes("active") || s.includes("open") || s.includes("enabled"))
      return "badge badge-ok";
    if (s.includes("blocked") || s.includes("closed") || s.includes("disabled"))
      return "badge badge-bad";
    return "badge badge-neutral";
  };

  return (
    <>
      <style>{`
        * { box-sizing: border-box; }
        body { margin: 0; font-family: 'Segoe UI', system-ui, sans-serif; }

        .page{
          min-height: 100vh;
          padding: 28px 16px;
          background: linear-gradient(135deg, #0f172a, #020617);
          color: #e5e7eb;
          display: flex;
          justify-content: center;
        }

        .container{
          width: min(1100px, 100%);
        }

        .topbar{
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 14px;
          margin-bottom: 18px;
        }

        .titleWrap{
          display: flex;
          align-items: center;
          gap: 14px;
        }

        .logo{
          width: 48px;
          height: 48px;
          border-radius: 14px;
          background: linear-gradient(135deg, #22c55e, #16a34a);
          color: #052e16;
          display: grid;
          place-items: center;
          font-weight: 900;
          font-size: 18px;
          flex: 0 0 auto;
        }

        h1{
          margin: 0;
          font-size: 22px;
          line-height: 1.2;
        }

        .subtitle{
          margin: 3px 0 0;
          font-size: 13px;
          color: #9ca3af;
        }

        .actions{
          display: flex;
          align-items: center;
          gap: 10px;
          flex-wrap: wrap;
          justify-content: flex-end;
        }

        .btn{
          border: 1px solid rgba(255,255,255,0.16);
          background: rgba(255,255,255,0.06);
          color: #e5e7eb;
          padding: 10px 12px;
          border-radius: 12px;
          cursor: pointer;
          text-decoration: none;
          font-weight: 600;
          font-size: 13px;
          transition: transform .12s ease, filter .12s ease, background .12s ease;
          display: inline-flex;
          align-items: center;
          gap: 8px;
        }

        .btn:hover{
          background: rgba(255,255,255,0.10);
          transform: translateY(-1px);
        }

        .btnPrimary{
          border: none;
          background: linear-gradient(135deg, #22c55e, #16a34a);
          color: #052e16;
          box-shadow: 0 16px 30px rgba(34,197,94,0.20);
        }

        .btnPrimary:hover{ filter: brightness(1.05); }

        .btnDanger{
          border: none;
          background: linear-gradient(135deg, #ef4444, #dc2626);
          color: #fff;
          box-shadow: 0 16px 30px rgba(239,68,68,0.18);
        }

        .btnDanger:hover{ filter: brightness(1.05); }

        .grid{
          display: grid;
          grid-template-columns: 1.2fr .8fr;
          gap: 14px;
          margin-bottom: 14px;
        }

        @media (max-width: 900px){
          .grid{ grid-template-columns: 1fr; }
        }

        .card{
          background: rgba(255,255,255,0.06);
          border: 1px solid rgba(255,255,255,0.14);
          backdrop-filter: blur(12px);
          border-radius: 18px;
          box-shadow: 0 20px 60px rgba(0,0,0,0.55);
          padding: 16px;
        }

        .cardTitle{
          margin: 0 0 10px;
          font-size: 14px;
          color: #cbd5e1;
          font-weight: 700;
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .kpis{
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 10px;
        }

        @media (max-width: 600px){
          .kpis{ grid-template-columns: 1fr; }
        }

        .kpi{
          padding: 12px;
          border-radius: 14px;
          border: 1px solid rgba(255,255,255,0.14);
          background: rgba(0,0,0,0.18);
        }

        .kpiLabel{
          font-size: 12px;
          color: #9ca3af;
          margin-bottom: 6px;
        }

        .kpiValue{
          font-size: 20px;
          font-weight: 800;
        }

        .kpiHint{
          margin-top: 6px;
          font-size: 12px;
          color: rgba(226,232,240,0.65);
        }

        .error{
          margin-top: 10px;
          padding: 10px 12px;
          border-radius: 12px;
          font-size: 13px;
          text-align: center;
          background: rgba(239,68,68,0.15);
          border: 1px solid rgba(239,68,68,0.4);
          color: #fecaca;
        }

        .tableWrap{
          overflow: auto;
          border-radius: 16px;
          border: 1px solid rgba(255,255,255,0.14);
        }

        table{
          width: 100%;
          border-collapse: collapse;
          min-width: 680px;
          background: rgba(0,0,0,0.10);
        }

        thead th{
          text-align: left;
          padding: 12px;
          font-size: 12px;
          color: #9ca3af;
          font-weight: 700;
          border-bottom: 1px solid rgba(255,255,255,0.12);
          background: rgba(255,255,255,0.05);
        }

        tbody td{
          padding: 12px;
          border-bottom: 1px solid rgba(255,255,255,0.08);
          font-size: 13px;
          color: #e5e7eb;
        }

        tbody tr:hover{
          background: rgba(255,255,255,0.06);
        }

        .mono{
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
          font-size: 12px;
          color: rgba(226,232,240,0.9);
        }

        .money{
          font-weight: 800;
        }

        .badge{
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 10px;
          border-radius: 999px;
          font-size: 12px;
          font-weight: 700;
          border: 1px solid rgba(255,255,255,0.14);
          background: rgba(255,255,255,0.06);
        }

        .badge-ok{
          border-color: rgba(34,197,94,0.40);
          background: rgba(34,197,94,0.12);
          color: #bbf7d0;
        }

        .badge-bad{
          border-color: rgba(239,68,68,0.40);
          background: rgba(239,68,68,0.12);
          color: #fecaca;
        }

        .badge-neutral{
          border-color: rgba(148,163,184,0.35);
          background: rgba(148,163,184,0.12);
          color: #e2e8f0;
        }

        .empty{
          text-align: center;
          padding: 24px;
          color: rgba(226,232,240,0.75);
          font-size: 13px;
        }

        .footer{
          margin-top: 14px;
          text-align: center;
          font-size: 12px;
          color: #6b7280;
        }
      `}</style>

      <div className="page">
        <div className="container">
          {/* Top bar */}
          <div className="topbar">
            <div className="titleWrap">
              <div className="logo">eB</div>
              <div>
                <h1>Client Dashboard</h1>
                <div className="subtitle">
                  Overview of your accounts & balances
                </div>
              </div>
            </div>

            <div className="actions">
              <Link to="/client/transfer" className="btn btnPrimary">
                <span aria-hidden="true">↗</span> New transfer
              </Link>

              <button onClick={handleLogout} className="btn btnDanger">
                <span aria-hidden="true">⎋</span> Logout
              </button>
            </div>
          </div>

          {/* KPI + Info */}
          <div className="grid">
            <div className="card">
              <div className="cardTitle">
                <span aria-hidden="true">📊</span> Quick stats
              </div>

              <div className="kpis">
                <div className="kpi">
                  <div className="kpiLabel">Accounts</div>
                  <div className="kpiValue">{accounts.length}</div>
                  <div className="kpiHint">Total linked accounts</div>
                </div>

                <div className="kpi">
                  <div className="kpiLabel">Total balance</div>
                  <div className="kpiValue money">
                    {formatMoney(totalBalance)} MAD
                  </div>
                  <div className="kpiHint">Sum of all balances</div>
                </div>

                <div className="kpi">
                  <div className="kpiLabel">Status</div>
                  <div className="kpiValue">
                    {loading ? "..." : accounts.length ? "Available" : "Empty"}
                  </div>
                  <div className="kpiHint">Data readiness</div>
                </div>
              </div>

              {error && <div className="error">{error}</div>}
            </div>

            <div className="card">
              <div className="cardTitle">
                <span aria-hidden="true">🛡️</span> Security
              </div>
              <div
                style={{
                  color: "rgba(226,232,240,0.78)",
                  fontSize: 13,
                  lineHeight: 1.6,
                }}
              >
                Your session uses a token-based authentication. If you see
                errors, verify your token and backend availability.
              </div>
              <div className="footer" style={{ marginTop: 12 }}>
                © 2025 eBank — Secure & Trusted
              </div>
            </div>
          </div>

          {/* Accounts table */}
          <div className="card">
            <div className="cardTitle">
              <span aria-hidden="true">🏦</span> My accounts
            </div>

            {loading ? (
              <div className="empty">Loading accounts...</div>
            ) : accounts.length === 0 ? (
              <div className="empty">No accounts yet.</div>
            ) : (
              <div className="tableWrap">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>RIB</th>
                      <th>Balance (MAD)</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {accounts.map((a) => (
                      <tr key={a.id}>
                        <td className="mono">{a.id}</td>
                        <td className="mono">{a.rib}</td>
                        <td className="money">{formatMoney(a.balance)}</td>
                        <td>
                          <span className={statusBadgeClass(a.status)}>
                            <span aria-hidden="true">●</span> {a.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="footer">eBank</div>
        </div>
      </div>
    </>
  );
}
