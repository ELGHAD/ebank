import React from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "../styles/app.css";

const NAV = {
  AGENT: [
    { to: "/admin", label: "Clients", end: true },
    { to: "/admin/clients/new", label: "Onboard client" },
    { to: "/admin/audit", label: "Audit log" },
  ],
  CLIENT: [
    { to: "/client", label: "My accounts", end: true },
    { to: "/client/transfer", label: "New transfer" },
  ],
};

/** Layout of every signed-in page: skip link, header with role-based navigation, main landmark, footer. */
export default function AppShell() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="app">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="app-header">
        <Link className="brand" to={auth.role === "AGENT" ? "/admin" : "/client"}>
          <span className="brand-logo" aria-hidden="true">
            eB
          </span>
          eBank
        </Link>
        <nav className="main-nav" aria-label="Main">
          <ul>
            {(NAV[auth.role] ?? []).map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.end}>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="user-menu">
          <span>
            {auth.username} ({auth.role === "AGENT" ? "Agent" : "Client"})
          </span>
          <Link className="btn btnSmall" to="/change-password">
            Change password
          </Link>
          <button type="button" className="btn btnSmall" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>
      <main id="main" className="app-main">
        <Outlet />
      </main>
      <footer className="app-footer">
        Banking simulation for demonstration purposes · synthetic data only · no real money
      </footer>
    </div>
  );
}
