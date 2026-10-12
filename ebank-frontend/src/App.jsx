import React from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";

import AppShell from "./components/AppShell";
import ProtectedRoute from "./components/ProtectedRoute";
import LoginPage from "./pages/LoginPage";
import ChangePasswordPage from "./pages/ChangePasswordPage";
import ClientDashboard from "./pages/ClientDashboard";
import NewTransfer from "./pages/NewTransfer";
import AccountHistory from "./pages/AccountHistory";
import ClientsPage from "./pages/agent/ClientsPage";
import OnboardClientPage from "./pages/agent/OnboardClientPage";
import ClientDetailPage from "./pages/agent/ClientDetailPage";
import AuditLogPage from "./pages/agent/AuditLogPage";

const App = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/login" element={<LoginPage />} />

          {/* Any signed-in user (also reachable while a password change is required) */}
          <Route
            path="/change-password"
            element={
              <ProtectedRoute>
                <ChangePasswordPage />
              </ProtectedRoute>
            }
          />

          {/* Agent workspace */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requiredRole="AGENT">
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route index element={<ClientsPage />} />
            <Route path="clients/new" element={<OnboardClientPage />} />
            <Route path="clients/:clientId" element={<ClientDetailPage />} />
            <Route path="accounts/:accountId" element={<AccountHistory role="agent" />} />
            <Route path="audit" element={<AuditLogPage />} />
          </Route>

          {/* Client space */}
          <Route
            path="/client"
            element={
              <ProtectedRoute requiredRole="CLIENT">
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route index element={<ClientDashboard />} />
            <Route path="transfer" element={<NewTransfer />} />
            <Route path="accounts/:accountId" element={<AccountHistory role="client" />} />
          </Route>

          {/* Default */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
