/**
 * @file auth-context.tsx
 * @description React Authentication Context and Provider managing user session hydration,
 * login, registration, and logout states across client-rendered and SSR routes.
 */

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api } from "../services/api";

/**
 * Logged-in user profile model.
 */
export type User = {
  id: string;
  name: string;
  email: string;
  phone?: string;
  city?: string;
  role: "USER" | "ADMIN";
  avatarUrl?: string;
};

/**
 * Shape of the authentication context state and dispatch functions.
 */
type AuthCtx = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
};

const Ctx = createContext<AuthCtx | null>(null);

/**
 * Global authentication provider wrapping the component hierarchy.
 * Automatically checks localStorage for existing tokens on mount and validates
 * them with `GET /api/v1/auth/me`.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUser = async () => {
      if (typeof window === "undefined") {
        setLoading(false);
        return;
      }
      try {
        const token = localStorage.getItem("tracenest.token");
        if (token) {
          const res = await api.get("/auth/me");
          setUser(res.data);
        }
      } catch {
        try {
          localStorage.removeItem("tracenest.token");
        } catch {
          // ignore
        }
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    fetchUser();
  }, []);

  /**
   * Authenticates user with credentials, stores JWT access token in localStorage,
   * and populates user state.
   */
  const login = async (email: string, password: string) => {
    const res = await api.post("/auth/login", { email, password });
    if (typeof window !== "undefined") {
      try {
        localStorage.setItem("tracenest.token", res.data.accessToken);
      } catch {
        // ignore storage errors
      }
    }
    setUser(res.data.user);
  };

  /**
   * Registers a new account, stores the issued token, and populates user state.
   */
  const register = async (name: string, email: string, password: string) => {
    const res = await api.post("/auth/register", { name, email, password });
    if (typeof window !== "undefined") {
      try {
        localStorage.setItem("tracenest.token", res.data.accessToken);
      } catch {
        // ignore
      }
    }
    setUser(res.data.user);
  };

  /**
   * Dispatches logout API notification, removes token from localStorage, and clears state.
   */
  const logout = async () => {
    try {
      await api.post("/auth/logout");
    } catch {
      // ignore network errors on logout
    }
    if (typeof window !== "undefined") {
      try {
        localStorage.removeItem("tracenest.token");
      } catch {
        // ignore
      }
    }
    setUser(null);
  };

  return <Ctx.Provider value={{ user, loading, login, register, logout }}>{children}</Ctx.Provider>;
}

/**
 * Custom React hook to access authentication context.
 *
 * @throws Error if called outside of an {@link AuthProvider}.
 */
export function useAuth() {
  const c = useContext(Ctx);
  if (!c) throw new Error("useAuth outside AuthProvider");
  return c;
}
