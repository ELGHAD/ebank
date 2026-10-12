import React from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * UI convenience only: the backend enforces every permission independently.
 */
const ProtectedRoute = ({ children, requiredRole }) => {
  const { auth } = useAuth();
  const location = useLocation();

  if (auth.status === "loading") {
    return (
      <p role="status" style={{ padding: 20 }}>
        Loading…
      </p>
    );
  }

  if (!auth.isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (auth.passwordChangeRequired && location.pathname !== "/change-password") {
    return <Navigate to="/change-password" replace />;
  }

  if (requiredRole && !auth.roles.includes(requiredRole)) {
    return (
      <p role="alert" style={{ padding: 20 }}>
        You are not authorized to access this page.
      </p>
    );
  }

  return children;
};

export default ProtectedRoute;
