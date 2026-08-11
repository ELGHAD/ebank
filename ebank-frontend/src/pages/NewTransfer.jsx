import { useEffect, useMemo, useState } from "react";
import { createTransfer, getMyAccounts } from "../services/clientService";
import { useNavigate } from "react-router-dom";

export default function NewTransfer() {
  const navigate = useNavigate();

  const [accounts, setAccounts] = useState([]);
  const [fromAccountId, setFromAccountId] = useState(null);

  const [fromRib, setFromRib] = useState("");
  const [toRib, setToRib] = useState("");
  const [amount, setAmount] = useState("");
  const [motif, setMotif] = useState("");

  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        setErr("");
        setLoadingAccounts(true);
        const res = await getMyAccounts();
        const list = res.data || [];
        setAccounts(list);

        if (list.length > 0) {
          setFromAccountId(list[0].id);
          setFromRib(list[0].rib);
        }
      } catch (e) {
        setErr("Cannot load accounts for transfer.");
      } finally {
        setLoadingAccounts(false);
      }
    };
    load();
  }, []);

  const onChangeAccount = (id) => {
    const acc = accounts.find((a) => a.id === Number(id));
    setFromAccountId(Number(id));
    setFromRib(acc?.rib || "");
  };

  const canSubmit = useMemo(() => {
    const a = Number(amount);
    return Boolean(fromRib && toRib && amount && !Number.isNaN(a) && a > 0);
  }, [fromRib, toRib, amount]);

  const onSubmit = async (e) => {
    e.preventDefault();
    setErr("");
    setMsg("");

    if (!fromRib || !toRib || !amount) {
      setErr("Please fill all fields.");
      return;
    }
    if (Number(amount) <= 0) {
      setErr("Amount must be > 0.");
      return;
    }

    try {
      setSubmitting(true);
      await createTransfer({
        fromRib,
        toRib,
        amount: Number(amount),
        motif,
      });

      setMsg("Transfer completed successfully ✅");
      setTimeout(() => navigate("/client"), 800);
    } catch (e2) {
      console.error(e2);
      setErr(e2?.response?.data?.message || "Transfer failed.");
    } finally {
      setSubmitting(false);
    }
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
          width: min(980px, 100%);
        }

        .topbar{
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 12px;
          margin-bottom: 14px;
          flex-wrap: wrap;
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
          font-weight: 700;
          font-size: 13px;
          transition: transform .12s ease, background .12s ease, filter .12s ease;
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
        .btnPrimary:disabled{ opacity: .7; cursor: not-allowed; }

        .btnBack{
          border: 1px solid rgba(255,255,255,0.16);
          background: rgba(255,255,255,0.06);
          color: #e5e7eb;
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
          font-weight: 800;
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .grid{
          display: grid;
          grid-template-columns: 1fr;
          gap: 14px;
        }

        .form{
          max-width: 560px;
        }

        .field{
          margin-bottom: 12px;
        }

        .label{
          display: block;
          margin-bottom: 6px;
          font-size: 13px;
          color: #9ca3af;
          font-weight: 700;
        }

        .inputWrap{
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px;
          border-radius: 12px;
          background: rgba(0,0,0,0.25);
          border: 1px solid rgba(255,255,255,0.18);
        }

        .inputWrap:focus-within{
          border-color: #22c55e;
          box-shadow: 0 0 0 3px rgba(34,197,94,0.25);
        }

        input, select{
          width: 100%;
          background: transparent;
          border: none;
          outline: none;
          color: #fff;
          font-size: 14px;
        }

        input::placeholder{
          color: rgba(226,232,240,0.55);
        }

        .hint{
          margin-top: 6px;
          font-size: 12px;
          color: rgba(226,232,240,0.6);
        }

        .row{
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
        }

        @media (max-width: 720px){
          .row{ grid-template-columns: 1fr; }
        }

        .alert{
          padding: 10px 12px;
          border-radius: 12px;
          font-size: 13px;
          text-align: center;
          margin-bottom: 12px;
          border: 1px solid rgba(255,255,255,0.14);
          background: rgba(255,255,255,0.06);
          color: #e5e7eb;
        }

        .alertError{
          background: rgba(239,68,68,0.15);
          border-color: rgba(239,68,68,0.4);
          color: #fecaca;
        }

        .alertOk{
          background: rgba(34,197,94,0.14);
          border-color: rgba(34,197,94,0.35);
          color: #bbf7d0;
        }

        .mini{
          display: inline-flex;
          align-items: center;
          gap: 8px;
          padding: 8px 10px;
          border-radius: 999px;
          border: 1px solid rgba(255,255,255,0.14);
          background: rgba(255,255,255,0.06);
          color: rgba(226,232,240,0.85);
          font-size: 12px;
          font-weight: 700;
        }

        .footer{
          margin-top: 14px;
          text-align: center;
          font-size: 12px;
          color: #6b7280;
        }

        .skeleton{
          height: 42px;
          border-radius: 12px;
          background: linear-gradient(
            90deg,
            rgba(255,255,255,0.05),
            rgba(255,255,255,0.09),
            rgba(255,255,255,0.05)
          );
          background-size: 200% 100%;
          animation: shine 1.1s infinite linear;
          border: 1px solid rgba(255,255,255,0.12);
        }

        @keyframes shine{
          0%{ background-position: 0% 0; }
          100%{ background-position: -200% 0; }
        }
      `}</style>

      <div className="page">
        <div className="container">
          {/* Topbar */}
          <div className="topbar">
            <div className="titleWrap">
              <div className="logo">eB</div>
              <div>
                <h1>New transfer</h1>
                <div className="subtitle">
                  Send money securely between accounts
                </div>
              </div>
            </div>

            <div className="actions">
              <button
                className="btn btnBack"
                onClick={() => navigate("/client")}
              >
                <span aria-hidden="true">←</span> Back
              </button>
            </div>
          </div>

          <div className="grid">
            {/* Info Card */}
            <div className="card">
              <div className="cardTitle">
                <span aria-hidden="true">🧾</span> Transfer form
              </div>

              {err && <div className="alert alertError">{err}</div>}
              {msg && <div className="alert alertOk">{msg}</div>}

              {loadingAccounts ? (
                <>
                  <div className="skeleton" style={{ marginBottom: 12 }} />
                  <div className="skeleton" style={{ marginBottom: 12 }} />
                  <div className="skeleton" style={{ marginBottom: 12 }} />
                </>
              ) : accounts.length === 0 ? (
                <div className="alert alertError">
                  No accounts found. You cannot make a transfer.
                </div>
              ) : (
                <form onSubmit={onSubmit} className="form">
                  {/* Account select if multiple */}
                  {accounts.length > 1 && (
                    <div className="field">
                      <span className="label">Choose account</span>
                      <div className="inputWrap">
                        <select
                          value={fromAccountId || ""}
                          onChange={(e) => onChangeAccount(e.target.value)}
                        >
                          {accounts.map((a) => (
                            <option key={a.id} value={a.id}>
                              {a.rib}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="hint">
                        Select the source account for this transfer.
                      </div>
                    </div>
                  )}

                  <div className="row">
                    <div className="field">
                      <span className="label">From RIB</span>
                      <div className="inputWrap">
                        <input value={fromRib} disabled />
                      </div>
                      <div className="hint">
                        <span className="mini">
                          <span aria-hidden="true">🔒</span> Read-only
                        </span>
                      </div>
                    </div>

                    <div className="field">
                      <span className="label">Amount (MAD)</span>
                      <div className="inputWrap">
                        <input
                          type="number"
                          value={amount}
                          onChange={(e) => setAmount(e.target.value)}
                          placeholder="Ex: 250.00"
                          min="0"
                          step="0.01"
                        />
                      </div>
                      <div className="hint">Amount must be greater than 0.</div>
                    </div>
                  </div>

                  <div className="field">
                    <span className="label">To RIB</span>
                    <div className="inputWrap">
                      <input
                        value={toRib}
                        onChange={(e) => setToRib(e.target.value)}
                        placeholder="Enter destination RIB"
                      />
                    </div>
                    <div className="hint">
                      Make sure the destination RIB is correct.
                    </div>
                  </div>

                  <div className="field">
                    <span className="label">Motif (optional)</span>
                    <div className="inputWrap">
                      <input
                        value={motif}
                        onChange={(e) => setMotif(e.target.value)}
                        placeholder="Ex: Rent / Invoice / Family..."
                      />
                    </div>
                    <div className="hint">
                      Short description for this transfer.
                    </div>
                  </div>

                  <button
                    className="btn btnPrimary"
                    type="submit"
                    disabled={!canSubmit || submitting}
                    style={{ width: "100%", justifyContent: "center" }}
                  >
                    {submitting ? (
                      <>
                        <span aria-hidden="true">⏳</span> Processing...
                      </>
                    ) : (
                      <>
                        <span aria-hidden="true">✓</span> Validate transfer
                      </>
                    )}
                  </button>
                </form>
              )}
            </div>

            <div className="footer">© 2025 eBank — Secure & Trusted</div>
          </div>
        </div>
      </div>
    </>
  );
}
