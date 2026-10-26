import React, { useState } from "react";
import { Link, useParams } from "react-router-dom";
import PageHeader from "../../components/PageHeader";
import StatusBadge from "../../components/StatusBadge";
import ConfirmButton from "../../components/ConfirmButton";
import useQuery from "../../hooks/useQuery";
import { errorMessage, fieldErrors } from "../../api/errors";
import { changeAccountStatus, getClient, openAccount } from "../../services/agentService";
import { formatDateTime, formatMoney } from "../../utils/format";

const CURRENCIES = ["MAD", "EUR", "USD"];
const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/;

function OpenAccountForm({ clientId, onOpened }) {
  const [currency, setCurrency] = useState("MAD");
  const [deposit, setDeposit] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (deposit !== "" && !AMOUNT_PATTERN.test(deposit)) {
      setError("Enter an amount with at most 2 decimals.");
      return;
    }
    setError("");
    try {
      setSaving(true);
      // Sent as a string so no binary floating-point conversion happens on the way
      const res = await openAccount({ clientId, currency, initialDeposit: deposit === "" ? null : deposit });
      setDeposit("");
      onOpened(res.data);
    } catch (err) {
      setError(fieldErrors(err).initialDeposit || errorMessage(err, "The account could not be opened."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={onSubmit} noValidate aria-labelledby="open-account-title">
      <h2 id="open-account-title">Open an account</h2>
      <div className="form-grid">
        <div>
          <label htmlFor="account-currency">Currency</label>
          <select id="account-currency" value={currency} onChange={(e) => setCurrency(e.target.value)}>
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="account-initialDeposit">Initial deposit (simulated funds)</label>
          <input
            id="account-initialDeposit"
            inputMode="decimal"
            value={deposit}
            placeholder="0.00"
            onChange={(e) => setDeposit(e.target.value)}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? "account-error" : undefined}
          />
        </div>
      </div>
      {error && (
        <div id="account-error" className="fieldError" role="alert">
          {error}
        </div>
      )}
      <div className="actions" style={{ marginTop: 14 }}>
        <button type="submit" className="btn btnPrimary" disabled={saving}>
          {saving ? "Opening…" : "Open account"}
        </button>
      </div>
    </form>
  );
}

export default function ClientDetailPage() {
  const { clientId } = useParams();
  const { data, error, loading, reload } = useQuery(() => getClient(clientId), clientId, "Cannot load this client.");
  const [notice, setNotice] = useState("");
  const [actionError, setActionError] = useState("");

  const setStatus = async (account, status) => {
    setNotice("");
    setActionError("");
    try {
      await changeAccountStatus(account.id, status);
      setNotice(`Account ${account.rib} is now ${status.toLowerCase()}.`);
      reload();
    } catch (err) {
      setActionError(errorMessage(err, "The status could not be changed."));
    }
  };

  const onOpened = (account) => {
    setActionError("");
    setNotice(`Account ${account.rib} opened with ${formatMoney(account.balance)} ${account.currency}.`);
    reload();
  };

  if (loading && !data) {
    return (
      <p className="empty" role="status">
        Loading client…
      </p>
    );
  }
  if (!data) {
    return (
      <>
        <PageHeader title="Client" breadcrumb={<Link to="/admin">← Clients</Link>} />
        <div className="alert alertError" role="alert">
          {error}
        </div>
      </>
    );
  }

  const { client, accounts, passwordChangeRequired } = data;

  return (
    <>
      <PageHeader
        title={`${client.firstName} ${client.lastName}`}
        breadcrumb={<Link to="/admin">← Clients</Link>}
        subtitle={client.email}
      />

      <div aria-live="polite">
        {notice && <div className="alert alertOk">{notice}</div>}
        {actionError && (
          <div className="alert alertError" role="alert">
            {actionError}
          </div>
        )}
      </div>

      <div className="grid-2">
        <section className="card" aria-labelledby="profile-title">
          <h2 id="profile-title">Profile</h2>
          <dl className="summary">
            <dt>Identity ref.</dt>
            <dd className="mono">{client.identityRef}</dd>
            <dt>Birth date</dt>
            <dd>{client.birthDate ?? "-"}</dd>
            <dt>Address</dt>
            <dd>{client.address ?? "-"}</dd>
            <dt>Client since</dt>
            <dd>{formatDateTime(client.createdAt)}</dd>
            <dt>Sign-in</dt>
            <dd>
              {passwordChangeRequired ? (
                <span className="badge badge-warn">Temporary password not changed yet</span>
              ) : (
                <span className="badge badge-ok">Own password set</span>
              )}
            </dd>
          </dl>
        </section>

        <section className="card">
          <OpenAccountForm clientId={client.id} onOpened={onOpened} />
        </section>
      </div>

      <section className="card" aria-labelledby="accounts-title">
        <h2 id="accounts-title">Accounts</h2>
        {accounts.length === 0 ? (
          <p className="empty">This client has no account yet.</p>
        ) : (
          <div className="tableWrap" role="region" aria-label="Client accounts" tabIndex={0} aria-busy={loading}>
            <table>
              <thead>
                <tr>
                  <th scope="col">RIB</th>
                  <th scope="col">Currency</th>
                  <th scope="col">Balance</th>
                  <th scope="col">Status</th>
                  <th scope="col">Actions</th>
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
                      <div className="actions">
                        <Link className="btn btnSmall" to={`/admin/accounts/${a.id}`}>
                          History
                        </Link>
                        {a.status === "ACTIVE" && (
                          <ConfirmButton
                            label="Block"
                            question="Block this account?"
                            confirmLabel="Block"
                            onConfirm={() => setStatus(a, "BLOCKED")}
                          />
                        )}
                        {a.status === "BLOCKED" && (
                          <ConfirmButton
                            label="Unblock"
                            question="Unblock this account?"
                            confirmLabel="Unblock"
                            onConfirm={() => setStatus(a, "ACTIVE")}
                          />
                        )}
                        {a.status !== "CLOSED" && (
                          <ConfirmButton
                            label="Close"
                            question="Close permanently? This cannot be undone."
                            confirmLabel="Close account"
                            danger
                            disabled={Number(a.balance) !== 0}
                            onConfirm={() => setStatus(a, "CLOSED")}
                          />
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="hint">Only an account with a zero balance can be closed. Closing is final.</p>
      </section>
    </>
  );
}
