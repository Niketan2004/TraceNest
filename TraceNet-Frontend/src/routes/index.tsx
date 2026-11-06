import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "framer-motion";
import { MapPin, Bell, Users, Search, Shield, Heart } from "lucide-react";
import { Button } from "../components/ui/button";
import homeLogo from "../assets/TraceNest.png";

export const Route = createFileRoute("/")({
  component: Landing,
});

const features = [
  {
    icon: Bell,
    title: "Instant Nearby Alerts",
    desc: "The moment a report is filed, people within your radius get notified — email in the MVP, SMS + push next.",
  },
  {
    icon: MapPin,
    title: "Location-first",
    desc: "Pin the exact last-seen spot. Browse a live map with color-coded pins for every open case.",
  },
  {
    icon: Users,
    title: "Community Powered",
    desc: "Every tip helps. Comments, sightings, and shares turn strangers into eyes on the ground.",
  },
  {
    icon: Shield,
    title: "Privacy by Design",
    desc: "You choose what to share, when to update status, and when to close a case.",
  },
];

const stats = [
  { value: "4", label: "Report categories" },
  { value: "10 km", label: "Default alert radius" },
  { value: "24/7", label: "Community watch" },
];

function Landing() {
  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-blue-50 via-background to-emerald-50" />
        <div className="absolute -left-32 top-20 -z-10 h-96 w-96 rounded-full bg-brand-blue/10 blur-3xl" />
        <div className="absolute -right-32 top-40 -z-10 h-96 w-96 rounded-full bg-brand-green/10 blur-3xl" />

        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-2 md:py-14">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
          >
            <span className="inline-flex items-center gap-2 rounded-full border border-border/60 bg-background/80 px-3 py-1 text-xs font-medium text-muted-foreground backdrop-blur">
              <span className="h-2 w-2 rounded-full bg-brand-green animate-pulse" />{" "}
              Community-driven Lost &amp; Found
            </span>
            <h1 className="mt-3 text-4xl font-extrabold leading-[1.05] tracking-tight sm:text-5xl md:text-6xl">
              Connecting Communities to{" "}
              <span className="bg-gradient-to-r from-brand-blue to-brand-green bg-clip-text text-transparent">
                Reunite What Matters
              </span>
            </h1>
            <p className="mt-3 max-w-lg text-lg text-muted-foreground">
              Report a missing person, lost pet, or stolen vehicle in seconds. TraceNest alerts
              nearby community members so no lead goes cold.
            </p>
            <div className="mt-5 flex flex-wrap gap-3">
              <Link to="/reports/new">
                <Button size="lg" className="rounded-xl">
                  Report now
                </Button>
              </Link>
              <Link to="/reports">
                <Button size="lg" variant="outline" className="rounded-xl">
                  Browse reports
                </Button>
              </Link>
              <Link to="/subscribe">
                <Button
                  size="lg"
                  variant="secondary"
                  className="rounded-xl bg-primary/10 text-primary hover:bg-primary/20"
                >
                  Get Alerts
                </Button>
              </Link>
            </div>

            <div className="mt-6 grid grid-cols-3 gap-6">
              {stats.map((s) => (
                <div key={s.label}>
                  <div className="text-2xl font-bold text-brand-blue">{s.value}</div>
                  <div className="text-xs text-muted-foreground">{s.label}</div>
                </div>
              ))}
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.6, delay: 0.15 }}
            className="relative flex items-center justify-center"
          >
            <div className="absolute inset-0 rounded-[2rem] bg-gradient-to-br from-brand-blue/20 via-transparent to-brand-green/20 blur-2xl" />
            <img
              src={homeLogo}
              alt="TraceNest"
              className="relative w-full max-w-md drop-shadow-2xl"
            />
          </motion.div>
        </div>
      </section>

      {/* Features */}
      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
            Everything you need to bring them home
          </h2>
          <p className="mt-3 text-muted-foreground">
            A single hub for reports, sightings, and community alerts.
          </p>
        </div>
        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((f, i) => (
            <motion.div
              key={f.title}
              initial={{ opacity: 0, y: 12 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: i * 0.06 }}
              className="rounded-2xl border border-border/60 bg-card p-6 shadow-[var(--shadow-soft)] transition hover:-translate-y-1 hover:shadow-[var(--shadow-elevated)]"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-blue/10 text-brand-blue">
                <f.icon className="h-5 w-5" />
              </div>
              <h3 className="mt-4 font-semibold">{f.title}</h3>
              <p className="mt-1.5 text-sm text-muted-foreground">{f.desc}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section className="bg-muted/40 py-12">
        <div className="mx-auto max-w-7xl px-4 sm:px-6">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">How TraceNest works</h2>
          </div>
          <div className="mt-12 grid gap-6 md:grid-cols-3">
            {[
              {
                icon: Search,
                step: "01",
                title: "File a report",
                desc: "Add photos, details, and pin the last-seen location on the map.",
              },
              {
                icon: Bell,
                step: "02",
                title: "Alert the neighborhood",
                desc: "Nearby members are notified instantly so eyes are on the ground fast.",
              },
              {
                icon: Heart,
                step: "03",
                title: "Reunite & resolve",
                desc: "Update the status as leads come in and mark the case resolved when they're home.",
              },
            ].map((s, i) => (
              <motion.div
                key={s.step}
                initial={{ opacity: 0, y: 12 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.4, delay: i * 0.08 }}
                className="rounded-2xl border border-border/60 bg-card p-6 shadow-[var(--shadow-soft)]"
              >
                <div className="flex items-center gap-3">
                  <span className="text-3xl font-extrabold text-brand-blue/30">{s.step}</span>
                  <s.icon className="h-5 w-5 text-brand-green" />
                </div>
                <h3 className="mt-3 font-semibold">{s.title}</h3>
                <p
                  className="mt-1.5 text-sm text-muted-foreground"
                  dangerouslySetInnerHTML={{ __html: s.desc }}
                />
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-blue to-brand-green p-10 text-white md:p-14">
          <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/10 blur-2xl" />
          <div className="relative max-w-2xl">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Every second counts. Start a report now.
            </h2>
            <p className="mt-3 text-white/90">
              Join a network of neighbors, volunteers, and responders working together.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Link to="/register">
                <Button size="lg" variant="secondary" className="rounded-xl">
                  Create free account
                </Button>
              </Link>
              <Link to="/reports">
                <Button
                  size="lg"
                  variant="outline"
                  className="rounded-xl border-white/40 bg-white/10 text-white hover:bg-white/20"
                >
                  Explore feed
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
