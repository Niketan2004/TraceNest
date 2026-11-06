import { lazy, Suspense, useEffect, useState, type ComponentProps } from "react";

const LeafletMap = lazy(() => import("./LeafletMap"));

export function ClientMap(props: ComponentProps<typeof LeafletMap>) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!mounted) {
    return (
      <div
        style={{ height: props.height ?? 480 }}
        className="flex w-full items-center justify-center rounded-2xl border border-border/60 bg-muted text-sm text-muted-foreground"
      >
        Loading map…
      </div>
    );
  }
  return (
    <Suspense
      fallback={
        <div style={{ height: props.height ?? 480 }} className="w-full rounded-2xl bg-muted" />
      }
    >
      <LeafletMap {...props} />
    </Suspense>
  );
}
