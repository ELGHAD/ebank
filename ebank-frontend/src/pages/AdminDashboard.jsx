import React, { useState } from "react";
import axiosClient from "../api/axiosClient";
import Navbar from "../components/Navbar";

const AdminDashboard = () => {
  // formulaire ajout client
  const [clientForm, setClientForm] = useState({
    identityRef: "",
    firstname: "",
    lastname: "",
    birthDate: "",
    email: "",
    address: "",
  });

  const [clientMsg, setClientMsg] = useState("");
  const [clientError, setClientError] = useState("");

  // formulaire ajout compte
  const [accountForm, setAccountForm] = useState({
    clientId: "",
    rib: "",
    balance: "",
  });

  const [accountMsg, setAccountMsg] = useState("");
  const [accountError, setAccountError] = useState("");

  const handleClientChange = (e) => {
    setClientForm({ ...clientForm, [e.target.name]: e.target.value });
  };

  const handleAccountChange = (e) => {
    setAccountForm({ ...accountForm, [e.target.name]: e.target.value });
  };

  const submitClient = async (e) => {
    e.preventDefault();
    setClientMsg("");
    setClientError("");

    try {
      const res = await axiosClient.post("/api/admin/clients", clientForm);
      setClientMsg(res.data.message);
    } catch (err) {
      console.error(err);
      setClientError(
        err.response?.data?.message ||
          "Error while creating client (identityRef or email may already exist)"
      );
    }
  };

  const submitAccount = async (e) => {
    e.preventDefault();
    setAccountMsg("");
    setAccountError("");

    try {
      const payload = {
        clientId: Number(accountForm.clientId),
        rib: accountForm.rib,
        balance: Number(accountForm.balance),
      };

      const res = await axiosClient.post("/api/admin/accounts", payload);
      setAccountMsg(res.data.message + " (RIB: " + res.data.rib + ")");
    } catch (err) {
      console.error(err);
      setAccountError(
        err.response?.data?.message || "Error while creating bank account"
      );
    }
  };

  return (
    <div>
      <Navbar />

      <div style={{ maxWidth: 900, margin: "0 auto", padding: 20 }}>
        <h2>Admin Dashboard - eBank</h2>

        {/* --------- Ajout client --------- */}
        <h3 style={{ marginTop: 20 }}>Add new client</h3>
        <form
          onSubmit={submitClient}
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 12,
            marginTop: 10,
          }}
        >
          <div>
            <label>Identity Ref</label>
            <input
              name="identityRef"
              value={clientForm.identityRef}
              onChange={handleClientChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div>
            <label>First Name</label>
            <input
              name="firstname"
              value={clientForm.firstname}
              onChange={handleClientChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div>
            <label>Last Name</label>
            <input
              name="lastname"
              value={clientForm.lastname}
              onChange={handleClientChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div>
            <label>Birth Date</label>
            <input
              type="date"
              name="birthDate"
              value={clientForm.birthDate}
              onChange={handleClientChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div>
            <label>Email</label>
            <input
              name="email"
              value={clientForm.email}
              onChange={handleClientChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div>
            <label>Address</label>
            <input
              name="address"
              value={clientForm.address}
              onChange={handleClientChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>

          <div style={{ gridColumn: "1 / span 2", marginTop: 10 }}>
            <button type="submit" style={{ padding: 8, cursor: "pointer" }}>
              Create client
            </button>
          </div>
        </form>

        {clientMsg && (
          <p style={{ color: "green", marginTop: 10 }}>{clientMsg}</p>
        )}
        {clientError && (
          <p style={{ color: "red", marginTop: 10 }}>{clientError}</p>
        )}

        {/* --------- Ajout compte --------- */}
        <h3 style={{ marginTop: 40 }}>Add bank account</h3>

        <form
          onSubmit={submitAccount}
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr 1fr",
            gap: 12,
            marginTop: 10,
          }}
        >
          <div>
            <label>Client ID</label>
            <input
              name="clientId"
              value={accountForm.clientId}
              onChange={handleAccountChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div>
            <label>RIB</label>
            <input
              name="rib"
              value={accountForm.rib}
              onChange={handleAccountChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div>
            <label>Initial Balance</label>
            <input
              name="balance"
              type="number"
              value={accountForm.balance}
              onChange={handleAccountChange}
              style={{ width: "100%", padding: 6 }}
            />
          </div>
          <div style={{ gridColumn: "1 / span 3", marginTop: 10 }}>
            <button type="submit" style={{ padding: 8, cursor: "pointer" }}>
              Create account
            </button>
          </div>
        </form>

        {accountMsg && (
          <p style={{ color: "green", marginTop: 10 }}>{accountMsg}</p>
        )}
        {accountError && (
          <p style={{ color: "red", marginTop: 10 }}>{accountError}</p>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;
