import React, { useState } from "react";
import { Link } from "react-router-dom";
import PageHeader from "../../components/PageHeader";
import { errorMessage, fieldErrors } from "../../api/errors";
import { onboardClient } from "../../services/agentService";

const emptyForm = { identityRef: "", firstName: "", lastName: "", birthDate: "", email: "", address: "" };

const FIELDS = [
  { name: "firstName", label: "First name", required: true, autoComplete: "off" },
  { name: "lastName", label: "Last name", required: true, autoComplete: "off" },
  { name: "email", label: "Email (used as login)", type: "email", required: true, autoComplete: "off" },
  { name: "identityRef", label: "Identity reference", required: true, autoComplete: "off" },
  { name: "birthDate", label: "Birth date", type: "date" },
  { name: "address", label: "Address", autoComplete: "off" },
];

export default function OnboardClientPage() {
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [created, setCreated] = useState(null);

  const onChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: undefined });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const missing = {};
    for (const f of FIELDS) {
      if (f.required && !form[f.name].trim()) missing[f.name] = `${f.label.split(" (")[0]} is required.`;
    }
    setErrors(missing);
    setError("");
    if (Object.keys(missing).length > 0) return;

    try {
      setSaving(true);
      const res = await onboardClient({ ...form, birthDate: form.birthDate || null, address: form.address || null });
      setCreated(res.data);
      setForm(emptyForm);
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err, "The client could not be created."));
    } finally {
      setSaving(false);
    }
  };

  if (created) {
    return (
      <>
        <PageHeader title="Client onboarded" breadcrumb={<Link to="/admin">← Clients</Link>} />
        <section className="card" role="status">
          <div className="alert alertOk">
            {created.client.firstName} {created.client.lastName} can now sign in.
          </div>
          <dl className="summary">
            <dt>Login</dt>
            <dd>{created.username}</dd>
            <dt>Temporary password</dt>
            <dd className="mono">{created.temporaryPassword}</dd>
          </dl>
          <div className="alert alertWarn">
            Give this temporary password to the client now: it is shown only once. The client must choose a new
            password at first sign-in.
          </div>
          <div className="actions">
            <Link className="btn btnPrimary" to={`/admin/clients/${created.client.id}`}>
              Open an account for this client
            </Link>
            <button type="button" className="btn" onClick={() => setCreated(null)}>
              Onboard another client
            </button>
          </div>
        </section>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Onboard a client"
        breadcrumb={<Link to="/admin">← Clients</Link>}
        subtitle="Creates the client and a login with a one-time password. Use synthetic data only."
      />
      <section className="card">
        {error && (
          <div className="alert alertError" role="alert">
            {error}
          </div>
        )}
        <form onSubmit={onSubmit} noValidate>
          <div className="form-grid">
            {FIELDS.map((f) => (
              <div key={f.name}>
                <label htmlFor={`client-${f.name}`}>
                  {f.label}
                  {f.required && <span aria-hidden="true"> *</span>}
                </label>
                <input
                  id={`client-${f.name}`}
                  name={f.name}
                  type={f.type ?? "text"}
                  value={form[f.name]}
                  onChange={onChange}
                  required={f.required}
                  autoComplete={f.autoComplete}
                  aria-invalid={Boolean(errors[f.name])}
                  aria-describedby={errors[f.name] ? `client-${f.name}-error` : undefined}
                />
                {errors[f.name] && (
                  <div id={`client-${f.name}-error`} className="fieldError">
                    {errors[f.name]}
                  </div>
                )}
              </div>
            ))}
          </div>
          <div className="actions" style={{ marginTop: 16 }}>
            <button type="submit" className="btn btnPrimary" disabled={saving}>
              {saving ? "Creating…" : "Create client"}
            </button>
          </div>
        </form>
      </section>
    </>
  );
}
