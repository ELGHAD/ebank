import React from "react";

/** Shows a newly created account's one-time password, once, with the handover instructions. */
export default function OneTimePassword({ title, username, password, children }) {
  return (
    <section className="card" role="status" aria-label={title}>
      <div className="alert alertOk">{title}</div>
      <dl className="summary">
        <dt>Login</dt>
        <dd>{username}</dd>
        <dt>Temporary password</dt>
        <dd className="mono">{password}</dd>
      </dl>
      <div className="alert alertWarn">
        Hand this temporary password over now: it is shown only once. It must be replaced at first sign-in.
      </div>
      {children && <div className="actions">{children}</div>}
    </section>
  );
}
