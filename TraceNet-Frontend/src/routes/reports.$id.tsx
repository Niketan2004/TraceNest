/**
 * @file reports.$id.tsx
 * @description Case dossier details route displaying complete incident information:
 * high-resolution photo gallery, map coordinates, attribute details, reporter contact button,
 * status modification (for owners), and one-click printable PDF search flyer download.
 */

import { createFileRoute, notFound, Link, useRouter } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getReport, updateStatus, getPosterUrl } from "../services/reports.service";
import type { ReportStatus } from "../services/types";
import { ClientMap } from "../components/maps/ClientMap";
import { STATUS_COLOR, TYPE_LABEL, timeAgo } from "../lib/report-utils";
import { Button } from "../components/ui/button";
import { ArrowLeft, MapPin, Phone, Clock, Gift, Shield, FileText, Edit } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "../lib/auth-context";
import { toastApiError } from "../lib/api-error";
import { ErrorState } from "../components/ui/error-state";

export const Route = createFileRoute("/reports/$id")({
  loader: async ({ params, context }) => {
    try {
      const r = await context.queryClient.ensureQueryData({
        queryKey: ["report", params.id],
        queryFn: () => getReport(params.id),
      });
      if (!r) throw notFound();
      return r;
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 404) {
        throw notFound();
      }
      throw err;
    }
  },
  head: ({ loaderData }) => ({
    meta: loaderData
      ? [
          { title: `${loaderData.title} — TraceNest` },
          { name: "description", content: loaderData.description.slice(0, 155) },
          { property: "og:title", content: loaderData.title },
          { property: "og:description", content: loaderData.description.slice(0, 155) },
          ...(loaderData.images[0]
            ? [{ property: "og:image", content: loaderData.images[0] }]
            : []),
        ]
      : [{ title: "Report — TraceNest" }],
  }),
  errorComponent: ({ error, reset }) => {
    const router = useRouter();
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <ErrorState
          error={error}
          title="Could not load report"
          message="We couldn't retrieve this report's information. The service may be temporarily unavailable."
          onRetry={() => {
            router.invalidate();
            reset();
          }}
          homeLink="/reports"
        />
      </div>
    );
  },
  notFoundComponent: () => (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="text-2xl font-bold">Report not found</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        The report you are looking for may have been deleted or the link is incorrect.
      </p>
      <Link to="/reports" className="mt-6 inline-block rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90">
        Back to reports
      </Link>
    </div>
  ),
  component: ReportDetail,
});

function ReportDetail() {
  const report = Route.useLoaderData() as import("../services/types").Report;
  const { id } = Route.useParams();
  const qc = useQueryClient();
  const router = useRouter();
  const { user } = useAuth();

  const mutation = useMutation({
    mutationFn: (status: ReportStatus) => updateStatus(id, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["report", id] });
      qc.invalidateQueries({ queryKey: ["reports"] });
      qc.invalidateQueries({ queryKey: ["stats"] });
      toast.success("Status updated");
      router.invalidate();
    },
    onError: (err) => {
      toastApiError(err, "Failed to update status");
    },
  });

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <Link
        to="/reports"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> All reports
      </Link>

      <div className="mt-4 grid gap-8 lg:grid-cols-[1fr_360px]">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={`rounded-full px-2.5 py-1 text-xs font-semibold uppercase ${STATUS_COLOR[report.status]}`}
            >
              {report.status}
            </span>
            <span className="rounded-full bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">
              {TYPE_LABEL[report.type]}
            </span>
          </div>
          <h1 className="mt-3 text-3xl font-bold tracking-tight">{report.title}</h1>
          <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-1">
              <MapPin className="h-4 w-4" />
              {report.city}
              {report.state ? `, ${report.state}` : ""}
            </span>
            <span className="inline-flex items-center gap-1">
              <Clock className="h-4 w-4" />
              {timeAgo(report.createdAt)}
            </span>
            {typeof report.reward === "number" && report.reward > 0 ? (
              <span className="inline-flex items-center gap-1 font-medium text-brand-gold">
                <Gift className="h-4 w-4" />₹{report.reward.toLocaleString("en-IN")} reward
              </span>
            ) : null}
          </div>

          {report.images.length > 0 && (
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              {report.images.map((src, i) => (
                <img
                  key={i}
                  src={src}
                  alt=""
                  className="aspect-[4/3] w-full rounded-2xl object-cover"
                />
              ))}
            </div>
          )}

          <div className="mt-8 rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)]">
            <h2 className="font-semibold">Description</h2>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
              {report.description}
            </p>
          </div>

          <div className="mt-6 rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)]">
            <h2 className="font-semibold">Details</h2>
            <dl className="mt-3 grid gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
              {report.age !== undefined && <Row k="Age" v={String(report.age)} />}
              {report.gender && <Row k="Gender" v={report.gender} />}
              {report.clothing && <Row k="Clothing" v={report.clothing} />}
              {report.species && <Row k="Species" v={report.species} />}
              {report.breed && <Row k="Breed" v={report.breed} />}
              {report.color && <Row k="Color" v={report.color} />}
              {report.vehicleType && <Row k="Vehicle type" v={report.vehicleType} />}
              {report.vehicleNumber && <Row k="Number plate" v={report.vehicleNumber} />}
              {report.manufacturer && <Row k="Manufacturer" v={report.manufacturer} />}
              {report.model && <Row k="Model" v={report.model} />}
              {report.year && <Row k="Year" v={String(report.year)} />}
              {report.policeComplaint && <Row k="Police complaint" v={report.policeComplaint} />}
              {report.firNumber && <Row k="FIR number" v={report.firNumber} />}
              {report.policeStation && <Row k="Police station" v={report.policeStation} />}
              {report.lastSeen && (
                <Row k="Last seen" v={new Date(report.lastSeen).toLocaleString()} />
              )}
            </dl>
          </div>

          <div className="mt-6">
            <h2 className="font-semibold">Last seen location</h2>
            <div className="mt-3">
              <ClientMap
                reports={[report]}
                center={[report.lat, report.lng]}
                zoom={13}
                height={360}
              />
            </div>
          </div>
        </div>

        <aside className="space-y-4">
          <div className="rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)]">
            <h3 className="font-semibold">Contact reporter</h3>
            <a
              href={`tel:${report.contact}`}
              className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:opacity-90"
            >
              <Phone className="h-4 w-4" /> {report.contact}
            </a>
            <a
              href={`https://www.google.com/maps/dir/?api=1&destination=${report.lat},${report.lng}`}
              target="_blank"
              rel="noreferrer"
              className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium hover:bg-muted"
            >
              <MapPin className="h-4 w-4" /> Get directions
            </a>
          </div>

          {/* Actions & Tools */}
          <div className="rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)] space-y-2">
            <h3 className="font-semibold text-sm">Case Tools</h3>
            <a
              href={getPosterUrl(report.id)}
              target="_blank"
              rel="noopener noreferrer"
              className="flex w-full items-center justify-center gap-2 rounded-xl border border-brand-blue/30 bg-brand-blue/10 px-4 py-2.5 text-sm font-medium text-brand-blue hover:bg-brand-blue/20 transition"
            >
              <FileText className="h-4 w-4" /> Download Printable Poster (PDF)
            </a>

            {user && (
              <Link
                to="/reports/$id/edit"
                params={{ id: report.id }}
                className="flex w-full items-center justify-center gap-2 rounded-xl border border-border bg-background px-4 py-2.5 text-sm font-medium hover:bg-muted transition"
              >
                <Edit className="h-4 w-4" /> Edit Report Details
              </Link>
            )}
          </div>

          {user && (
            <div className="rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)]">
              <h3 className="font-semibold">Update status</h3>
              <p className="mt-1 text-xs text-muted-foreground">
                Owners &amp; verified users can change the case status.
              </p>
              <div className="mt-3 grid grid-cols-3 gap-2">
                {(["missing", "found", "resolved"] as ReportStatus[]).map((s) => (
                  <Button
                    key={s}
                    variant={report.status === s ? "default" : "outline"}
                    size="sm"
                    className="rounded-lg capitalize"
                    onClick={() => mutation.mutate(s)}
                    disabled={mutation.isPending}
                  >
                    {s}
                  </Button>
                ))}
              </div>
            </div>
          )}

          <div className="rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)]">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <Shield className="h-4 w-4 text-brand-green" />
              Safety tip
            </div>
            <p className="mt-2 text-xs text-muted-foreground">
              Meet in public places and involve local authorities when returning found items or
              verifying tips.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}

function Row({ k, v }: { k: string; v: string }) {
  return (
    <>
      <dt className="text-muted-foreground">{k}</dt>
      <dd className="font-medium">{v}</dd>
    </>
  );
}
