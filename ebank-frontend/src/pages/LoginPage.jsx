import React, { useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import api from "../api/axiosClient";
import { errorMessage } from "../api/errors";
import { useAuth } from "../context/AuthContext";
import "../styles/auth.css";

const HOME_BY_ROLE = [
  ["PLATFORM_ADMIN", "/platform"],
  ["ORG_ADMIN", "/org"],
  ["AGENT", "/admin"],
  ["CLIENT", "/client"],
];

export const homeFor = (user) =>
  user.passwordChangeRequired
    ? "/change-password"
    : (HOME_BY_ROLE.find(([role]) => user.roles.includes(role))?.[1] ?? "/login");

const LoginPage = () => {
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPwd, setShowPwd] = useState(false);

  const { auth, login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!form.username || !form.password) {
      setError("Please fill in all fields.");
      return;
    }

    try {
      setLoading(true);
      // The session token comes back as an HttpOnly cookie; the body only describes the user
      const res = await api.post("/api/auth/login", form);
      login(res.data.user);
      navigate(homeFor(res.data.user));
    } catch (err) {
      setError(errorMessage(err, "Login failed. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  if (auth.isAuthenticated) {
    return <Navigate to={homeFor(auth.user)} replace />;
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <div className="brand">
          <div className="logo">eB</div>
          <div>
            <h1>eBank</h1>
            <p>Banking simulation</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="login-username">Email</label>
            <div className="input-box">
              <input
                id="login-username"
                name="username"
                type="email"
                autoComplete="username"
                value={form.username}
                onChange={handleChange}
                placeholder="you@example.com"
              />
            </div>
          </div>

          <div className="field">
            <label htmlFor="login-password">Password</label>
            <div className="input-box">
              <input
                id="login-password"
                name="password"
                type={showPwd ? "text" : "password"}
                autoComplete="current-password"
                value={form.password}
                onChange={handleChange}
                placeholder="Enter password"
              />
              <button
                type="button"
                aria-label={showPwd ? "Hide password" : "Show password"}
                onClick={() => setShowPwd(!showPwd)}
              >
                {showPwd ? "Hide" : "Show"}
              </button>
            </div>
          </div>

          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}

          <button type="submit" className="primary-btn" disabled={loading}>
            {loading ? "Signing in..." : "Login"}
          </button>
        </form>

        <div className="footer">Banking simulation · synthetic data only</div>
      </div>
    </main>
  );
};

export default LoginPage;
