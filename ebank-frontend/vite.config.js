import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    // Same-origin in development too: the browser calls /api on the Vite server,
    // which forwards to the Spring Boot backend. No CORS configuration needed.
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.js"],
    restoreMocks: true,
    testTimeout: 20000,
  },
});
