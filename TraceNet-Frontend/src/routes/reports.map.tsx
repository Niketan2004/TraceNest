import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { listReports } from "../services/reports.service";
import type { Report } from "../services/types";
import { ClientMap } from "../components/maps/ClientMap";
import { STATUS_COLOR, TYPE_LABEL, timeAgo } from "../lib/report-utils";
import { ErrorState } from "../components/ui/error-state";

export const Route = createFileRoute("/reports/map")({
  head: () => ({ meta: [{ title: "Map view — TraceNest" }] }),
  component: MapView,
});

function MapView() {
  const {
    data = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({ queryKey: ["reports", "all"], queryFn: () => listReports({}) });
  const [selected, setSelected] = useState<Report | null>(null);

  if (isError) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6">
        <ErrorState
          error={error}
          title="Could not load map reports"
          message="Unable to load reports for the map view. Please check your connection."
          onRetry={() => refetch()}
          homeLink="/reports"
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight">Reports near you</h1>
      <p className="mt-1 text-muted-foreground">Blue = missing · Green = found · Gold = resolved</p>

      <div className="mt-6 grid gap-4 lg:grid-cols-[1fr_360px]">
        <ClientMap reports={data} height={620} onSelect={(r) => setSelected(r)} />
        <div className="max-h-[620px] overflow-y-auto rounded-2xl border border-border/60 bg-card p-4 shadow-[var(--shadow-soft)]">
          {selected ? (
            <Link to="/reports/$id" params={{ id: selected.id }} className="block">
              {selected.images[0] && (
                <img
                  src={selected.images[0]}
                  alt=""
                  className="mb-3 h-40 w-full rounded-xl object-cover"
                />
              )}
              <span
                className={`inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${STATUS_COLOR[selected.status]}`}
              >
                {selected.status.toUpperCase()}
              </span>
              <h3 className="mt-2 font-semibold">{selected.title}</h3>
              <p className="mt-1 text-xs text-muted-foreground">
                {TYPE_LABEL[selected.type]} · {selected.city} · {timeAgo(selected.createdAt)}
              </p>
              <p className="mt-2 text-sm text-muted-foreground line-clamp-4">
                {selected.description}
              </p>
              <span className="mt-3 inline-block text-sm font-medium text-primary">
                Open report →
              </span>
            </Link>
          ) : (
            <div className="py-8 text-center text-sm text-muted-foreground">
              Click a pin to see report details.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
