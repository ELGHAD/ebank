import React, { useState } from "react";

/**
 * A button that asks for confirmation inline before running a consequential action
 * (no modal: keyboard and screen-reader friendly, and the question stays next to its context).
 */
export default function ConfirmButton({ label, question, confirmLabel = "Confirm", onConfirm, danger = false, disabled }) {
  const [asking, setAsking] = useState(false);
  const [busy, setBusy] = useState(false);

  if (!asking) {
    return (
      <button
        type="button"
        className={`btn btnSmall${danger ? " btnDanger" : ""}`}
        disabled={disabled}
        onClick={() => setAsking(true)}
      >
        {label}
      </button>
    );
  }

  const confirm = async () => {
    setBusy(true);
    try {
      await onConfirm();
    } finally {
      setBusy(false);
      setAsking(false);
    }
  };

  return (
    <span className="confirm" role="group" aria-label={question}>
      <span>{question}</span>
      <button
        type="button"
        className={`btn btnSmall${danger ? " btnDanger" : " btnPrimary"}`}
        disabled={busy}
        onClick={confirm}
      >
        {busy ? "Working…" : confirmLabel}
      </button>
      <button type="button" className="btn btnSmall" disabled={busy} onClick={() => setAsking(false)}>
        Cancel
      </button>
    </span>
  );
}
