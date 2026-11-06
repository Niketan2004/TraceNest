import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { User, PawPrint, Bike, ShieldAlert } from "lucide-react";
import type { ReportType } from "../services/types";

export const Route = createFileRoute("/_authenticated/reports/new/")({
  head: () => ({ meta: [{ title: "New report — TraceNest" }] }),
  component: NewReportPicker,
});

const categories: {
  type: ReportType;
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  desc: string;
  color: string;
}[] = [
  {
    type: "person",
    icon: User,
    title: "Missing person",
    desc: "Report a missing adult or child.",
    color: "from-brand-blue to-blue-400",
  },
  {
    type: "pet",
    icon: PawPrint,
    title: "Lost pet",
    desc: "Cats, dogs, birds — any beloved companion.",
    color: "from-brand-green to-emerald-400",
  },
  {
    type: "lost-vehicle",
    icon: Bike,
    title: "Lost vehicle",
    desc: "Bike, car, scooter, or other lost vehicle.",
    color: "from-brand-gold to-amber-400",
  },
  {
    type: "stolen-vehicle",
    icon: ShieldAlert,
    title: "Stolen vehicle",
    desc: "Include FIR details for faster action.",
    color: "from-red-500 to-rose-400",
  },
];

function NewReportPicker() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight">What are you reporting?</h1>
      <p className="mt-2 text-muted-foreground">
        Choose a category to continue. You can add photos and pin the location on the next step.
      </p>

      <div className="mt-8 grid gap-5 sm:grid-cols-2">
        {categories.map((c, i) => (
          <motion.div
            key={c.type}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.05 }}
          >
            <Link
              to="/reports/new/$type"
              params={{ type: c.type }}
              className="group block overflow-hidden rounded-2xl border border-border/60 bg-card p-6 shadow-[var(--shadow-soft)] transition hover:-translate-y-1 hover:shadow-[var(--shadow-elevated)]"
            >
              <div
                className={`flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br ${c.color} text-white`}
              >
                <c.icon className="h-6 w-6" />
              </div>
              <h2 className="mt-4 text-lg font-semibold">{c.title}</h2>
              <p className="mt-1 text-sm text-muted-foreground">{c.desc}</p>
              <span className="mt-4 inline-block text-sm font-medium text-primary group-hover:underline">
                Start report →
              </span>
            </Link>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
