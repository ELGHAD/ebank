import React, { useState } from "react";
import PageHeader from "../../components/PageHeader";
import ConfirmButton from "../../components/ConfirmButton";
import OneTimePassword from "../../components/OneTimePassword";
import useQuery from "../../hooks/useQuery";
import { useAuth } from "../../context/AuthContext";
import { errorMessage, fieldErrors } from "../../api/errors";
import { createStaff, listStaff, setStaffEnabled } from "../../services/adminServices";
import { formatDateTime } from "../../utils/format";

const ROLE_LABELS = { AGENT: "Agent", ORG_ADMIN: "Bank admin" };

function AddStaffForm({ onCreated }) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("AGENT");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim()) {
      setError("Enter an email address.");
      return;
    }
    setError("");
    try {
      setSaving(true);
      const res = await createStaff({ email: email.trim(), role });
      setEmail("");
      onCreated(res.data);
    } catch (err) {
      setError(fieldErrors(err).email || errorMessage(err, "The staff member could not be created."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form onSubmit={onSubmit} noValidate aria-labelledby="add-staff-title">
      <h2 id="add-staff-title">Add a staff member</h2>
      <div className="form-grid">
        <div>
          <label htmlFor="staff-email">Email</label>
          <input
            id="staff-email"
            type="email"
            autoComplete="off"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? "staff-error" : undefined}
          />
        </div>
        <div>
          <label htmlFor="staff-role">Role</label>
          <select id="staff-role" value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="AGENT">Agent (serves clients)</option>
            <option value="ORG_ADMIN">Bank admin (manages staff)</option>
          </select>
        </div>
      </div>
      {error && (
        <div id="staff-error" className="fieldError" role="alert">
          {error}
        </div>
      )}
      <div className="actions" style={{ marginTop: 14 }}>
        <button type="submit" className="btn btnPrimary" disabled={saving}>
          {saving ? "Creating…" : "Add staff member"}
        </button>
      </div>
    </form>
  );
}

export default function StaffPage() {
  const { auth } = useAuth();
  const { data, error, loading, reload } = useQuery(listStaff, "staff", "Cannot load the staff.");
  const [created, setCreated] = useState(null);
  const [notice, setNotice] = useState("");
  const [actionError, setActionError] = useState("");

  const toggle = async (member, enabled) => {
    setNotice("");
    setActionError("");
    try {
      await setStaffEnabled(member.id, enabled);
      setNotice(`${member.username} is now ${enabled ? "enabled" : "disabled"}.`);
      reload();
    } catch (err) {
      setActionError(errorMessage(err, "The change could not be saved."));
    }
  };

  return (
    <>
      <PageHeader title="Staff" subtitle="Agents and administrators of your bank." />

      <div aria-live="polite">
        {notice && <div className="alert alertOk">{notice}</div>}
        {actionError && (
          <div className="alert alertError" role="alert">
            {actionError}
          </div>
        )}
      </div>

      {created ? (
        <OneTimePassword title={`${ROLE_LABELS[created.member.roles[0]] ?? "Staff member"} created`}
                         username={created.member.username} password={created.temporaryPassword}>
          <button type="button" className="btn" onClick={() => setCreated(null)}>
            Add another staff member
          </button>
        </OneTimePassword>
      ) : (
        <section className="card">
          <AddStaffForm
            onCreated={(result) => {
              setCreated(result);
              reload();
            }}
          />
        </section>
      )}

      <section className="card" aria-labelledby="staff-title">
        <h2 id="staff-title">Team</h2>
        {error && (
          <div className="alert alertError" role="alert">
            {error}
          </div>
        )}
        {loading && !data ? (
          <p className="empty" role="status">
            Loading staff…
          </p>
        ) : data ? (
          <div className="tableWrap" role="region" aria-label="Staff" tabIndex={0} aria-busy={loading}>
            <table>
              <thead>
                <tr>
                  <th scope="col">Email</th>
                  <th scope="col">Role</th>
                  <th scope="col">Status</th>
                  <th scope="col">Since</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.map((m) => (
                  <tr key={m.id}>
                    <td>{m.username}</td>
                    <td>{m.roles.map((r) => ROLE_LABELS[r] ?? r).join(", ")}</td>
                    <td>
                      {!m.enabled ? (
                        <span className="badge badge-bad">Disabled</span>
                      ) : m.passwordChangeRequired ? (
                        <span className="badge badge-warn">Temporary password</span>
                      ) : (
                        <span className="badge badge-ok">Active</span>
                      )}
                    </td>
                    <td>{formatDateTime(m.createdAt)}</td>
                    <td>
                      {m.username === auth.username ? (
                        <span className="hint">You</span>
                      ) : m.enabled ? (
                        <ConfirmButton
                          label="Disable"
                          question={`Disable ${m.username}? They lose access immediately.`}
                          confirmLabel="Disable"
                          danger
                          onConfirm={() => toggle(m, false)}
                        />
                      ) : (
                        <ConfirmButton
                          label="Enable"
                          question={`Enable ${m.username}?`}
                          confirmLabel="Enable"
                          onConfirm={() => toggle(m, true)}
                        />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </>
  );
}
