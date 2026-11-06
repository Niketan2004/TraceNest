import axios from "axios";
import { toast } from "sonner";

export interface BackendErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  code?: string;
  message?: string;
  fieldErrors?: Record<string, string>;
  path?: string;
  traceId?: string;
}

export interface ApiErrorInfo {
  title: string;
  message: string;
  code?: string;
  status?: number;
  fieldErrors?: Record<string, string>;
  path?: string;
  traceId?: string;
  isNetworkError: boolean;
  isAuthError: boolean;
  rawError?: unknown;
}

/**
 * Parses any error into a rich, structured ApiErrorInfo with user-friendly messages.
 */
export function extractApiError(error: unknown, fallbackMessage = "An unexpected error occurred."): ApiErrorInfo {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const data = error.response?.data as BackendErrorResponse | undefined;

    // 1. Network / Server Unreachable Error
    if (!error.response) {
      if (error.code === "ECONNABORTED" || error.message.includes("timeout")) {
        return {
          title: "Connection Timeout",
          message: "The server took too long to respond. Please check your connection and try again.",
          code: "TIMEOUT",
          isNetworkError: true,
          isAuthError: false,
          rawError: error,
        };
      }
      return {
        title: "Network Connection Error",
        message: "Unable to connect to the TraceNest server. Please ensure the backend is running and your network is connected.",
        code: "NETWORK_ERROR",
        isNetworkError: true,
        isAuthError: false,
        rawError: error,
      };
    }

    // 2. Server responded with an error status
    const backendMessage = data?.message;
    const backendCode = data?.code;
    const fieldErrors = data?.fieldErrors;
    const traceId = data?.traceId;
    const path = data?.path;

    let title = "Request Failed";
    let message = backendMessage || fallbackMessage;

    switch (status) {
      case 400:
        title = "Validation Error";
        if (fieldErrors && Object.keys(fieldErrors).length > 0) {
          const formattedList = Object.entries(fieldErrors)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join(", ");
          message = backendMessage ? `${backendMessage} (${formattedList})` : formattedList;
        } else if (!backendMessage) {
          message = "The request contained invalid data. Please check the form and try again.";
        }
        break;

      case 401:
        title = "Authentication Required";
        if (!backendMessage) {
          message = "Your session has expired or you are not signed in. Please log in again.";
        }
        break;

      case 403:
        title = "Access Denied";
        if (!backendMessage) {
          message = "You do not have permission to view or modify this resource.";
        }
        break;

      case 404:
        title = "Resource Not Found";
        if (!backendMessage) {
          message = "The requested report, user, or page could not be found.";
        }
        break;

      case 409:
        title = "Conflict";
        if (backendCode === "EMAIL_TAKEN") {
          title = "Email Already Registered";
          message = backendMessage || "An account with this email address already exists. Please sign in or use another email.";
        } else if (!backendMessage) {
          message = "This record conflicts with existing data in the system.";
        }
        break;

      case 413:
        title = "File Too Large";
        message = backendMessage || "The uploaded file exceeds the 5MB size limit. Please upload a smaller file.";
        break;

      case 415:
        title = "Unsupported Format";
        message = backendMessage || "The uploaded file format is not supported. Please use JPG, PNG, WEBP, or GIF.";
        break;

      case 429:
        title = "Too Many Requests";
        message = "You have made too many requests in a short time. Please wait a moment and try again.";
        break;

      case 500:
      case 502:
      case 503:
      case 504:
        title = "Server Error";
        message = backendMessage || "A server error occurred while processing your request. Please try again shortly.";
        break;
    }

    return {
      title,
      message,
      code: backendCode,
      status,
      fieldErrors,
      path,
      traceId,
      isNetworkError: false,
      isAuthError: status === 401,
      rawError: error,
    };
  }

  // Standard Error instance
  if (error instanceof Error) {
    return {
      title: "Error",
      message: error.message || fallbackMessage,
      isNetworkError: false,
      isAuthError: false,
      rawError: error,
    };
  }

  // String error
  if (typeof error === "string") {
    return {
      title: "Error",
      message: error,
      isNetworkError: false,
      isAuthError: false,
      rawError: error,
    };
  }

  return {
    title: "Unexpected Error",
    message: fallbackMessage,
    isNetworkError: false,
    isAuthError: false,
    rawError: error,
  };
}

/**
 * Extracts a concise error message string from any error object.
 */
export function getErrorMessage(error: unknown, fallback?: string): string {
  return extractApiError(error, fallback).message;
}

/**
 * Triggers a Sonner error toast with title and detailed description, and returns the message.
 */
export function toastApiError(error: unknown, fallback?: string): string {
  const info = extractApiError(error, fallback);
  toast.error(info.title, {
    description: info.message,
    duration: 5000,
  });
  return info.message;
}
