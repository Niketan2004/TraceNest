import { createFileRoute, Outlet, redirect } from "@tanstack/react-router";
import { useAuth } from "../lib/auth-context";
import { useEffect } from "react";
import { useRouter } from "@tanstack/react-router";

export const Route = createFileRoute("/_authenticated")({
  component: AuthGate,
});

function AuthGate() {
  const { user, loading } = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (!loading && !user) router.navigate({ to: "/login" });
  }, [loading, user, router]);

  if (loading)
    return <div className="p-10 text-center text-sm text-muted-foreground">Loading…</div>;
  if (!user) return null;
  return <Outlet />;
}
