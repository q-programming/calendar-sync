import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
    dedupe: ["react", "react-dom"],
  },
  root: path.resolve(__dirname),
  build: {
    outDir: path.resolve(__dirname, "dist"),
    emptyOutDir: true,
  },
  server: {
    port: parseInt(process.env.PORT || "5173"),
    host: "0.0.0.0",
    proxy: {
      "/api": {
        target: process.env.API_URL || "http://localhost:3001",
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: parseInt(process.env.PORT || "4173"),
    host: "0.0.0.0",
  },
});
