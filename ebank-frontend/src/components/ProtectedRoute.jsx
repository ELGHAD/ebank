import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const ProtectedRoute = ({ children, requiredRole }) => {
  const { auth } = useAuth();

  // non connecté
  if (!auth.isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // rôle imposé et différent
  if (requiredRole && auth.role !== requiredRole) {
    return (
      <p style={{ padding: 20 }}>You are not authorized to access this page.</p>
    );
  }

  return children;
};

export default ProtectedRoute;
