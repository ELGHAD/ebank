import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { createTransfer, getMyAccounts } from "../services/clientService";
import { errorMessage, fieldErrors } from "../api/errors";
import { isValidRib, normalizeRib } from "../utils/rib";
import { formatMoney } from "../utils/format";
import PageHeader from "../components/PageHeader";

const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/;

/** A fresh key per transfer attempt. Falls back to getRandomValues where randomUUID is unavailable (plain HTTP). */
const newIdempotencyKey = () =>
  crypto.randomUUID?.() ??
  Array.from(crypto.getRandomValues(new Uint8Array(16)), (b) => b.toString(16).padStart(2, "0")).join("");

const emptyForm = { sourceAccountId: "", destinationRib: "", amount: "", description: "" };

export default function NewTransfer() {
  const [accounts, setAccounts] = useState([]);
  const [loadingAccounts, setLoadingAccounts] = useState(true);
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});

  // "form" -> "review" -> "done"
  const [step, setStep] = useState("form");
  const [attemptKey, setAttemptKey] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [canRetry, setCanRetry] = useState(false);
  const [result, setResult] = useState(null);
  const [loadError, setLoadError] = useState("");

  useEffect(() => {
    getMyAccounts()
      .then((res) => {
        const active = (res.data || []).filter((a) => a.status === "ACTIVE");
        setAccounts(active);
        if (active.length > 0) {
          setForm((f) => ({ ...f, sourceAccountId: String(active[0].id) }));
        }
      })
      .catch((e) => setLoadError(errorMessage(e, "Cannot load your accounts.")))
      .finally(() => setLoadingAccounts(false));
  }, []);

  const source = accounts.find((a) => String(a.id) === form.sourceAccountId);

  const update = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: undefined });
  };

  const validate = () => {
    const found = {};
    if (!source) found.sourceAccountId = "Choose a source account.";
    if (!isValidRib(form.destinationRib)) {
      found.destinationRib = "This RIB is not valid. Check for typos (format: EB + 22 digits).";
    } else if (source && normalizeRib(form.destinationRib) === source.rib) {
      found.destinationRib = "The recipient must be a different account.";
    }
    if (!AMOUNT_PATTERN.test(form.amount) || Number(form.amount) <= 0) {
      found.amount = "Enter an amount greater than 0 with at most 2 decimals.";
    } else if (source && Number(form.amount) > Number(source.balance)) {
      found.amount = "The amount exceeds the available balance.";
    }
    if (form.description.length > 140) found.description = "At most 140 characters.";
    return found;
  };

  const onReview = (e) => {
    e.preventDefault();
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length === 0) {
      setAttemptKey(newIdempotencyKey()); // one key for this attempt, kept for retries
      setSubmitError("");
      setCanRetry(false);
      setStep("review");
    }
  };

  const onEdit = () => {
    setAttemptKey(null); // any change makes it a new attempt
    setStep("form");
  };

  const onConfirm = async () => {
    setSubmitting(true);
    setSubmitError("");
    try {
      const res = await createTransfer(
        {
          sourceAccountId: Number(form.sourceAccountId),
          destinationRib: normalizeRib(form.destinationRib),
          amount: form.amount, // decimal string: no binary floating point on the way
          description: form.description.trim() || null,
        },
        attemptKey,
      );
      setResult(res.data);
      setStep("done");
    } catch (err) {
      if (!err.response) {
        // The transfer may or may not have been executed: retrying with the same key is safe
        setCanRetry(true);
        setSubmitError("Network problem: we could not confirm the transfer. You can safely retry.");
      } else {
        setErrors(fieldErrors(err));
        setSubmitError(errorMessage(err, "The transfer was refused."));
        setAttemptKey(null);
        setStep("form");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const onNewTransfer = () => {
    setForm({ ...emptyForm, sourceAccountId: form.sourceAccountId });
    setResult(null);
    setStep("form");
  };

  const fieldProps = (name) => ({
    id: `transfer-${name}`,
    name,
    value: form[name],
    onChange: update,
    "aria-invalid": Boolean(errors[name]),
    "aria-describedby": errors[name] ? `transfer-${name}-error` : undefined,
  });

  const fieldError = (name) =>
    errors[name] && (
      <div id={`transfer-${name}-error`} className="fieldError">
        {errors[name]}
      </div>
    );

  return (
    <>
      <PageHeader
        title="New transfer"
        breadcrumb={<Link to="/client">← My accounts</Link>}
        subtitle="Simulated transfer to any account of your bank"
      />

        <div className="card">
          {loadError && (
            <div className="alert alertError" role="alert">
              {loadError}
            </div>
          )}
          {submitError && (
            <div className="alert alertError" role="alert">
              {submitError}
            </div>
          )}

          {loadingAccounts ? (
            <p className="empty">Loading your accounts…</p>
          ) : accounts.length === 0 ? (
            <p className="empty">You have no active account to transfer from.</p>
          ) : step === "form" ? (
            <form className="form" onSubmit={onReview} noValidate>
              <div>
                <label htmlFor="transfer-sourceAccountId">From account</label>
                <select {...fieldProps("sourceAccountId")}>
                  {accounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.rib}: {formatMoney(a.balance)} {a.currency}
                    </option>
                  ))}
                </select>
                {fieldError("sourceAccountId")}
              </div>
              <div>
                <label htmlFor="transfer-destinationRib">Recipient RIB</label>
                <input {...fieldProps("destinationRib")} placeholder="EB + 22 digits" autoComplete="off" />
                {fieldError("destinationRib")}
              </div>
              <div>
                <label htmlFor="transfer-amount">Amount{source ? ` (${source.currency})` : ""}</label>
                <input {...fieldProps("amount")} inputMode="decimal" placeholder="250.00" />
                {fieldError("amount")}
              </div>
              <div>
                <label htmlFor="transfer-description">Description (optional)</label>
                <input {...fieldProps("description")} maxLength={140} placeholder="Rent, invoice…" />
                {fieldError("description")}
              </div>
              <div className="actions">
                <button type="submit" className="btn btnPrimary">
                  Review transfer
                </button>
              </div>
            </form>
          ) : step === "review" ? (
            <section aria-labelledby="review-title">
              <h2 id="review-title" style={{ fontSize: 16 }}>
                Confirm this transfer
              </h2>
              <dl className="summary">
                <dt>From</dt>
                <dd className="mono">{source?.rib}</dd>
                <dt>To</dt>
                <dd className="mono">{normalizeRib(form.destinationRib)}</dd>
                <dt>Amount</dt>
                <dd>
                  {formatMoney(form.amount)} {source?.currency}
                </dd>
                {form.description.trim() && (
                  <>
                    <dt>Description</dt>
                    <dd>{form.description.trim()}</dd>
                  </>
                )}
              </dl>
              <div className="actions">
                <button type="button" className="btn btnPrimary" onClick={onConfirm} disabled={submitting}>
                  {submitting ? "Processing…" : canRetry ? "Retry" : "Confirm transfer"}
                </button>
                <button type="button" className="btn" onClick={onEdit} disabled={submitting}>
                  Edit
                </button>
              </div>
            </section>
          ) : (
            <section role="status">
              <div className="alert alertOk">Transfer completed.</div>
              <dl className="summary">
                <dt>Amount</dt>
                <dd>
                  {formatMoney(result.amount)} {result.currency}
                </dd>
                <dt>To</dt>
                <dd className="mono">{result.destinationRib}</dd>
                <dt>New balance</dt>
                <dd>
                  {formatMoney(result.balanceAfter)} {result.currency}
                </dd>
                <dt>Reference</dt>
                <dd>#{result.id}</dd>
              </dl>
              <div className="actions">
                <Link className="btn" to={`/client/accounts/${result.sourceAccountId}`}>
                  View account history
                </Link>
                <button type="button" className="btn" onClick={onNewTransfer}>
                  New transfer
                </button>
              </div>
            </section>
          )}
        </div>
    </>
  );
}
