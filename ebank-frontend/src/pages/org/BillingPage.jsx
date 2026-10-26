import React, { useState } from "react";
import PageHeader from "../../components/PageHeader";
import ConfirmButton from "../../components/ConfirmButton";
import UsageMeter from "../../components/UsageMeter";
import useQuery from "../../hooks/useQuery";
import { errorMessage } from "../../api/errors";
import { changePlan, getSubscription, listPlans } from "../../services/adminServices";
import { formatDateTime, formatMoney } from "../../utils/format";

const LIMIT_LABELS = [
  ["maxStaff", "staff members"],
  ["maxClients", "clients"],
  ["maxAccounts", "open accounts"],
  ["maxTransfersPerMonth", "transfers / month"],
];

const limitText = (value) => (value === null || value === undefined ? "Unlimited" : value.toLocaleString("en-US"));

export default function BillingPage() {
  const { data, error, loading, reload } = useQuery(getSubscription, "subscription", "Cannot load the subscription.");
  const { data: plans } = useQuery(listPlans, "plans");
  const [notice, setNotice] = useState("");
  const [actionError, setActionError] = useState("");

  const switchTo = async (plan) => {
    setNotice("");
    setActionError("");
    try {
      await changePlan(plan.code);
      setNotice(
        Number(plan.monthlyPrice) > 0
          ? `You are now on ${plan.name}. A simulated invoice of ${formatMoney(plan.monthlyPrice)} ${plan.currency} was issued.`
          : `You are now on ${plan.name}.`,
      );
      reload();
    } catch (err) {
      setActionError(errorMessage(err, "The plan could not be changed."));
    }
  };

  return (
    <>
      <PageHeader
        title="Plan & billing"
        subtitle="Billing is simulated: no payment is taken and no card details are ever requested or stored."
      />

      <div aria-live="polite">
        {notice && <div className="alert alertOk">{notice}</div>}
        {actionError && (
          <div className="alert alertError" role="alert">
            {actionError}
          </div>
        )}
      </div>
      {error && (
        <div className="alert alertError" role="alert">
          {error}
        </div>
      )}

      {loading && !data ? (
        <p className="empty" role="status">
          Loading subscription…
        </p>
      ) : data ? (
        <>
          {data.status === "PAST_DUE" && (
            <div className="alert alertError" role="alert">
              Payment overdue (simulated): new clients, accounts and staff are blocked until it is settled. Customers
              keep full access to their money. Switching to the Free plan also clears it.
            </div>
          )}

          <section className="card" aria-labelledby="usage-title">
            <h2 id="usage-title">
              {data.plan.name} plan: usage this period
            </h2>
            <p className="hint" style={{ marginTop: -6 }}>
              Period {formatDateTime(data.currentPeriodStart)} to {formatDateTime(data.currentPeriodEnd)}. Transfers are
              counted per calendar month (UTC).
            </p>
            <div className="meters">
              {data.usage.map((u) => (
                <UsageMeter key={u.quota} label={u.label} used={u.used} limit={u.limit} />
              ))}
            </div>
          </section>

          <section className="card" aria-labelledby="plans-title">
            <h2 id="plans-title">Plans</h2>
            <div className="plan-grid">
              {(plans ?? []).map((plan) => {
                const current = plan.code === data.plan.code;
                return (
                  <article key={plan.code} className="plan-card" aria-current={current} aria-labelledby={`plan-${plan.code}`}>
                    <h3 id={`plan-${plan.code}`}>{plan.name}</h3>
                    <p className="plan-price">
                      {formatMoney(plan.monthlyPrice)} {plan.currency}
                      <span className="hint"> / month{Number(plan.monthlyPrice) > 0 && " (simulated)"}</span>
                    </p>
                    <ul>
                      {LIMIT_LABELS.map(([key, label]) => (
                        <li key={key}>
                          {limitText(plan[key])} {label}
                        </li>
                      ))}
                    </ul>
                    {current ? (
                      <span className="badge badge-ok">Current plan</span>
                    ) : (
                      <ConfirmButton
                        label={`Switch to ${plan.name}`}
                        question={
                          Number(plan.monthlyPrice) > 0
                            ? `Switch to ${plan.name}? A simulated invoice will be issued.`
                            : `Switch to ${plan.name}?`
                        }
                        confirmLabel="Switch"
                        onConfirm={() => switchTo(plan)}
                      />
                    )}
                  </article>
                );
              })}
            </div>
          </section>

          <section className="card" aria-labelledby="invoices-title">
            <h2 id="invoices-title">Invoices (simulated)</h2>
            {data.invoices.length === 0 ? (
              <p className="empty">No invoices: the Free plan is not billed.</p>
            ) : (
              <div className="tableWrap" role="region" aria-label="Invoices" tabIndex={0}>
                <table>
                  <thead>
                    <tr>
                      <th scope="col">Period</th>
                      <th scope="col">Plan</th>
                      <th scope="col">Amount</th>
                      <th scope="col">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.invoices.map((i) => (
                      <tr key={i.id}>
                        <td>
                          {formatDateTime(i.periodStart)} to {formatDateTime(i.periodEnd)}
                        </td>
                        <td>{i.planCode}</td>
                        <td className="money">
                          {formatMoney(i.amount)} {i.currency}
                        </td>
                        <td>
                          <span className="badge">Paid (simulated)</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : null}
    </>
  );
}
