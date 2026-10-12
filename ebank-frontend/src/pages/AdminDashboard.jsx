import React, { useState } from "react";
import axiosClient from "../api/axiosClient";
import { errorMessage, fieldErrors } from "../api/errors";
import Navbar from "../components/Navbar";

const CURRENCIES = ["MAD", "EUR", "USD"];

const emptyClientForm = {
  identityRef: "",
  firstName: "",
  lastName: "",
  birthDate: "",
  email: "",
  address: "",
};

const inputStyle = { width: "100%", padding: 6 };
const fieldErrorStyle = { color: "#b91c1c", fontSize: 12, margin: "4px 0 0" };

const Field = ({ id, label, error, children }) => (
  <div>
    <label htmlFor={id}>{label}</label>
    {children}
    {error && (
      <p id={`${id}-error`} style={fieldErrorStyle}>
        {error}
      </p>
    )}
  </div>
);

const AdminDashboard = () => {
  // Client onboarding form
  const [clientForm, setClientForm] = useState(emptyClientForm);
  const [clientErrors, setClientErrors] = useState({});
  const [clientError, setClientError] = useState("");
  const [onboarded, setOnboarded] = useState(null);

  // Account opening form
  const [accountForm, setAccountForm] = useState({ clientId: "", currency: "MAD", initialDeposit: "" });
  const [accountErrors, setAccountErrors] = useState({});
  const [accountError, setAccountError] = useState("");
  const [openedAccount, setOpenedAccount] = useState(null);

  const handleClientChange = (e) => {
    setClientForm({ ...clientForm, [e.target.name]: e.target.value });
  };

  const handleAccountChange = (e) => {
    setAccountForm({ ...accountForm, [e.target.name]: e.target.value });
  };

  const submitClient = async (e) => {
    e.preventDefault();
    setClientError("");
    setClientErrors({});
    setOnboarded(null);

    try {
      const payload = { ...clientForm, birthDate: clientForm.birthDate || null };
      const res = await axiosClient.post("/api/admin/clients", payload);
      setOnboarded(res.data);
      setClientForm(emptyClientForm);
      setAccountForm((f) => ({ ...f, clientId: String(res.data.client.id) }));
    } catch (err) {
      setClientErrors(fieldErrors(err));
      setClientError(errorMessage(err, "Error while creating the client"));
    }
  };

  const submitAccount = async (e) => {
    e.preventDefault();
    setAccountError("");
    setAccountErrors({});
    setOpenedAccount(null);

    try {
      const payload = {
        clientId: accountForm.clientId === "" ? null : Number(accountForm.clientId),
        currency: accountForm.currency,
        // Sent as a string so no binary floating-point conversion happens on the way
        initialDeposit: accountForm.initialDeposit === "" ? null : accountForm.initialDeposit,
      };
      const res = await axiosClient.post("/api/admin/accounts", payload);
      setOpenedAccount(res.data);
    } catch (err) {
      setAccountErrors(fieldErrors(err));
      setAccountError(errorMessage(err, "Error while opening the account"));
    }
  };

  const clientInput = (name, label, type = "text") => (
    <Field id={`client-${name}`} label={label} error={clientErrors[name]}>
      <input
        id={`client-${name}`}
        name={name}
        type={type}
        value={clientForm[name]}
        onChange={handleClientChange}
        aria-invalid={Boolean(clientErrors[name])}
        aria-describedby={clientErrors[name] ? `client-${name}-error` : undefined}
        style={inputStyle}
      />
    </Field>
  );

  return (
    <div>
      <Navbar />

      <div style={{ maxWidth: 900, margin: "0 auto", padding: 20 }}>
        <h2>Agent dashboard</h2>

        {/* --------- Client onboarding --------- */}
        <h3 style={{ marginTop: 20 }}>Onboard a new client</h3>
        <form
          onSubmit={submitClient}
          noValidate
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
            gap: 12,
            marginTop: 10,
          }}
        >
          {clientInput("identityRef", "Identity reference")}
          {clientInput("email", "Email (used as login)", "email")}
          {clientInput("firstName", "First name")}
          {clientInput("lastName", "Last name")}
          {clientInput("birthDate", "Birth date", "date")}
          {clientInput("address", "Address")}

          <div style={{ gridColumn: "1 / -1", marginTop: 10 }}>
            <button type="submit" style={{ padding: 8, cursor: "pointer" }}>
              Create client
            </button>
          </div>
        </form>

        {clientError && (
          <p role="alert" style={{ color: "#b91c1c", marginTop: 10 }}>
            {clientError}
          </p>
        )}
        {onboarded && (
          <div role="status" style={{ marginTop: 10, padding: 12, border: "1px solid #16a34a", borderRadius: 8 }}>
            <p style={{ margin: 0, color: "#15803d" }}>
              Client #{onboarded.client.id} created. Login: <b>{onboarded.username}</b>
            </p>
            <p style={{ margin: "6px 0 0" }}>
              Temporary password: <code>{onboarded.temporaryPassword}</code>
            </p>
            <p style={{ margin: "6px 0 0", fontSize: 13, color: "#555" }}>
              Give it to the client now: it is shown only once and cannot be retrieved later.
            </p>
          </div>
        )}

        {/* --------- Account opening --------- */}
        <h3 style={{ marginTop: 40 }}>Open a bank account</h3>

        <form
          onSubmit={submitAccount}
          noValidate
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
            gap: 12,
            marginTop: 10,
          }}
        >
          <Field id="account-clientId" label="Client ID" error={accountErrors.clientId}>
            <input
              id="account-clientId"
              name="clientId"
              type="number"
              min="1"
              value={accountForm.clientId}
              onChange={handleAccountChange}
              aria-invalid={Boolean(accountErrors.clientId)}
              style={inputStyle}
            />
          </Field>
          <Field id="account-currency" label="Currency" error={accountErrors.currency}>
            <select
              id="account-currency"
              name="currency"
              value={accountForm.currency}
              onChange={handleAccountChange}
              style={inputStyle}
            >
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </Field>
          <Field id="account-initialDeposit" label="Initial deposit" error={accountErrors.initialDeposit}>
            <input
              id="account-initialDeposit"
              name="initialDeposit"
              type="number"
              min="0"
              step="0.01"
              value={accountForm.initialDeposit}
              onChange={handleAccountChange}
              aria-invalid={Boolean(accountErrors.initialDeposit)}
              style={inputStyle}
            />
          </Field>
          <div style={{ gridColumn: "1 / -1", marginTop: 10 }}>
            <button type="submit" style={{ padding: 8, cursor: "pointer" }}>
              Open account
            </button>
          </div>
        </form>

        {accountError && (
          <p role="alert" style={{ color: "#b91c1c", marginTop: 10 }}>
            {accountError}
          </p>
        )}
        {openedAccount && (
          <p role="status" style={{ color: "#15803d", marginTop: 10 }}>
            Account #{openedAccount.id} opened. RIB: <code>{openedAccount.rib}</code>, balance{" "}
            {openedAccount.balance} {openedAccount.currency}
          </p>
        )}

        <p style={{ marginTop: 40, fontSize: 12, color: "#6b7280" }}>
          Banking simulation for demonstration purposes. Use synthetic data only.
        </p>
      </div>
    </div>
  );
};

export default AdminDashboard;
