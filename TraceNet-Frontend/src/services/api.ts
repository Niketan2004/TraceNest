/**
 * @file api.ts
 * @description Centralized Axios HTTP client instance configured for isomorphic execution
 * (Node.js SSR and Browser client-side). Includes automatic Bearer JWT injection,
 * CORS-free reverse-proxy routing, and automatic 401 session clearing.
 */

import axios from "axios";

/**
 * Resolves the appropriate base URL based on the runtime environment:
 * - Server-side (SSR / Nitro): Uses direct inter-container bridge URL (http://backend:8080/api/v1).
 * - Browser (Client): Uses relative path "/api/v1" proxied through the Vite dev server to bypass CORS.
 */
const isServer = typeof window === "undefined";
const API_BASE_URL = isServer
  ? (process.env.INTERNAL_API_URL || "http://backend:8080/api/v1")
  : "/api/v1";

/**
 * Pre-configured Axios instance for all TraceNest backend interactions.
 */
export const api = axios.create({
  baseURL: API_BASE_URL,
});

/**
 * Request interceptor: Inspects localStorage in browser environments and
 * automatically injects the active Bearer authentication token into the HTTP Authorization header.
 */
api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    try {
      const token = localStorage.getItem("tracenest.token");
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch {
      // Local storage inaccessible (e.g. strict sandbox / private browsing mode)
    }
  }
  return config;
});

/**
 * Response interceptor: Traps 401 Unauthorized responses and purges stale
 * authentication tokens from localStorage, gracefully terminating invalid sessions.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      try {
        localStorage.removeItem("tracenest.token");
      } catch {
        // Ignore localStorage write failures in restricted contexts
      }
    }
    return Promise.reject(error);
  }
);
