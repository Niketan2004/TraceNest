/**
 * @file _authenticated.my-reports.tsx
 * @description Case management dashboard for authenticated case authors.
 * Allows users to inspect their filed reports, toggle status (Missing -> Found -> Resolved),
 * edit report details, download official PDF posters, or delete resolved cases.
 */

import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  getMyReports,
  updateStatus,
  deleteReport,
  getPosterUrl,
} from "../services/reports.service";
import type { Report, ReportStatus } from "../services/types";
import { STATUS_COLOR, TYPE_LABEL, timeAgo } from "../lib/report-utils";
import { Button } from "../components/ui/button";
import {
  Plus,
  FileText,
  Edit,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ExternalLink,
} from "lucide-react";
import { toast } from "sonner";
import { toastApiError } from "../lib/api-error";
import { ErrorState } from "../components/ui/error-state";

export const Route = createFileRoute("/_authenticated/my-reports")({
  head: () => ({ meta: [{ title: "My Reports — TraceNest" }] }),
  component: MyReportsPage,
});

function MyReportsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<"all" | ReportStatus>("all");

  const {
    data: reports = [],
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["my-reports"],
    queryFn: () => getMyReports(),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: ReportStatus }) => updateStatus(id, status),
    onSuccess: () => {
      toast.success("Report status updated");
      queryClient.invalidateQueries({ queryKey: ["my-reports"] });
      queryClient.invalidateQueries({ queryKey: ["stats"] });
    },
    onError: (e) => toastApiError(e, "Failed to update status"),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteReport(id),
    onSuccess: () => {
      toast.success("Report deleted successfully");
      queryClient.invalidateQueries({ queryKey: ["my-reports"] });
      queryClient.invalidateQueries({ queryKey: ["stats"] });
    },
    onError: (e) => toastApiError(e, "Failed to delete report"),
  });

  const filtered = reports.filter((r) => {
    if (filter === "all") return true;
    return r.status === filter;
  });

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">My Reports</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage your reported cases, update status, generate posters, or edit details.
          </p>
        </div>
        <Link to="/reports/new">
          <Button className="rounded-xl gap-1.5">
            <Plus className="h-4 w-4" />
            New report
          </Button>
        </Link>
      </div>

      {/* Filter Tabs */}
      <div className="mt-6 flex gap-2 border-b border-border/60 pb-3">
        {(["all", "missing", "found", "resolved"] as const).map((s) => (
          <button
            key={s}
            onClick={() => setFilter(s)}
            className={`rounded-xl px-4 py-1.5 text-sm font-medium transition ${
              filter === s
                ? "bg-primary text-primary-foreground shadow-sm"
                : "text-muted-foreground hover:bg-muted"
            }`}
          >
            {s.charAt(0).toUpperCase() + s.slice(1)}
            <span className="ml-1.5 text-xs opacity-75">
              ({s === "all" ? reports.length : reports.filter((r) => r.status === s).length})
            </span>
          </button>
        ))}
      </div>

      {/* Reports List */}
      <div className="mt-6">
        {isLoading ? (
          <div className="flex items-center justify-center py-20 text-muted-foreground">
            <Loader2 className="mr-2 h-6 w-6 animate-spin" />
            Loading your reports...
          </div>
        ) : isError ? (
          <div className="py-8">
            <ErrorState
              error={error}
              title="Could not load your reports"
              message="We encountered an issue fetching your reports. Please try again."
              onRetry={() => refetch()}
            />
          </div>
        ) : filtered.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border/80 p-12 text-center">
            <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-muted text-muted-foreground">
              <AlertCircle className="h-6 w-6" />
            </div>
            <h3 className="font-semibold">No reports found</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              {filter === "all"
                ? "You haven't filed any reports yet."
                : `No reports with status '${filter}'.`}
            </p>
            {filter === "all" && (
              <Link to="/reports/new" className="mt-4 inline-block">
                <Button className="rounded-xl">Create your first report</Button>
              </Link>
            )}
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((report) => (
              <div
                key={report.id}
                className="flex flex-col justify-between rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)] transition hover:shadow-md"
              >
                <div>
                  <div className="flex items-center justify-between gap-2">
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${STATUS_COLOR[report.status]}`}
                    >
                      {report.status.toUpperCase()}
                    </span>
                    <span className="text-xs text-muted-foreground">{TYPE_LABEL[report.type]}</span>
                  </div>

                  {report.images && report.images[0] && (
                    <div className="mt-3 aspect-video w-full overflow-hidden rounded-xl bg-muted">
                      <img src={report.images[0]} alt="" className="h-full w-full object-cover" />
                    </div>
                  )}

                  <h3 className="mt-3 font-semibold line-clamp-1">{report.title}</h3>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {report.city || "Unknown city"} · Filed {timeAgo(report.createdAt)}
                  </p>
                  <p className="mt-2 text-sm text-muted-foreground line-clamp-2">
                    {report.description}
                  </p>
                </div>

                <div className="mt-5 border-t border-border/50 pt-4">
                  {/* Status Toggle Buttons */}
                  <div className="flex items-center gap-1.5 text-xs">
                    <span className="text-muted-foreground mr-1">Status:</span>
                    {(["missing", "found", "resolved"] as const).map((st) => (
                      <button
                        key={st}
                        disabled={report.status === st || statusMutation.isPending}
                        onClick={() => statusMutation.mutate({ id: report.id, status: st })}
                        className={`rounded-lg px-2 py-0.5 font-medium transition ${
                          report.status === st
                            ? "bg-muted font-bold text-foreground cursor-default"
                            : "text-muted-foreground hover:bg-muted/70 hover:text-foreground"
                        }`}
                      >
                        {st.charAt(0).toUpperCase() + st.slice(1)}
                      </button>
                    ))}
                  </div>

                  {/* Action Links */}
                  <div className="mt-3 flex items-center justify-between gap-2 pt-2 border-t border-border/30">
                    <Link
                      to="/reports/$id"
                      params={{ id: report.id }}
                      className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                    >
                      <ExternalLink className="h-3.5 w-3.5" />
                      View
                    </Link>

                    <Link
                      to="/reports/$id/edit"
                      params={{ id: report.id }}
                      className="inline-flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-foreground"
                    >
                      <Edit className="h-3.5 w-3.5" />
                      Edit
                    </Link>

                    <a
                      href={getPosterUrl(report.id)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-xs font-medium text-brand-blue hover:underline"
                    >
                      <FileText className="h-3.5 w-3.5" />
                      Poster
                    </a>

                    <button
                      onClick={() => {
                        if (
                          confirm(
                            "Are you sure you want to delete this report? This action cannot be undone.",
                          )
                        ) {
                          deleteMutation.mutate(report.id);
                        }
                      }}
                      className="inline-flex items-center gap-1 text-xs font-medium text-destructive hover:underline"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
