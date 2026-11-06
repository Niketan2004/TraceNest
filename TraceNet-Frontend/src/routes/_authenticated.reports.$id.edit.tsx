import { createFileRoute, Link, useRouter } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useState, useEffect } from "react";
import { toast } from "sonner";
import { ArrowLeft, Loader2, LocateFixed, MapPin, PencilLine } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import { ClientMap } from "../components/maps/ClientMap";
import { getReport, updateReport } from "../services/reports.service";
import { TYPE_LABEL } from "../lib/report-utils";
import { detectLocation, reverseGeocode, type GeoAddress } from "../lib/geocode";
import { extractApiError, toastApiError, type ApiErrorInfo } from "../lib/api-error";
import { ErrorBanner } from "../components/ui/error-banner";
import { ErrorState } from "../components/ui/error-state";

export const Route = createFileRoute("/_authenticated/reports/$id/edit")({
  head: () => ({ meta: [{ title: "Edit Report — TraceNest" }] }),
  component: EditReportPage,
});

const numeric = z
  .union([z.string(), z.number()])
  .optional()
  .transform((v) => {
    if (v === undefined || v === null || v === "") return undefined;
    const n = Number(v);
    return isNaN(n) ? undefined : n;
  });

const rewardNumeric = z
  .union([z.string(), z.number()])
  .optional()
  .transform((v) => {
    if (v === undefined || v === null || v === "") return undefined;
    const n = Number(v);
    return isNaN(n) || n <= 0 ? undefined : n;
  });

const schema = z.object({
  title: z.string().min(4, "Title must be at least 4 characters"),
  description: z.string().min(10, "Please add more details"),
  city: z.string().min(2, "City is required"),
  state: z.string().optional(),
  address: z.string().optional(),
  postcode: z.string().optional(),
  country: z.string().optional(),
  contact: z.string().min(7, "Valid contact number required"),
  reward: rewardNumeric,
  lastSeen: z.string().optional(),
  age: numeric,
  gender: z.string().optional(),
  clothing: z.string().optional(),
  species: z.string().optional(),
  breed: z.string().optional(),
  color: z.string().optional(),
  vehicleType: z.string().optional(),
  vehicleNumber: z.string().optional(),
  manufacturer: z.string().optional(),
  model: z.string().optional(),
  year: numeric,
  firNumber: z.string().optional(),
  policeStation: z.string().optional(),
  policeComplaint: z.string().optional(),
});

type FormInput = z.input<typeof schema>;
type FormData = z.output<typeof schema>;

function EditReportPage() {
  const { id } = Route.useParams();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [formError, setFormError] = useState<ApiErrorInfo | null>(null);

  const {
    data: report,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["report", id],
    queryFn: () => getReport(id),
  });

  const [pin, setPin] = useState<[number, number] | null>(null);
  const [locationMode, setLocationMode] = useState<"gps" | "manual">("gps");
  const [detecting, setDetecting] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormData>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (report) {
      reset({
        title: report.title,
        description: report.description,
        city: report.city || "",
        state: report.state || "",
        address: report.address || "",
        postcode: report.postcode || "",
        country: report.country || "",
        contact: report.contact || "",
        reward: report.reward && report.reward > 0 ? String(report.reward) : "",
        lastSeen: report.lastSeen ? report.lastSeen.slice(0, 16) : "",
        age: report.age != null ? String(report.age) : "",
        gender: report.gender || "",
        clothing: report.clothing || "",
        species: report.species || "",
        breed: report.breed || "",
        color: report.color || "",
        vehicleType: report.vehicleType || "",
        vehicleNumber: report.vehicleNumber || "",
        manufacturer: report.manufacturer || "",
        model: report.model || "",
        year: report.year != null ? String(report.year) : "",
        firNumber: report.firNumber || "",
        policeStation: report.policeStation || "",
        policeComplaint: report.policeComplaint || "",
      });

      if (report.lat && report.lng) {
        setPin([report.lat, report.lng]);
      }
    }
  }, [report, reset]);

  const applyGeo = (g: GeoAddress) => {
    setPin([g.lat, g.lng]);
    if (g.city) setValue("city", g.city, { shouldValidate: true });
    if (g.state) setValue("state", g.state);
    if (g.address) setValue("address", g.address);
    if (g.postcode) setValue("postcode", g.postcode);
    if (g.country) setValue("country", g.country);
  };

  const handleDetect = async () => {
    setDetecting(true);
    try {
      const g = await detectLocation();
      applyGeo(g);
      toast.success("Current location detected");
    } catch {
      toast.error("Location permission denied or unavailable");
    } finally {
      setDetecting(false);
    }
  };

  const handleMapPick = async (lat: number, lng: number) => {
    setPin([lat, lng]);
    if (locationMode === "gps") {
      try {
        const g = await reverseGeocode(lat, lng);
        applyGeo(g);
      } catch {
        /* ignore */
      }
    }
  };

  const updateMutation = useMutation({
    mutationFn: (values: FormData) =>
      updateReport(id, {
        ...values,
        lat: pin ? pin[0] : report?.lat,
        lng: pin ? pin[1] : report?.lng,
        locationSource: locationMode,
      }),
    onSuccess: () => {
      toast.success("Report updated successfully");
      queryClient.invalidateQueries({ queryKey: ["report", id] });
      queryClient.invalidateQueries({ queryKey: ["reports"] });
      queryClient.invalidateQueries({ queryKey: ["my-reports"] });
      router.navigate({ to: "/reports/$id", params: { id } });
    },
    onError: (e) => {
      const apiErr = extractApiError(e);
      setFormError(apiErr);
      if (apiErr.fieldErrors) {
        for (const [field, msg] of Object.entries(apiErr.fieldErrors)) {
          setError(field as any, { type: "server", message: msg });
        }
      }
      toastApiError(e, "Failed to update report");
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-6 w-6 animate-spin" />
        Loading report details...
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-xl py-16 px-4">
        <ErrorState
          error={error}
          title="Could not load report for editing"
          onRetry={() => refetch()}
          homeLink="/my-reports"
        />
      </div>
    );
  }

  if (!report) {
    return (
      <div className="mx-auto max-w-xl py-16 text-center">
        <h2 className="text-xl font-bold">Report not found</h2>
        <Link to="/my-reports" className="mt-4 inline-block text-primary hover:underline">
          Return to My Reports
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <Link
        to="/reports/$id"
        params={{ id }}
        className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="mr-1 h-4 w-4" /> Back to report
      </Link>

      <div className="mt-4">
        <h1 className="text-3xl font-bold tracking-tight">Edit Report</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Update the information for this {TYPE_LABEL[report.type]} report.
        </p>
      </div>

      <ErrorBanner
        error={formError}
        onDismiss={() => setFormError(null)}
        className="mt-6 mb-2"
      />

      <form
        onSubmit={handleSubmit((d) => {
          setFormError(null);
          updateMutation.mutate(d);
        })}
        className="mt-8 space-y-6"
      >
        {/* Core fields */}
        <div className="rounded-2xl border border-border/60 bg-card p-6 space-y-4 shadow-[var(--shadow-soft)]">
          <h2 className="text-lg font-semibold">Basic Details</h2>

          <div>
            <Label htmlFor="title">Title *</Label>
            <Input id="title" {...register("title")} className="mt-1 rounded-xl" />
            {errors.title && (
              <p className="mt-1 text-xs text-destructive">{errors.title.message}</p>
            )}
          </div>

          <div>
            <Label htmlFor="description">Description *</Label>
            <Textarea
              id="description"
              rows={4}
              {...register("description")}
              className="mt-1 rounded-xl"
            />
            {errors.description && (
              <p className="mt-1 text-xs text-destructive">{errors.description.message}</p>
            )}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="contact">Contact Number / Email *</Label>
              <Input id="contact" {...register("contact")} className="mt-1 rounded-xl" />
              {errors.contact && (
                <p className="mt-1 text-xs text-destructive">{errors.contact.message}</p>
              )}
            </div>

            <div>
              <Label htmlFor="reward">Reward (₹ INR optional)</Label>
              <Input
                id="reward"
                type="number"
                min="0"
                placeholder="Optional reward amount"
                {...register("reward")}
                className="mt-1 rounded-xl"
              />
            </div>
          </div>

          <div>
            <Label htmlFor="lastSeen">Last Seen Date & Time</Label>
            <Input
              id="lastSeen"
              type="datetime-local"
              {...register("lastSeen")}
              className="mt-1 rounded-xl"
            />
          </div>
        </div>

        {/* Category specific details */}
        {report.type === "person" && (
          <div className="rounded-2xl border border-border/60 bg-card p-6 space-y-4 shadow-[var(--shadow-soft)]">
            <h2 className="text-lg font-semibold">Person Details</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="age">Age</Label>
                <Input id="age" type="number" {...register("age")} className="mt-1 rounded-xl" />
              </div>
              <div>
                <Label htmlFor="gender">Gender</Label>
                <Input id="gender" {...register("gender")} className="mt-1 rounded-xl" />
              </div>
            </div>
            <div>
              <Label htmlFor="clothing">Clothing Description</Label>
              <Input id="clothing" {...register("clothing")} className="mt-1 rounded-xl" />
            </div>
          </div>
        )}

        {report.type === "pet" && (
          <div className="rounded-2xl border border-border/60 bg-card p-6 space-y-4 shadow-[var(--shadow-soft)]">
            <h2 className="text-lg font-semibold">Pet Details</h2>
            <div className="grid gap-4 sm:grid-cols-3">
              <div>
                <Label htmlFor="species">Species (Dog, Cat, etc.)</Label>
                <Input id="species" {...register("species")} className="mt-1 rounded-xl" />
              </div>
              <div>
                <Label htmlFor="breed">Breed</Label>
                <Input id="breed" {...register("breed")} className="mt-1 rounded-xl" />
              </div>
              <div>
                <Label htmlFor="color">Color</Label>
                <Input id="color" {...register("color")} className="mt-1 rounded-xl" />
              </div>
            </div>
          </div>
        )}

        {(report.type === "lost-vehicle" || report.type === "stolen-vehicle") && (
          <div className="rounded-2xl border border-border/60 bg-card p-6 space-y-4 shadow-[var(--shadow-soft)]">
            <h2 className="text-lg font-semibold">Vehicle Details</h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label htmlFor="vehicleNumber">Registration Number</Label>
                <Input
                  id="vehicleNumber"
                  {...register("vehicleNumber")}
                  className="mt-1 rounded-xl uppercase"
                />
              </div>
              <div>
                <Label htmlFor="vehicleType">Vehicle Type</Label>
                <Input id="vehicleType" {...register("vehicleType")} className="mt-1 rounded-xl" />
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-3">
              <div>
                <Label htmlFor="manufacturer">Manufacturer</Label>
                <Input
                  id="manufacturer"
                  {...register("manufacturer")}
                  className="mt-1 rounded-xl"
                />
              </div>
              <div>
                <Label htmlFor="model">Model</Label>
                <Input id="model" {...register("model")} className="mt-1 rounded-xl" />
              </div>
              <div>
                <Label htmlFor="year">Year</Label>
                <Input id="year" type="number" {...register("year")} className="mt-1 rounded-xl" />
              </div>
            </div>
            {report.type === "stolen-vehicle" && (
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <Label htmlFor="firNumber">FIR Number</Label>
                  <Input id="firNumber" {...register("firNumber")} className="mt-1 rounded-xl" />
                </div>
                <div>
                  <Label htmlFor="policeStation">Police Station</Label>
                  <Input
                    id="policeStation"
                    {...register("policeStation")}
                    className="mt-1 rounded-xl"
                  />
                </div>
              </div>
            )}
          </div>
        )}

        {/* Location Picker */}
        <div className="rounded-2xl border border-border/60 bg-card p-6 space-y-4 shadow-[var(--shadow-soft)]">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Location</h2>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDetect}
              disabled={detecting}
              className="rounded-xl gap-1.5"
            >
              <LocateFixed className="h-4 w-4" />
              {detecting ? "Locating..." : "Use GPS"}
            </Button>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="city">City *</Label>
              <Input id="city" {...register("city")} className="mt-1 rounded-xl" />
              {errors.city && (
                <p className="mt-1 text-xs text-destructive">{errors.city.message}</p>
              )}
            </div>
            <div>
              <Label htmlFor="state">State</Label>
              <Input id="state" {...register("state")} className="mt-1 rounded-xl" />
            </div>
          </div>

          <div>
            <Label htmlFor="address">Address / Landmark</Label>
            <Input id="address" {...register("address")} className="mt-1 rounded-xl" />
          </div>

          <div>
            <p className="text-xs text-muted-foreground mb-2">
              Click on the map to adjust the exact location pin.
            </p>
            <ClientMap
              center={pin || [12.9716, 77.5946]}
              zoom={13}
              pickable
              marker={pin}
              onPick={handleMapPick}
              height={300}
            />
          </div>
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-3">
          <Link to="/reports/$id" params={{ id }}>
            <Button type="button" variant="outline" className="rounded-xl">
              Cancel
            </Button>
          </Link>
          <Button
            type="submit"
            className="rounded-xl gap-1.5 min-w-[140px]"
            disabled={isSubmitting || updateMutation.isPending}
          >
            {isSubmitting || updateMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" /> Saving...
              </>
            ) : (
              "Save Changes"
            )}
          </Button>
        </div>
      </form>
    </div>
  );
}
