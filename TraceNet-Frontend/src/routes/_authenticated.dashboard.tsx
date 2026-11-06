import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { getStats, listReports } from "../services/reports.service";
import { useAuth } from "../lib/auth-context";
import { Button } from "../components/ui/button";
import { ReportCard } from "../components/reports/ReportCard";
import { Bell, MapPin, Plus, TrendingUp } from "lucide-react";
import { ErrorState } from "../components/ui/error-state";
import { Skeleton } from "../components/ui/skeleton";

export const Route = createFileRoute("/_authenticated/dashboard")({
  head: () => ({ meta: [{ title: "Dashboard — TraceNest" }] }),
  component: Dashboard,
});

function Dashboard() {
  const { user } = useAuth();
  const { data: stats } = useQuery({ queryKey: ["stats"], queryFn: getStats });
  const {
    data: recent = [],
    isLoading: recentLoading,
    isError: recentError,
    error: recentErr,
    refetch: refetchRecent,
  } = useQuery({
    queryKey: ["recent"],
    queryFn: () => listReports({ sort: "newest" }),
  });

  const tiles = [
    {
      label: "Total reports",
      value: stats?.total ?? "—",
      icon: TrendingUp,
      color: "text-brand-blue bg-brand-blue/10",
    },
    {
      label: "Active",
      value: stats?.active ?? "—",
      icon: Bell,
      color: "text-brand-blue bg-brand-blue/10",
    },
    {
      label: "Found",
      value: stats?.found ?? "—",
      icon: MapPin,
      color: "text-brand-green bg-brand-green/10",
    },
    {
      label: "Resolved",
      value: stats?.resolved ?? "—",
      icon: TrendingUp,
      color: "text-brand-gold bg-brand-gold/10",
    },
  ];

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm text-muted-foreground">Welcome back</p>
          <h1 className="text-3xl font-bold tracking-tight">{user?.name}</h1>
        </div>
        <Link to="/reports/new">
          <Button className="rounded-xl">
            <Plus className="mr-1 h-4 w-4" />
            New report
          </Button>
        </Link>
      </div>

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {tiles.map((t, i) => (
          <motion.div
            key={t.label}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
            className="rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)]"
          >
            <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${t.color}`}>
              <t.icon className="h-5 w-5" />
            </div>
            <div className="mt-4 text-3xl font-bold">{t.value}</div>
            <div className="text-xs text-muted-foreground">{t.label}</div>
          </motion.div>
        ))}
      </div>

      <div className="mt-10 flex items-center justify-between">
        <h2 className="text-xl font-bold">Recent reports</h2>
        <Link to="/reports" className="text-sm font-medium text-primary hover:underline">
          View all →
        </Link>
      </div>
      <div className="mt-4">
        {recentLoading ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-80 rounded-2xl" />
            ))}
          </div>
        ) : recentError ? (
          <div className="py-6">
            <ErrorState
              error={recentErr}
              title="Could not load recent reports"
              message="Failed to retrieve recent community reports."
              onRetry={() => refetchRecent()}
            />
          </div>
        ) : recent.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border/80 p-8 text-center text-muted-foreground">
            No recent reports found.
          </div>
        ) : (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {recent.slice(0, 6).map((r, i) => (
              <ReportCard key={r.id} report={r} index={i} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
