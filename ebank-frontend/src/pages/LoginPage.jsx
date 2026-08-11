import React, { useState } from "react";
import axiosClient from "../api/axiosClient";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

const LoginPage = () => {
  const [form, setForm] = useState({ username: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [showPwd, setShowPwd] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!form.username || !form.password) {
      setError("Please fill in all fields.");
      return;
    }

    try {
      setLoading(true);
      const res = await axiosClient.post("/api/auth/login", form);

      const { accessToken, username, role } = res.data;
      login({ token: accessToken, username, role });

      navigate(role === "AGENT_GUICHET" ? "/admin" : "/client");
    } catch {
      setError("Invalid username or password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <style>{`
        * { box-sizing: border-box; }

        body {
          margin: 0;
          font-family: 'Segoe UI', system-ui, sans-serif;
        }

        .login-page {
          min-height: 100vh;
          display: flex;
          justify-content: center;
          align-items: center;
          background: linear-gradient(135deg, #0f172a, #020617);
        }

        .login-card {
          width: 400px;
          padding: 28px;
          border-radius: 18px;
          background: rgba(255,255,255,0.06);
          backdrop-filter: blur(12px);
          border: 1px solid rgba(255,255,255,0.15);
          box-shadow: 0 20px 60px rgba(0,0,0,0.6);
          color: #e5e7eb;
        }

        .brand {
          display: flex;
          align-items: center;
          gap: 14px;
          margin-bottom: 24px;
        }

        .logo {
          width: 48px;
          height: 48px;
          border-radius: 14px;
          background: linear-gradient(135deg, #22c55e, #16a34a);
          color: #052e16;
          display: flex;
          justify-content: center;
          align-items: center;
          font-weight: 900;
          font-size: 18px;
        }

        .brand h1 {
          margin: 0;
          font-size: 22px;
        }

        .brand p {
          margin: 2px 0 0;
          font-size: 13px;
          color: #9ca3af;
        }

        .field {
          margin-bottom: 14px;
        }

        .field label {
          font-size: 13px;
          color: #9ca3af;
        }

        .input-box {
          margin-top: 6px;
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px;
          border-radius: 12px;
          background: rgba(0,0,0,0.25);
          border: 1px solid rgba(255,255,255,0.18);
        }

        .input-box input {
          width: 100%;
          background: transparent;
          border: none;
          outline: none;
          color: #fff;
          font-size: 14px;
        }

        .input-box button {
          background: none;
          border: none;
          color: #9ca3af;
          cursor: pointer;
          font-size: 12px;
        }

        .input-box:focus-within {
          border-color: #22c55e;
          box-shadow: 0 0 0 3px rgba(34,197,94,0.25);
        }

        .error {
          margin: 10px 0;
          padding: 10px;
          background: rgba(239,68,68,0.15);
          border: 1px solid rgba(239,68,68,0.4);
          border-radius: 10px;
          font-size: 13px;
          color: #fecaca;
          text-align: center;
        }

        .login-btn {
          width: 100%;
          padding: 12px;
          margin-top: 10px;
          border-radius: 14px;
          border: none;
          background: linear-gradient(135deg, #22c55e, #16a34a);
          color: #052e16;
          font-weight: 700;
          cursor: pointer;
          font-size: 15px;
        }

        .login-btn:hover {
          filter: brightness(1.05);
        }

        .login-btn:disabled {
          opacity: 0.7;
          cursor: not-allowed;
        }

        .footer {
          margin-top: 14px;
          text-align: center;
          font-size: 12px;
          color: #6b7280;
        }
      `}</style>

      <div className="login-page">
        <div className="login-card">
          <div className="brand">
            <div className="logo">eB</div>
            <div>
              <h1>eBank</h1>
              <p>Secure banking access</p>
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="field">
              <label>Username</label>
              <div className="input-box">
                <input
                  name="username"
                  value={form.username}
                  onChange={handleChange}
                  placeholder="Enter username"
                />
              </div>
            </div>

            <div className="field">
              <label>Password</label>
              <div className="input-box">
                <input
                  type={showPwd ? "text" : "password"}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Enter password"
                />
                <button type="button" onClick={() => setShowPwd(!showPwd)}>
                  {showPwd ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            {error && <div className="error">{error}</div>}

            <button className="login-btn" disabled={loading}>
              {loading ? "Signing in..." : "Login"}
            </button>
          </form>

          <div className="footer">© 2025 eBank — Secure & Trusted</div>
        </div>
      </div>
    </>
  );
};

export default LoginPage;
