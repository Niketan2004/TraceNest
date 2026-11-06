import React from "react";
import { AlertCircle, X } from "lucide-react";
import { extractApiError, type ApiErrorInfo } from "../../lib/api-error";

interface ErrorBannerProps {
  error: unknown | string | null | undefined;
  title?: string;
  onDismiss?: () => void;
  className?: string;
}

export function ErrorBanner({ error, title, onDismiss, className = "" }: ErrorBannerProps) {
  if (!error) return null;

  const errorInfo: ApiErrorInfo =
    typeof error === "string"
      ? {
          title: "Error",
          message: error,
          isNetworkError: false,
          isAuthError: false,
        }
      : extractApiError(error);

  const displayTitle = title || errorInfo.title;
  const fieldErrorEntries = errorInfo.fieldErrors
    ? Object.entries(errorInfo.fieldErrors)
    : [];

  return (
    <div
      role="alert"
      className={`relative rounded-xl border border-destructive/40 bg-destructive/10 p-4 text-destructive dark:bg-destructive/15 dark:text-red-300 animate-in fade-in duration-200 ${className}`}
    >
      <div className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-destructive dark:text-red-400" />
        <div className="flex-1 text-sm">
          {displayTitle && (
            <h4 className="font-semibold text-destructive dark:text-red-300 leading-tight">
              {displayTitle}
            </h4>
          )}
          <p className={`${displayTitle ? "mt-1" : ""} text-xs md:text-sm leading-relaxed text-destructive/90 dark:text-red-200/90`}>
            {errorInfo.message}
          </p>

          {fieldErrorEntries.length > 0 && (
            <ul className="mt-2.5 list-disc list-inside space-y-1 text-xs text-destructive/90 dark:text-red-200/90 bg-destructive/5 rounded-lg p-2.5">
              {fieldErrorEntries.map(([field, msg]) => (
                <li key={field}>
                  <strong className="font-medium capitalize">{field}:</strong> {msg}
                </li>
              ))}
            </ul>
          )}

          {errorInfo.traceId && (
            <div className="mt-2 text-[10px] font-mono text-destructive/70 dark:text-red-300/60">
              Trace ID: {errorInfo.traceId}
            </div>
          )}
        </div>

        {onDismiss && (
          <button
            type="button"
            onClick={onDismiss}
            className="-mr-1 -mt-1 rounded-lg p-1.5 text-destructive/70 hover:bg-destructive/15 hover:text-destructive dark:text-red-300/70 dark:hover:text-red-200 transition"
            aria-label="Dismiss error"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
}
