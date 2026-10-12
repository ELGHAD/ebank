import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * UI convenience only: the backend enforces every permission independently.
 */
const ProtectedRoute = ({ children, requiredRole }) => {
  const { auth } = useAuth();

  if (!auth.isAuthenticated) {
    return <Navigate to="/login" replace />;
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
