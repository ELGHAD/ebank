import React, { useState } from "react";
import PageHeader from "../../components/PageHeader";
import Pagination from "../../components/Pagination";
import useQuery from "../../hooks/useQuery";
import { getAuditEvents } from "../../services/agentService";
import { formatDateTime } from "../../utils/format";

const PAGE_SIZE = 20;

export const EVENT_LABELS = {
  LOGIN_SUCCEEDED: "Login",
  LOGIN_FAILED: "Failed login",
  LOGIN_THROTTLED: "Login throttled",
  LOGOUT: "Logout",
  PASSWORD_CHANGED: "Password changed",
  PASSWORD_CHANGE_FAILED: "Password change failed",
  CLIENT_ONBOARDED: "Client onboarded",
  ACCOUNT_OPENED: "Account opened",
  ACCOUNT_STATUS_CHANGED: "Account status changed",
  TRANSFER_COMPLETED: "Transfer completed",
};

const ALERT_TYPES = new Set(["LOGIN_FAILED", "LOGIN_THROTTLED", "PASSWORD_CHANGE_FAILED"]);

export default function AuditLogPage() {
  const [type, setType] = useState("");
  const [page, setPage] = useState(0);
  const { data, error, loading } = useQuery(
    () => getAuditEvents({ page, size: PAGE_SIZE, type: type || undefined }),
    `${type}|${page}`,
    "Cannot load the audit log.",
  );

  return (
    <>
      <PageHeader title="Audit log" subtitle="Security and business events of your bank, newest first. Read-only." />
      <section className="card">
        <div style={{ maxWidth: 280, marginBottom: 14 }}>
          <label htmlFor="audit-type">Event type</label>
          <select
            id="audit-type"
            value={type}
            onChange={(e) => {
              setType(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All events</option>
            {Object.entries(EVENT_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        {error && (
          <div className="alert alertError" role="alert">
            {error}
          </div>
        )}
        {loading && !data ? (
          <p className="empty" role="status">
            Loading events…
          </p>
        ) : data && data.content.length === 0 ? (
          <p className="empty">No events to show.</p>
        ) : data ? (
          <>
            <div className="tableWrap" role="region" aria-label="Audit events" tabIndex={0} aria-busy={loading}>
              <table>
                <caption className="sr-only">Audit events, newest first</caption>
                <thead>
                  <tr>
                    <th scope="col">When</th>
                    <th scope="col">Event</th>
                    <th scope="col">User</th>
                    <th scope="col">Target</th>
                    <th scope="col">Details</th>
                    <th scope="col">IP address</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((e) => (
                    <tr key={e.id}>
                      <td>{formatDateTime(e.createdAt)}</td>
                      <td>
                        <span className={ALERT_TYPES.has(e.type) ? "badge badge-warn" : "badge"}>
                          {EVENT_LABELS[e.type] ?? e.type}
                        </span>
                      </td>
                      <td>{e.actorUsername ?? "-"}</td>
                      <td className="mono">{e.target ?? "-"}</td>
                      <td>{e.details ?? "-"}</td>
                      <td className="mono">{e.ipAddress ?? "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination data={data} onPageChange={setPage} disabled={loading} label="events" />
          </>
        ) : null}
      </section>
    </>
  );
}
