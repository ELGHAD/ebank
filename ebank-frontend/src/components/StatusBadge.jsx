import React from "react";

const STYLES = { ACTIVE: "badge badge-ok", BLOCKED: "badge badge-warn", CLOSED: "badge badge-bad" };
const LABELS = { ACTIVE: "Active", BLOCKED: "Blocked", CLOSED: "Closed" };

export default function StatusBadge({ status }) {
  return <span className={STYLES[status] ?? "badge"}>{LABELS[status] ?? status}</span>;
}
