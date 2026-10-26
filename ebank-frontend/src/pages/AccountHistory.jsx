import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import * as clientService from "../services/clientService";
import * as agentService from "../services/agentService";
import PageHeader from "../components/PageHeader";
import Pagination from "../components/Pagination";
import StatusBadge from "../components/StatusBadge";
import useQuery from "../hooks/useQuery";
import { formatDateTime, formatMoney } from "../utils/format";

const PAGE_SIZE = 10;
const TYPE_LABELS = { DEPOSIT: "Deposit", DEBIT: "Transfer out", CREDIT: "Transfer in" };

/** The same backend checks apply to both roles: a client sees only their own accounts, an agent their bank's. */
const SOURCES = {
  client: {
    loadAccount: (id) =>
      clientService.getMyAccounts().then((res) => ({ data: (res.data || []).find((a) => String(a.id) === id) ?? null })),
    loadEntries: (id, params) => clientService.getAccountEntries(id, params),
    back: () => ({ to: "/client", label: "← My accounts" }),
  },
  agent: {
    loadAccount: (id) => agentService.getAccount(id),
    loadEntries: (id, params) => agentService.getAccountEntries(id, params),
    back: (account) =>
      account ? { to: `/admin/clients/${account.clientId}`, label: "← Client" } : { to: "/admin", label: "← Clients" },
  },
};

export default function AccountHistory({ role = "client" }) {
  const source = SOURCES[role];
  const { accountId } = useParams();
  const [type, setType] = useState("");
  const [page, setPage] = useState(0);

  const { data: account } = useQuery(() => source.loadAccount(accountId), accountId);
  const { data, error, loading } = useQuery(
    () => source.loadEntries(accountId, { page, size: PAGE_SIZE, type: type || undefined }),
    `${accountId}|${page}|${type}`,
    "Cannot load the account history.",
  );

  const back = source.back(account);
  const currency = account?.currency ?? "";

  return (
    <>
      <PageHeader
        title="Account history"
        breadcrumb={<Link to={back.to}>{back.label}</Link>}
        subtitle={account ? <span className="mono">{account.rib}</span> : `Account #${accountId}`}
        actions={
          role === "client" && (
            <Link className="btn btnPrimary" to="/client/transfer">
              New transfer
            </Link>
          )
        }
      />

      <section className="card">
        {account && (
          <p style={{ marginTop: 0 }}>
            Current balance:{" "}
            <strong className="money">
              {formatMoney(account.balance)} {account.currency}
            </strong>{" "}
            <StatusBadge status={account.status} />
          </p>
        )}

        <div style={{ maxWidth: 240, marginBottom: 12 }}>
          <label htmlFor="history-type">Show</label>
          <select
            id="history-type"
            value={type}
            onChange={(e) => {
              setType(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All movements</option>
            <option value="CREDIT">Transfers in</option>
            <option value="DEBIT">Transfers out</option>
            <option value="DEPOSIT">Deposits</option>
          </select>
        </div>

        {error && (
          <div className="alert alertError" role="alert">
            {error}
          </div>
        )}

        {loading && !data ? (
          <p className="empty" role="status">
            Loading history…
          </p>
        ) : data && data.content.length === 0 ? (
          <p className="empty">No movements to show.</p>
        ) : data ? (
          <>
            <div className="tableWrap" role="region" aria-label="Account movements" tabIndex={0} aria-busy={loading}>
              <table>
                <caption className="sr-only">Movements, newest first</caption>
                <thead>
                  <tr>
                    <th scope="col">Date</th>
                    <th scope="col">Type</th>
                    <th scope="col">Counterparty</th>
                    <th scope="col">Description</th>
                    <th scope="col">Amount</th>
                    <th scope="col">Balance after</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((e) => (
                    <tr key={e.id}>
                      <td>{formatDateTime(e.createdAt)}</td>
                      <td>{TYPE_LABELS[e.type] ?? e.type}</td>
                      <td className="mono">{e.counterpartyRib ?? "-"}</td>
                      <td>{e.description ?? "-"}</td>
                      <td className={e.type === "DEBIT" ? "amountOut" : "amountIn"}>
                        {e.type === "DEBIT" ? "−" : "+"}
                        {formatMoney(e.amount)} {currency}
                      </td>
                      <td>
                        {formatMoney(e.balanceAfter)} {currency}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination data={data} onPageChange={setPage} disabled={loading} label="movements" />
          </>
        ) : null}
      </section>
    </>
  );
}
