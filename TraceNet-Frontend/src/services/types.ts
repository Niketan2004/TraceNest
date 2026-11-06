export type ReportType = "person" | "pet" | "lost-vehicle" | "stolen-vehicle";
export type ReportStatus = "missing" | "found" | "resolved";

export type Report = {
  id: string;
  type: ReportType;
  title: string;
  description: string;
  city: string;
  state?: string;
  address?: string;
  postcode?: string;
  country?: string;
  locationSource?: "gps" | "manual";
  lat: number;
  lng: number;
  status: ReportStatus;
  reward?: number;
  contact: string;
  images: string[];
  createdAt: string;
  lastSeen?: string;
  // Category-specific
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
  isCityWide?: boolean;
};

export type ReportFilters = {
  q?: string;
  type?: ReportType | "all";
  status?: ReportStatus | "all";
  city?: string;
  sort?: "newest" | "reward";
};

export type CreateReportInput = Omit<Report, "id" | "createdAt" | "images"> & {
  images?: (string | File)[];
};
