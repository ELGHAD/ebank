import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

import LoginPage from "./pages/LoginPage";
import AdminDashboard from "./pages/AdminDashboard";
import ClientDashboard from "./pages/ClientDashboard";
import NewTransfer from "./pages/NewTransfer";
import AccountHistory from "./pages/AccountHistory";

import ProtectedRoute from "./components/ProtectedRoute";

const App = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/login" element={<LoginPage />} />

          {/* Admin */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRole="AGENT">
                <AdminDashboard />
              </ProtectedRoute>
            }
          />

          {/* Client */}
          <Route
            path="/client"
            element={
              <ProtectedRoute requiredRole="CLIENT">
                <ClientDashboard />
              </ProtectedRoute>
            }
          />

          {/* Client - Transfer */}
          <Route
            path="/client/transfer"
            element={
              <ProtectedRoute requiredRole="CLIENT">
                <NewTransfer />
              </ProtectedRoute>
            }
          />

          {/* Client - Account history */}
          <Route
            path="/client/accounts/:accountId"
            element={
              <ProtectedRoute requiredRole="CLIENT">
                <AccountHistory />
              </ProtectedRoute>
            }
          />

          {/* Default */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
