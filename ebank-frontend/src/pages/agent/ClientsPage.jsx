import React, { useState } from "react";
import { Link } from "react-router-dom";
import PageHeader from "../../components/PageHeader";
import Pagination from "../../components/Pagination";
import useQuery from "../../hooks/useQuery";
import { searchClients } from "../../services/agentService";
import { formatDateTime } from "../../utils/format";

const PAGE_SIZE = 10;

export default function ClientsPage() {
  const [draft, setDraft] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);

  const { data, error, loading } = useQuery(
    () => searchClients({ q: query || undefined, page, size: PAGE_SIZE }),
    `${query}|${page}`,
    "Cannot load the clients.",
  );

  const onSearch = (e) => {
    e.preventDefault();
    setQuery(draft.trim());
    setPage(0);
  };

  return (
    <>
      <PageHeader
        title="Clients"
        subtitle="Clients of your bank. Search by name, email or identity reference."
        actions={
          <Link className="btn btnPrimary" to="/admin/clients/new">
            Onboard a client
          </Link>
        }
      />

      <section className="card" aria-labelledby="clients-results">
        <form className="search-form" role="search" onSubmit={onSearch}>
          <div>
            <label htmlFor="client-search">Search clients</label>
            <input
              id="client-search"
              type="search"
              value={draft}
              maxLength={100}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="Name, email or identity reference"
            />
          </div>
          <button type="submit" className="btn">
            Search
          </button>
          {query && (
            <button
              type="button"
              className="btn"
              onClick={() => {
                setDraft("");
                setQuery("");
                setPage(0);
              }}
            >
              Clear
            </button>
          )}
        </form>

        <h2 id="clients-results" className="sr-only">
          Results
        </h2>
        {error && (
          <div className="alert alertError" role="alert">
            {error}
          </div>
        )}
        {loading && !data ? (
          <p className="empty" role="status">
            Loading clients…
          </p>
        ) : data && data.content.length === 0 ? (
          <p className="empty">
            {query ? `No client matches “${query}”.` : "No clients yet. Start by onboarding one."}
          </p>
        ) : data ? (
          <>
            <div className="tableWrap" role="region" aria-label="Clients" tabIndex={0} aria-busy={loading}>
              <table>
                <caption className="sr-only">Clients, sorted by last name</caption>
                <thead>
                  <tr>
                    <th scope="col">Name</th>
                    <th scope="col">Email</th>
                    <th scope="col">Identity ref.</th>
                    <th scope="col">Accounts</th>
                    <th scope="col">Since</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((c) => (
                    <tr key={c.id}>
                      <td>
                        <Link className="link" to={`/admin/clients/${c.id}`}>
                          {c.lastName} {c.firstName}
                        </Link>
                      </td>
                      <td>{c.email}</td>
                      <td className="mono">{c.identityRef}</td>
                      <td>{c.accountCount}</td>
                      <td>{formatDateTime(c.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination data={data} onPageChange={setPage} disabled={loading} label="clients" />
          </>
        ) : null}
      </section>
    </>
  );
}
