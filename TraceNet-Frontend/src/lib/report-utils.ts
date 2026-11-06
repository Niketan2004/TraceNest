import type { ReportStatus, ReportType } from "../services/types";

export const TYPE_LABEL: Record<ReportType, string> = {
  person: "Missing Person",
  pet: "Lost Pet",
  "lost-vehicle": "Lost Vehicle",
  "stolen-vehicle": "Stolen Vehicle",
};

export const STATUS_COLOR: Record<ReportStatus, string> = {
  missing: "bg-brand-blue text-white",
  found: "bg-brand-green text-white",
  resolved: "bg-brand-gold text-white",
};

export const STATUS_HEX: Record<ReportStatus, string> = {
  missing: "#2563eb",
  found: "#10b981",
  resolved: "#f59e0b",
};

export function timeAgo(iso: string) {
  const s = Math.floor((Date.now() - +new Date(iso)) / 1000);
  if (s < 60) return `${s}s ago`;
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ago`;
  return `${Math.floor(s / 86400)}d ago`;
}
