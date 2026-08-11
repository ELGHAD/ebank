import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const Navbar = () => {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <nav
      style={{
        display: "flex",
        justifyContent: "space-between",
        padding: "10px 20px",
        borderBottom: "1px solid #ddd",
        marginBottom: 20,
        backgroundColor: "#fafafa",
      }}
    >
      <div>
        <strong>eBank</strong>
        {auth.role === "AGENT_GUICHET" && (
          <span style={{ marginLeft: 15 }}>
            <Link to="/admin">Admin</Link>
          </span>
        )}
        {auth.role === "CLIENT" && (
          <span style={{ marginLeft: 15 }}>
            <Link to="/client">My accounts</Link>
          </span>
        )}
      </div>

      <div>
        {auth.isAuthenticated ? (
          <>
            <span style={{ marginRight: 10 }}>
              Logged in as <b>{auth.username}</b> ({auth.role})
            </span>
            <button onClick={handleLogout}>Logout</button>
          </>
        ) : (
          <Link to="/login">Login</Link>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
