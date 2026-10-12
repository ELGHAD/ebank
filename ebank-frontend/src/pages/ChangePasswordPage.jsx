import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosClient";
import { errorMessage, fieldErrors } from "../api/errors";
import { useAuth } from "../context/AuthContext";
import { homeFor } from "./LoginPage";
import "../styles/auth.css";

const MIN_LENGTH = 12;

const ChangePasswordPage = () => {
  const { auth, login, logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [errors, setErrors] = useState({});
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    const found = {};
    if (!form.currentPassword) found.currentPassword = "Enter your current password.";
    if (form.newPassword.length < MIN_LENGTH) found.newPassword = `Use at least ${MIN_LENGTH} characters.`;
    if (form.newPassword !== form.confirmPassword) found.confirmPassword = "The passwords do not match.";
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    try {
      setSaving(true);
      const res = await api.post("/api/auth/password", {
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });
      login(res.data.user); // the backend issued a fresh session cookie; older sessions are revoked
      navigate(homeFor(res.data.user), { replace: true });
    } catch (err) {
      setErrors(fieldErrors(err));
      setError(errorMessage(err, "The password could not be changed."));
    } finally {
      setSaving(false);
    }
  };

  const input = (name, label, autoComplete) => (
    <div className="field">
      <label htmlFor={`pwd-${name}`}>{label}</label>
      <div className="input-box">
        <input
          id={`pwd-${name}`}
          name={name}
          type="password"
          autoComplete={autoComplete}
          value={form[name]}
          onChange={handleChange}
          aria-invalid={Boolean(errors[name])}
          aria-describedby={errors[name] ? `pwd-${name}-error` : undefined}
        />
      </div>
      {errors[name] && (
        <div id={`pwd-${name}-error`} className="fieldError">
          {errors[name]}
        </div>
      )}
    </div>
  );

  return (
    <main className="auth-page">
      <div className="auth-card">
        <h1>Choose your password</h1>
        <p className="intro">
          {auth.passwordChangeRequired
            ? "You signed in with a temporary password. Choose your own to continue."
            : "Changing your password signs you out on your other devices."}
        </p>

        <form onSubmit={handleSubmit} noValidate>
          {input("currentPassword", auth.passwordChangeRequired ? "Temporary password" : "Current password", "current-password")}
          {input("newPassword", `New password (at least ${MIN_LENGTH} characters)`, "new-password")}
          {input("confirmPassword", "Confirm new password", "new-password")}

          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}

          <button type="submit" className="primary-btn" disabled={saving}>
            {saving ? "Saving..." : "Save password"}
          </button>
        </form>

        <div className="footer">
          <button type="button" className="primary-btn" onClick={logout}>
            Log out
          </button>
        </div>
      </div>
    </main>
  );
};

export default ChangePasswordPage;
