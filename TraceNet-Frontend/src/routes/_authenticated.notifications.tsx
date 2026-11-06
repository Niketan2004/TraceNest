import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Bell, MapPin, Sparkles, CheckCheck, Loader2 } from "lucide-react";
import {
  getNotifications,
  markAsRead,
  markAllAsRead,
  type AppNotification,
} from "../services/notifications.service";
import { timeAgo } from "../lib/report-utils";
import { Button } from "../components/ui/button";
import { toast } from "sonner";
import { toastApiError } from "../lib/api-error";
import { ErrorState } from "../components/ui/error-state";

export const Route = createFileRoute("/_authenticated/notifications")({
  head: () => ({ meta: [{ title: "Notifications — TraceNest" }] }),
  component: NotificationsPage,
});

function getIcon(type: string) {
  if (type.includes("REPORT") || type.includes("NEARBY")) return MapPin;
  if (type.includes("STATUS") || type.includes("RESOLVED") || type.includes("FOUND"))
    return Sparkles;
  return Bell;
}

function NotificationsPage() {
  const queryClient = useQueryClient();

  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["notifications"],
    queryFn: () => getNotifications(false, 0, 50),
  });

  const readMutation = useMutation({
    mutationFn: markAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["notifications"] }),
    onError: (e) => toastApiError(e, "Failed to update notification"),
  });

  const readAllMutation = useMutation({
    mutationFn: markAllAsRead,
    onSuccess: () => {
      toast.success("All notifications marked as read");
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
    onError: (e) => toastApiError(e, "Failed to mark all notifications as read"),
  });

  const items: AppNotification[] = data?.items || [];
  const unreadCount = data?.unreadCount || 0;

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Notifications</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {unreadCount > 0
              ? `${unreadCount} unread alert${unreadCount > 1 ? "s" : ""}`
              : "You're all caught up"}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            className="rounded-xl gap-1.5"
            onClick={() => readAllMutation.mutate()}
            disabled={readAllMutation.isPending}
          >
            <CheckCheck className="h-4 w-4" />
            Mark all read
          </Button>
        )}
      </div>

      <div className="mt-6 divide-y divide-border/60 overflow-hidden rounded-2xl border border-border/60 bg-card shadow-[var(--shadow-soft)]">
        {isLoading ? (
          <div className="flex items-center justify-center py-16 text-muted-foreground">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            Loading notifications...
          </div>
        ) : isError ? (
          <div className="p-8">
            <ErrorState
              error={error}
              title="Could not load notifications"
              message="Failed to retrieve notifications. Please check your connection."
              onRetry={() => refetch()}
            />
          </div>
        ) : items.length === 0 ? (
          <div className="py-16 text-center">
            <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-muted text-muted-foreground">
              <Bell className="h-6 w-6" />
            </div>
            <h3 className="font-semibold">No notifications yet</h3>
            <p className="mt-1 text-sm text-muted-foreground">
              When cases are reported near your area, you'll see alerts here.
            </p>
          </div>
        ) : (
          items.map((n) => {
            const Icon = getIcon(n.type);
            const content = (
              <div
                className={`flex items-start gap-3 p-4 transition hover:bg-muted/50 cursor-pointer ${
                  !n.read ? "bg-primary/5" : ""
                }`}
                onClick={() => {
                  if (!n.read) readMutation.mutate(n.id);
                }}
              >
                <div
                  className={`mt-0.5 flex h-9 w-9 flex-none items-center justify-center rounded-xl ${
                    !n.read ? "bg-brand-blue/15 text-brand-blue" : "bg-muted text-muted-foreground"
                  }`}
                >
                  <Icon className="h-4 w-4" />
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-foreground">{n.title}</span>
                    {!n.read && <span className="h-2 w-2 rounded-full bg-brand-blue flex-none" />}
                  </div>
                  <div className="text-sm text-muted-foreground mt-0.5">{n.body}</div>
                </div>
                <div className="text-xs text-muted-foreground flex-none">
                  {n.createdAt ? timeAgo(n.createdAt) : ""}
                </div>
              </div>
            );

            if (n.reportId) {
              return (
                <Link key={n.id} to="/reports/$id" params={{ id: n.reportId }} className="block">
                  {content}
                </Link>
              );
            }

            return <div key={n.id}>{content}</div>;
          })
        )}
      </div>
    </div>
  );
}
