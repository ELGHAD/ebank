import React, { useState } from "react";
import PageHeader from "../../components/PageHeader";
import Pagination from "../../components/Pagination";
import ConfirmButton from "../../components/ConfirmButton";
import OneTimePassword from "../../components/OneTimePassword";
import useQuery from "../../hooks/useQuery";
import { errorMessage, fieldErrors } from "../../api/errors";
import {
  changeOrganizationStatus,
  changeSubscriptionStatus,
  createOrganization,
  listOrganizations,
} from "../../services/adminServices";
import { formatDateTime } from "../../utils/format";

function CreateOrganizationForm({ onCreated }) {
  const [form, setForm] = useState({ name: "", adminEmail: "" });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    const missing = {};
    if (form.name.trim().length < 2) missing.name = "Enter a name of at least 2 characters.";
    if (!form.adminEmail.trim()) missing.adminEmail = "Enter the administrator's email.";
    setErrors(missing);
    setError("");
    if (Object.keys(missing).length > 0) return;
    try {
      setSaving(true);
      const res = await createOrganization({ name: form.name.trim(), adminEmail: form.adminEmail.trim() });
      setForm({ name: "", adminEmail: "" });
      onCreated(res.data);
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err, "The organization could not be created."));
    } finally {
      setSaving(false);
    }
  };

  const field = (name, label, type = "text") => (
    <div>
      <label htmlFor={`org-${name}`}>{label}</label>
      <input
        id={`org-${name}`}
        name={name}
        type={type}
        value={form[name]}
        onChange={onChange}
        autoComplete="off"
        aria-invalid={Boolean(errors[name])}
        aria-describedby={errors[name] ? `org-${name}-error` : undefined}
      />
      {errors[name] && (
        <div id={`org-${name}-error`} className="fieldError">
          {errors[name]}
        </div>
      )}
    </div>
  );

  return (
    <form onSubmit={onSubmit} noValidate aria-labelledby="create-org-title">
      <h2 id="create-org-title">New organization</h2>
      {error && (
        <div className="alert alertError" role="alert">
          {error}
        </div>
      )}
      <div className="form-grid">
        {field("name", "Organization name")}
        {field("adminEmail", "First administrator's email", "email")}
      </div>
      <div className="actions" style={{ marginTop: 14 }}>
        <button type="submit" className="btn btnPrimary" disabled={saving}>
          {saving ? "Creating…" : "Create organization"}
        </button>
      </div>
    </form>
  );
}

export default function OrganizationsPage() {
  const [page, setPage] = useState(0);
  const [created, setCreated] = useState(null);
  const [notice, setNotice] = useState("");
  const [actionError, setActionError] = useState("");
  const { data, error, loading, reload } = useQuery(
    () => listOrganizations({ page, size: 20 }),
    String(page),
    "Cannot load the organizations.",
  );

  const setStatus = async (org, status) => {
    setNotice("");
    setActionError("");
    try {
      await changeOrganizationStatus(org.id, status);
      setNotice(status === "SUSPENDED" ? `${org.name} is suspended: its users can no longer sign in.` : `${org.name} is active again.`);
      reload();
    } catch (err) {
      setActionError(errorMessage(err, "The status could not be changed."));
    }
  };

  const setPayment = async (org, status) => {
    setNotice("");
    setActionError("");
    try {
      await changeSubscriptionStatus(org.id, status);
      setNotice(
        status === "PAST_DUE"
          ? `Simulated failed payment for ${org.name}: its growth is blocked until payment is recorded.`
          : `Simulated payment recorded for ${org.name}.`,
      );
      reload();
    } catch (err) {
      setActionError(errorMessage(err, "The subscription status could not be changed."));
    }
  };

  return (
    <>
      <PageHeader
        title="Organizations"
        subtitle="Banks hosted on the platform. As platform administrator you manage their lifecycle; you never see their customers or money."
      />

      <div aria-live="polite">
        {notice && <div className="alert alertOk">{notice}</div>}
        {actionError && (
          <div className="alert alertError" role="alert">
            {actionError}
          </div>
        )}
      </div>

      {created ? (
        <OneTimePassword
          title={`${created.organization.name} created`}
          username={created.adminUsername}
          password={created.temporaryPassword}
        >
          <button type="button" className="btn" onClick={() => setCreated(null)}>
            Create another organization
          </button>
        </OneTimePassword>
      ) : (
        <section className="card">
          <CreateOrganizationForm
            onCreated={(result) => {
              setCreated(result);
              reload();
            }}
          />
        </section>
      )}

      <section className="card" aria-labelledby="orgs-title">
        <h2 id="orgs-title">All organizations</h2>
        {error && (
          <div className="alert alertError" role="alert">
            {error}
          </div>
        )}
        {loading && !data ? (
          <p className="empty" role="status">
            Loading organizations…
          </p>
        ) : data && data.content.length === 0 ? (
          <p className="empty">No organization yet.</p>
        ) : data ? (
          <>
            <div className="tableWrap" role="region" aria-label="Organizations" tabIndex={0} aria-busy={loading}>
              <table>
                <thead>
                  <tr>
                    <th scope="col">Name</th>
                    <th scope="col">Status</th>
                    <th scope="col">Plan</th>
                    <th scope="col">Users</th>
                    <th scope="col">Clients</th>
                    <th scope="col">Accounts</th>
                    <th scope="col">Created</th>
                    <th scope="col">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((o) => (
                    <tr key={o.id}>
                      <td>{o.name}</td>
                      <td>
                        <span className={o.status === "ACTIVE" ? "badge badge-ok" : "badge badge-bad"}>
                          {o.status === "ACTIVE" ? "Active" : "Suspended"}
                        </span>
                      </td>
                      <td>
                        {o.plan ?? "-"}{" "}
                        {o.subscriptionStatus === "PAST_DUE" && <span className="badge badge-warn">Payment overdue</span>}
                      </td>
                      <td>{o.users}</td>
                      <td>{o.clients}</td>
                      <td>{o.accounts}</td>
                      <td>{formatDateTime(o.createdAt)}</td>
                      <td>
                        <div className="actions">
                          {o.status === "ACTIVE" ? (
                            <ConfirmButton
                              label="Suspend"
                              question={`Suspend ${o.name}? All its users lose access.`}
                              confirmLabel="Suspend"
                              danger
                              onConfirm={() => setStatus(o, "SUSPENDED")}
                            />
                          ) : (
                            <ConfirmButton
                              label="Reactivate"
                              question={`Reactivate ${o.name}?`}
                              confirmLabel="Reactivate"
                              onConfirm={() => setStatus(o, "ACTIVE")}
                            />
                          )}
                          {o.subscriptionStatus === "PAST_DUE" ? (
                            <ConfirmButton
                              label="Record payment"
                              question={`Record a (simulated) payment for ${o.name}?`}
                              confirmLabel="Record payment"
                              onConfirm={() => setPayment(o, "ACTIVE")}
                            />
                          ) : (
                            <ConfirmButton
                              label="Simulate failed payment"
                              question={`Simulate a failed payment for ${o.name}?`}
                              confirmLabel="Simulate"
                              onConfirm={() => setPayment(o, "PAST_DUE")}
                            />
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination data={data} onPageChange={setPage} disabled={loading} label="organizations" />
          </>
        ) : null}
      </section>
    </>
  );
}
