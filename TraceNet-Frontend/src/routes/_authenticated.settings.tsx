import { createFileRoute } from "@tanstack/react-router";
import { Switch } from "../components/ui/switch";
import { Label } from "../components/ui/label";
import { Button } from "../components/ui/button";
import { useAuth } from "../lib/auth-context";
import { toast } from "sonner";

export const Route = createFileRoute("/_authenticated/settings")({
  head: () => ({ meta: [{ title: "Settings — TraceNest" }] }),
  component: SettingsPage,
});

function SettingsPage() {
  const { logout } = useAuth();
  const rows = [
    {
      key: "email",
      title: "Email notifications",
      desc: "Receive alerts for nearby reports and updates.",
      def: true,
    },
    {
      key: "sms",
      title: "SMS notifications",
      desc: "Coming soon — currently email-only in the MVP.",
      def: false,
    },
    { key: "push", title: "Browser push", desc: "Instant alerts inside your browser.", def: false },
    { key: "dark", title: "Dark mode", desc: "Follow system by default.", def: false },
  ];
  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
      <div className="mt-6 space-y-3">
        {rows.map((r) => (
          <div
            key={r.key}
            className="flex items-start justify-between gap-4 rounded-2xl border border-border/60 bg-card p-5 shadow-[var(--shadow-soft)]"
          >
            <div>
              <Label className="text-base font-semibold">{r.title}</Label>
              <p className="mt-1 text-sm text-muted-foreground">{r.desc}</p>
            </div>
            <Switch defaultChecked={r.def} />
          </div>
        ))}
      </div>

      <div className="mt-10 rounded-2xl border border-destructive/30 bg-destructive/5 p-6">
        <h2 className="text-lg font-semibold text-destructive">Danger Zone</h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Once you delete your account, there is no going back. This will permanently delete all
          your reports and notifications.
        </p>
        <Button
          variant="destructive"
          className="mt-4 rounded-xl"
          onClick={async () => {
            if (
              confirm(
                "Are you absolutely sure you want to delete your account? This action cannot be undone.",
              )
            ) {
              try {
                await import("../services/api").then((m) => m.api.delete("/users/me"));
                toast.success("Account deleted successfully.");
                logout();
              } catch (e) {
                toast.error("Failed to delete account.");
              }
            }
          }}
        >
          Delete Account
        </Button>
      </div>
    </div>
  );
}
