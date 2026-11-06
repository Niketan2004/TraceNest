import React, { useState } from "react";
import { AlertTriangle, RefreshCw, Home, ChevronDown, ChevronUp, WifiOff } from "lucide-react";
import { Button } from "./button";
import { extractApiError, type ApiErrorInfo } from "../../lib/api-error";

interface ErrorStateProps {
  error: unknown;
  title?: string;
  message?: string;
  onRetry?: () => void | Promise<unknown>;
  homeLink?: string;
  className?: string;
}

export function ErrorState({
  error,
  title,
  message,
  onRetry,
  homeLink = "/",
  className = "",
}: ErrorStateProps) {
  const [retrying, setRetrying] = useState(false);
  const [showDetails, setShowDetails] = useState(false);

  const errorInfo: ApiErrorInfo = extractApiError(error);
  const displayTitle = title || errorInfo.title;
  const displayMessage = message || errorInfo.message;

  const handleRetry = async () => {
    if (!onRetry) return;
    setRetrying(true);
    try {
      await onRetry();
    } finally {
      setRetrying(false);
    }
  };

  const IconComponent = errorInfo.isNetworkError ? WifiOff : AlertTriangle;

  return (
    <div
      className={`mx-auto flex max-w-lg flex-col items-center justify-center rounded-2xl border border-destructive/30 bg-card p-8 text-center shadow-[var(--shadow-soft)] ${className}`}
      role="alert"
    >
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-destructive/10 text-destructive shadow-sm">
        <IconComponent className="h-8 w-8 animate-pulse" />
      </div>

      <h3 className="mt-4 text-xl font-bold tracking-tight text-foreground">{displayTitle}</h3>
      <p className="mt-2 text-sm text-muted-foreground">{displayMessage}</p>

      {/* Field Errors if available */}
      {errorInfo.fieldErrors && Object.keys(errorInfo.fieldErrors).length > 0 && (
        <div className="mt-4 w-full rounded-xl bg-destructive/5 p-3 text-left text-xs text-destructive">
          <p className="font-semibold mb-1">Please correct the following:</p>
          <ul className="list-inside list-disc space-y-0.5">
            {Object.entries(errorInfo.fieldErrors).map(([field, msg]) => (
              <li key={field}>
                <span className="font-medium capitalize">{field}</span>: {msg}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Technical Diagnostics Accordion */}
      {(errorInfo.status || errorInfo.code || errorInfo.traceId) && (
        <div className="mt-4 w-full">
          <button
            type="button"
            onClick={() => setShowDetails(!showDetails)}
            className="flex items-center justify-center gap-1 mx-auto text-xs font-medium text-muted-foreground hover:text-foreground transition"
          >
            <span>{showDetails ? "Hide technical details" : "Show technical details"}</span>
            {showDetails ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>

          {showDetails && (
            <div className="mt-2 rounded-xl bg-muted/60 p-3 text-left text-xs font-mono text-muted-foreground space-y-1">
              {errorInfo.status && (
                <div>
                  <span className="text-foreground font-semibold">Status Code:</span> {errorInfo.status}
                </div>
              )}
              {errorInfo.code && (
                <div>
                  <span className="text-foreground font-semibold">Error Code:</span> {errorInfo.code}
                </div>
              )}
              {errorInfo.traceId && (
                <div>
                  <span className="text-foreground font-semibold">Trace ID:</span> {errorInfo.traceId}
                </div>
              )}
              {errorInfo.path && (
                <div>
                  <span className="text-foreground font-semibold">Endpoint:</span> {errorInfo.path}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
        {onRetry && (
          <Button
            onClick={handleRetry}
            disabled={retrying}
            className="rounded-xl gap-2 font-medium"
          >
            <RefreshCw className={`h-4 w-4 ${retrying ? "animate-spin" : ""}`} />
            {retrying ? "Retrying…" : "Try again"}
          </Button>
        )}
        {homeLink && (
          <a
            href={homeLink}
            className="inline-flex items-center gap-1.5 rounded-xl border border-input bg-background px-4 py-2 text-sm font-medium text-foreground hover:bg-accent hover:text-accent-foreground transition"
          >
            <Home className="h-4 w-4" />
            Go to home
          </a>
        )}
      </div>
    </div>
  );
}
