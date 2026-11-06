/**
 * @file ReportCard.tsx
 * @description Reusable incident card showcasing primary photograph, status badge,
 * category pill, location metadata, reward callout, and smooth hover/entry animations.
 */

import { Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { MapPin, Clock, Gift } from "lucide-react";
import type { Report } from "../../services/types";
import { STATUS_COLOR, TYPE_LABEL, timeAgo } from "../../lib/report-utils";

export function ReportCard({ report, index = 0 }: { report: Report; index?: number }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay: Math.min(index * 0.04, 0.3) }}
    >
      <Link
        to="/reports/$id"
        params={{ id: report.id }}
        className="group block overflow-hidden rounded-2xl border border-border/60 bg-card shadow-[var(--shadow-soft)] transition hover:-translate-y-0.5 hover:shadow-[var(--shadow-elevated)]"
      >
        <div className="relative aspect-[4/3] overflow-hidden bg-muted">
          {report.images[0] ? (
            <img
              src={report.images[0]}
              alt={report.title}
              className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
              loading="lazy"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-muted-foreground">
              No image
            </div>
          )}
          <span
            className={`absolute left-3 top-3 rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wide ${STATUS_COLOR[report.status]}`}
          >
            {report.status}
          </span>
          <span className="absolute right-3 top-3 rounded-full bg-black/60 px-2.5 py-1 text-xs font-medium text-white backdrop-blur-sm">
            {TYPE_LABEL[report.type]}
          </span>
        </div>
        <div className="p-4">
          <h3 className="line-clamp-1 font-semibold">{report.title}</h3>
          <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{report.description}</p>
          <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
            <span className="inline-flex items-center gap-1">
              <MapPin className="h-3.5 w-3.5" />
              {report.city}
            </span>
            <span className="inline-flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" />
              {timeAgo(report.createdAt)}
            </span>
            {typeof report.reward === "number" && report.reward > 0 ? (
              <span className="inline-flex items-center gap-1 font-medium text-brand-gold">
                <Gift className="h-3.5 w-3.5" />₹{report.reward.toLocaleString("en-IN")}
              </span>
            ) : null}
          </div>
        </div>
      </Link>
    </motion.div>
  );
}
