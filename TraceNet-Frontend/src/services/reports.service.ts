/**
 * @file reports.service.ts
 * @description Frontend API client service for incident report lifecycle operations:
 * querying reports with search/filters, creating cases with multipart image uploads,
 * updating status, retrieving user's authored reports, downloading posters, and fetching statistics.
 */

import { api } from "./api";
import type { Report, ReportFilters, CreateReportInput } from "./types";

/**
 * Shape of the ReportDto returned from the Spring Boot backend REST endpoints.
 */
type BackendReport = {
  id: string;
  type: Report["type"];
  title: string;
  description: string;
  status: Report["status"];
  reward?: number;
  contact: string;
  images?: string[];
  createdAt: string;
  lastSeen?: string;
  location?: {
    lat: number;
    lng: number;
    address?: string;
    city?: string;
    state?: string;
    postcode?: string;
    country?: string;
    source?: "gps" | "manual";
  };
  details?: {
    age?: number;
    gender?: string;
    clothing?: string;
    species?: string;
    breed?: string;
    color?: string;
    vehicleType?: string;
    vehicleNumber?: string;
    manufacturer?: string;
    model?: string;
    year?: number;
    firNumber?: string;
    policeStation?: string;
    policeComplaint?: string;
  };
};

/**
 * Normalizes a nested backend `ReportDto` into the flat `Report` model used across UI views.
 *
 * @param r Raw backend report payload.
 * @returns Flattened, UI-ready {@link Report} object.
 */
function mapToFrontend(r: BackendReport): Report {
  return {
    id: r.id,
    type: r.type,
    title: r.title,
    description: r.description,
    status: r.status,
    reward: typeof r.reward === "number" && r.reward > 0 ? r.reward : undefined,
    contact: r.contact,
    images: r.images || [],
    createdAt: r.createdAt,
    lastSeen: r.lastSeen,
    lat: r.location?.lat ?? 0,
    lng: r.location?.lng ?? 0,
    address: r.location?.address,
    city: r.location?.city ?? "",
    state: r.location?.state,
    postcode: r.location?.postcode,
    country: r.location?.country,
    locationSource: r.location?.source,
    age: r.details?.age,
    gender: r.details?.gender,
    clothing: r.details?.clothing,
    species: r.details?.species,
    breed: r.details?.breed,
    color: r.details?.color,
    vehicleType: r.details?.vehicleType,
    vehicleNumber: r.details?.vehicleNumber,
    manufacturer: r.details?.manufacturer,
    model: r.details?.model,
    year: r.details?.year,
    firNumber: r.details?.firNumber,
    policeStation: r.details?.policeStation,
    policeComplaint: r.details?.policeComplaint,
  };
}

/**
 * Fetches reports matching optional search parameters, category filters, and sorting criteria.
 *
 * @param filters Query filter parameters (q, type, status, city, sort).
 * @returns Promise resolving to an array of normalized {@link Report} objects.
 */
export async function listReports(filters: ReportFilters = {}): Promise<Report[]> {
  const { q, type, status, city, sort } = filters;
  const params = new URLSearchParams();
  if (q) params.append("q", q);
  if (type && type !== "all") params.append("type", type);
  if (status && status !== "all") params.append("status", status);
  if (city) params.append("city", city);
  if (sort) params.append("sort", sort);

  const res = await api.get(`/reports?${params.toString()}`);
  const items = res.data.items || [];
  return items.map(mapToFrontend);
}

/**
 * Retrieves full details for a single report by its ID.
 *
 * @param id Unique report identifier.
 * @returns Promise resolving to the {@link Report}.
 */
export async function getReport(id: string): Promise<Report> {
  const res = await api.get(`/reports/${id}`);
  return mapToFrontend(res.data);
}

/**
 * Submits a new emergency incident report with up to 5 multipart image files.
 *
 * @param input Form input containing type, attributes, location coordinates, and file attachments.
 * @returns Promise resolving to the newly created {@link Report}.
 */
export async function createReport(input: CreateReportInput): Promise<Report> {
  const payload = {
    type: input.type,
    title: input.title,
    description: input.description,
    contact: input.contact,
    reward: input.reward,
    lastSeen: input.lastSeen,
    location: {
      lat: input.lat,
      lng: input.lng,
      address: input.address,
      city: input.city,
      state: input.state,
      postcode: input.postcode,
      country: input.country,
      source: input.locationSource,
    },
    details: {
      age: input.age,
      gender: input.gender,
      clothing: input.clothing,
      species: input.species,
      breed: input.breed,
      color: input.color,
      vehicleType: input.vehicleType,
      vehicleNumber: input.vehicleNumber,
      manufacturer: input.manufacturer,
      model: input.model,
      year: input.year,
      firNumber: input.firNumber,
      policeStation: input.policeStation,
      policeComplaint: input.policeComplaint,
    },
  };

  const formData = new FormData();
  formData.append("request", new Blob([JSON.stringify(payload)], { type: "application/json" }));

  if (input.images && input.images.length > 0) {
    for (const file of input.images as unknown as File[]) {
      formData.append("images", file);
    }
  }

  const res = await api.post("/reports", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return mapToFrontend(res.data);
}

/**
 * Updates editable fields of an existing report owned by the authenticated user.
 *
 * @param id Report unique ID.
 * @param input Partial report modifications.
 * @returns Promise resolving to the updated {@link Report}.
 */
export async function updateReport(id: string, input: Partial<Report>): Promise<Report> {
  const payload: Record<string, unknown> = {
    title: input.title,
    description: input.description,
    contact: input.contact,
    reward: input.reward,
    lastSeen: input.lastSeen,
  };
  if (input.lat !== undefined || input.lng !== undefined || input.city !== undefined) {
    payload.location = {
      lat: input.lat,
      lng: input.lng,
      address: input.address,
      city: input.city,
      state: input.state,
      postcode: input.postcode,
      country: input.country,
      source: input.locationSource,
    };
  }
  if (input.images) {
    payload.images = input.images;
  }
  payload.details = {
    age: input.age,
    gender: input.gender,
    clothing: input.clothing,
    species: input.species,
    breed: input.breed,
    color: input.color,
    vehicleType: input.vehicleType,
    vehicleNumber: input.vehicleNumber,
    manufacturer: input.manufacturer,
    model: input.model,
    year: input.year,
    firNumber: input.firNumber,
    policeStation: input.policeStation,
    policeComplaint: input.policeComplaint,
  };

  const res = await api.patch(`/reports/${id}`, payload);
  return mapToFrontend(res.data);
}

/**
 * Permanently deletes a report and cascades deletion across media and notifications.
 *
 * @param id Target report identifier.
 */
export async function deleteReport(id: string): Promise<void> {
  await api.delete(`/reports/${id}`);
}

/**
 * Toggles or updates the status of an authored report (MISSING -> FOUND -> RESOLVED).
 *
 * @param id Target report identifier.
 * @param status Target status string.
 * @returns Promise resolving to updated {@link Report}.
 */
export async function updateStatus(
  id: string,
  status: Report["status"],
): Promise<Report> {
  const res = await api.patch(`/reports/${id}/status`, { status });
  return mapToFrontend(res.data);
}

/**
 * Retrieves reports authored by the currently logged-in user.
 *
 * @param page Zero-indexed page number.
 * @param size Slice size.
 * @returns Promise resolving to user's reports.
 */
export async function getMyReports(page = 0, size = 50): Promise<Report[]> {
  const res = await api.get(`/reports/mine?page=${page}&size=${size}`);
  const items = res.data.items || [];
  return items.map(mapToFrontend);
}

/**
 * Retrieves aggregate metrics across all reports in the database.
 *
 * @returns Object with counts for total, active, found, and resolved cases.
 */
export async function getStats() {
  try {
    const res = await api.get("/reports/stats");
    return {
      total: res.data.total ?? 0,
      active: res.data.active ?? 0,
      found: res.data.found ?? 0,
      resolved: res.data.resolved ?? 0,
    };
  } catch {
    const res = await api.get("/dashboard/summary");
    const stats = res.data.communityStats || {};
    return {
      total: stats.total ?? 0,
      active: stats.active ?? 0,
      found: stats.found ?? 0,
      resolved: stats.resolved ?? 0,
    };
  }
}

/**
 * Returns the relative URL for streaming the printable A4 emergency flyer / poster.
 * Proxied seamlessly through the Vite reverse proxy in development and Docker.
 *
 * @param id Report unique ID.
 * @returns URL string to the poster endpoint.
 */
export function getPosterUrl(id: string): string {
  return `/api/v1/reports/${id}/poster`;
}
