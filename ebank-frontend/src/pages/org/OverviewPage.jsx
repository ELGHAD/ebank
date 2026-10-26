import React from "react";
import { Link } from "react-router-dom";
import PageHeader from "../../components/PageHeader";
import useQuery from "../../hooks/useQuery";
import { getOverview } from "../../services/adminServices";
import { formatMoney } from "../../utils/format";

const amounts = (list) =>
  list.length === 0
    ? "-"
    : list.map((a) => (
        <div key={a.currency}>
          {formatMoney(a.amount)} {a.currency}
        </div>
      ));

export default function OverviewPage() {
  const { data, error, loading } = useQuery(getOverview, "overview", "Cannot load the overview.");

  return (
    <>
      <PageHeader
        title={data ? data.organizationName : "Overview"}
        subtitle="Your bank at a glance (simulated data)."
        actions={
          <Link className="btn btnPrimary" to="/org/staff">
            Manage staff
          </Link>
        }
      />
      {error && (
        <div className="alert alertError" role="alert">
          {error}
        </div>
      )}
      {loading && !data ? (
        <p className="empty" role="status">
          Loading overview…
        </p>
      ) : data ? (
        <>
          <section className="card" aria-labelledby="kpi-customers">
            <h2 id="kpi-customers">Customers and accounts</h2>
            <div className="kpis">
              <div className="kpi">
                <div className="kpiLabel">Clients</div>
                <div className="kpiValue">{data.clients}</div>
              </div>
              <div className="kpi">
                <div className="kpiLabel">Accounts</div>
                <div className="kpiValue">{data.accounts}</div>
              </div>
              <div className="kpi">
                <div className="kpiLabel">Active / blocked / closed</div>
                <div className="kpiValue">
                  {data.activeAccounts} / {data.blockedAccounts} / {data.closedAccounts}
                </div>
              </div>
              <div className="kpi">
                <div className="kpiLabel">Staff</div>
                <div className="kpiValue">{data.staff}</div>
              </div>
            </div>
          </section>
          <section className="card" aria-labelledby="kpi-money">
            <h2 id="kpi-money">Money</h2>
            <div className="kpis">
              <div className="kpi">
                <div className="kpiLabel">Total balances</div>
                <div className="kpiValue money">{amounts(data.balances)}</div>
              </div>
              <div className="kpi">
                <div className="kpiLabel">Transfers, last 30 days</div>
                <div className="kpiValue">{data.transfersLast30Days}</div>
              </div>
              <div className="kpi">
                <div className="kpiLabel">Transfer volume, last 30 days</div>
                <div className="kpiValue money">{amounts(data.transferVolumeLast30Days)}</div>
              </div>
            </div>
          </section>
        </>
      ) : null}
    </>
  );
}
