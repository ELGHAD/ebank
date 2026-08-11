import React, { createContext, useContext, useEffect, useState } from "react";

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [auth, setAuth] = useState({
    isAuthenticated: false,
    username: null,
    role: null,
    token: null,
  });

  // Chargement initial depuis localStorage
  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    const username = localStorage.getItem("username");
    const role = localStorage.getItem("role");

    if (token && username && role) {
      setAuth({
        isAuthenticated: true,
        username,
        role,
        token,
      });
    }
  }, []);

  const login = ({ token, username, role }) => {
    localStorage.setItem("accessToken", token);
    localStorage.setItem("username", username);
    localStorage.setItem("role", role);

    setAuth({
      isAuthenticated: true,
      username,
      role,
      token,
    });
  };

  const logout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("username");
    localStorage.removeItem("role");

    setAuth({
      isAuthenticated: false,
      username: null,
      role: null,
      token: null,
    });
  };

  return (
    <AuthContext.Provider value={{ auth, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
