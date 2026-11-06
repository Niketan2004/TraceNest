import { createFileRoute } from "@tanstack/react-router";
import { useAuth } from "../lib/auth-context";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { toast } from "sonner";

export const Route = createFileRoute("/_authenticated/profile")({
  head: () => ({ meta: [{ title: "Profile — TraceNest" }] }),
  component: ProfilePage,
});

function ProfilePage() {
  const { user } = useAuth();
  if (!user) return null;
  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="text-3xl font-bold tracking-tight">Profile</h1>
      <div className="mt-6 rounded-2xl border border-border/60 bg-card p-6 shadow-[var(--shadow-soft)]">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary text-2xl font-semibold text-primary-foreground">
            {user.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <div className="text-lg font-semibold">{user.name}</div>
            <div className="text-sm text-muted-foreground">{user.email}</div>
          </div>
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            toast.success("Profile updated");
          }}
          className="mt-6 grid gap-4 sm:grid-cols-2"
        >
          <div>
            <Label>Name</Label>
            <Input defaultValue={user.name} />
          </div>
          <div>
            <Label>Phone</Label>
            <Input defaultValue={user.phone ?? ""} placeholder="+91 …" />
          </div>
          <div>
            <Label>City</Label>
            <Input defaultValue={user.city ?? ""} />
          </div>
          <div>
            <Label>State</Label>
            <Input placeholder="State" />
          </div>
          <div className="sm:col-span-2 flex justify-end">
            <Button type="submit" className="rounded-xl">
              Save changes
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
