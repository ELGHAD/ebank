import React, { useId } from "react";

/**
 * Usage of one quota against its plan limit. The fill carries severity (normal, near the limit, limit reached),
 * the track is a dimmer step of the same hue, and the state is always also written out: never color alone.
 */
export default function UsageMeter({ label, used, limit }) {
  // aria-labelledby takes a space-separated id list: the id must not be derived from the (multi-word) label
  const labelId = useId();

  if (limit === null || limit === undefined) {
    return (
      <div className="meter">
        <div className="meter-head">
          <span className="meter-label">{label}</span>
          <span className="meter-value">
            {used} <span className="hint">· unlimited</span>
          </span>
        </div>
      </div>
    );
  }

  const ratio = limit > 0 ? Math.min(used / limit, 1) : 1;
  const level = used >= limit ? "full" : ratio >= 0.8 ? "near" : "ok";
  const state = { ok: null, near: "Near limit", full: "Limit reached" }[level];

  return (
    <div className={`meter meter-${level}`}>
      <div className="meter-head">
        <span className="meter-label" id={labelId}>
          {label}
        </span>
        <span className="meter-value">
          {used} / {limit}
          {state && (
            <span className="meter-state">
              <span aria-hidden="true">{level === "full" ? "⛔" : "⚠"}</span> {state}
            </span>
          )}
        </span>
      </div>
      <div
        className="meter-track"
        role="meter"
        aria-labelledby={labelId}
        aria-valuemin={0}
        aria-valuemax={limit}
        aria-valuenow={Math.min(used, limit)}
        aria-valuetext={`${used} of ${limit}${state ? `, ${state.toLowerCase()}` : ""}`}
      >
        <div className="meter-fill" style={{ width: `${Math.round(ratio * 100)}%` }} />
      </div>
    </div>
  );
}
