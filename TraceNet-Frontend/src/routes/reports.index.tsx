import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Search } from "lucide-react";
import { listReports } from "../services/reports.service";
import type { ReportStatus, ReportType } from "../services/types";
import { ReportCard } from "../components/reports/ReportCard";
import { Input } from "../components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { Skeleton } from "../components/ui/skeleton";
import { ErrorState } from "../components/ui/error-state";

export const Route = createFileRoute("/reports/")({
  head: () => ({
    meta: [
      { title: "Browse reports — TraceNest" },
      {
        name: "description",
        content: "Search missing persons, lost pets, and vehicles reported near you.",
      },
    ],
  }),
  component: ReportsFeed,
});

function ReportsFeed() {
  const [q, setQ] = useState("");
  const [type, setType] = useState<ReportType | "all">("all");
  const [status, setStatus] = useState<ReportStatus | "all">("all");
  const [sort, setSort] = useState<"newest" | "reward">("newest");

  const {
    data = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["reports", { q, type, status, sort }],
    queryFn: () => listReports({ q, type, status, sort }),
  });

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Community reports</h1>
          <p className="mt-1 text-muted-foreground">Every tip helps someone find what matters.</p>
        </div>
      </div>

      <div className="mt-6 grid gap-3 rounded-2xl border border-border/60 bg-card p-4 shadow-[var(--shadow-soft)] md:grid-cols-[1fr_auto_auto_auto]">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Search name, plate, location, breed…"
            className="pl-9"
          />
        </div>
        <Select value={type} onValueChange={(v) => setType(v as ReportType | "all")}>
          <SelectTrigger className="w-full md:w-[160px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All categories</SelectItem>
            <SelectItem value="person">Missing person</SelectItem>
            <SelectItem value="pet">Lost pet</SelectItem>
            <SelectItem value="lost-vehicle">Lost vehicle</SelectItem>
            <SelectItem value="stolen-vehicle">Stolen vehicle</SelectItem>
          </SelectContent>
        </Select>
        <Select value={status} onValueChange={(v) => setStatus(v as ReportStatus | "all")}>
          <SelectTrigger className="w-full md:w-[140px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Any status</SelectItem>
            <SelectItem value="missing">Missing</SelectItem>
            <SelectItem value="found">Found</SelectItem>
            <SelectItem value="resolved">Resolved</SelectItem>
          </SelectContent>
        </Select>
        <Select value={sort} onValueChange={(v) => setSort(v as "newest" | "reward")}>
          <SelectTrigger className="w-full md:w-[140px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="newest">Newest</SelectItem>
            <SelectItem value="reward">Highest reward</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading ? (
          Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-80 rounded-2xl" />)
        ) : isError ? (
          <div className="col-span-full py-8">
            <ErrorState
              error={error}
              title="Could not load reports"
              message="We encountered an issue fetching the latest reports. Please check your connection and try again."
              onRetry={() => refetch()}
            />
          </div>
        ) : data.length === 0 ? (
          <div className="col-span-full py-16 text-center text-muted-foreground">
            No reports match your filters.
          </div>
        ) : (
          data.map((r, i) => <ReportCard key={r.id} report={r} index={i} />)
        )}
      </div>
    </div>
  );
}
