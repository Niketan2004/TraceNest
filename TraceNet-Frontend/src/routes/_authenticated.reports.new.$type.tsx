import { createFileRoute, notFound, useRouter, Link } from "@tanstack/react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useState } from "react";
import { toast } from "sonner";
import { ArrowLeft, ImagePlus, Loader2, LocateFixed, MapPin, PencilLine, X } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { Textarea } from "../components/ui/textarea";
import { ClientMap } from "../components/maps/ClientMap";
import { Switch } from "../components/ui/switch";
import { createReport } from "../services/reports.service";
import type { ReportType } from "../services/types";
import { TYPE_LABEL } from "../lib/report-utils";
import { useAuth } from "../lib/auth-context";
import { detectLocation, reverseGeocode, type GeoAddress } from "../lib/geocode";
import { extractApiError, toastApiError, type ApiErrorInfo } from "../lib/api-error";
import { ErrorBanner } from "../components/ui/error-banner";

const VALID: ReportType[] = ["person", "pet", "lost-vehicle", "stolen-vehicle"];

export const Route = createFileRoute("/_authenticated/reports/new/$type")({
  loader: ({ params }) => {
    if (!VALID.includes(params.type as ReportType)) throw notFound();
    return { type: params.type as ReportType };
  },
  head: ({ loaderData }) => ({
    meta: [{ title: `New ${loaderData ? TYPE_LABEL[loaderData.type] : "report"} — TraceNest` }],
  }),
  notFoundComponent: () => (
    <div className="p-10 text-center">
      <h1 className="text-2xl font-bold">Unknown category</h1>
      <Link to="/reports/new" className="mt-4 inline-block text-primary hover:underline">
        Back
      </Link>
    </div>
  ),
  component: NewReportForm,
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

const baseSchema = z.object({
  title: z.string().min(4, "Add a short title"),
  description: z.string().min(15, "Please add more details"),
  city: z.string().min(2, "City is required"),
  state: z.string().optional(),
  address: z.string().optional(),
  postcode: z.string().optional(),
  country: z.string().optional(),
  contact: z.string().min(7, "Add a contact number"),
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
  policeComplaint: z.string().optional(),
  firNumber: z.string().optional(),
  policeStation: z.string().optional(),
});
type FormInput = z.input<typeof baseSchema>;
type FormData = z.output<typeof baseSchema>;

function NewReportForm() {
  const { type } = Route.useLoaderData() as { type: ReportType };
  const router = useRouter();
  const qc = useQueryClient();
  const { user } = useAuth();
  const [images, setImages] = useState<{ file: File; url: string }[]>([]);
  const [pin, setPin] = useState<[number, number] | null>(null);
  const [locationMode, setLocationMode] = useState<"gps" | "manual">("gps");
  const [detecting, setDetecting] = useState(false);
  const [detected, setDetected] = useState<GeoAddress | null>(null);
  const [isCityWide, setIsCityWide] = useState(false);
  const [formError, setFormError] = useState<ApiErrorInfo | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormData>({
    resolver: zodResolver(baseSchema),
    defaultValues: { contact: user?.phone ?? "", city: user?.city ?? "" },
  });

  const applyGeo = (g: GeoAddress) => {
    setPin([g.lat, g.lng]);
    setDetected(g);
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
      toast.success("Location detected");
    } catch (e) {
      const msg =
        e instanceof GeolocationPositionError
          ? "Location permission denied — enable it or enter manually."
          : (e as Error).message;
      toast.error(msg);
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

  const mutation = useMutation({
    mutationFn: (data: FormData) => {
      if (images.length === 0 || images.length > 5) {
        throw new Error("Please provide between 1 and 5 images");
      }
      return createReport({
        type,
        title: data.title,
        description: data.description,
        city: data.city,
        state: data.state,
        address: data.address,
        postcode: data.postcode,
        country: data.country,
        locationSource: locationMode,
        contact: data.contact,
        reward: data.reward && data.reward > 0 ? data.reward : undefined,
        lastSeen: data.lastSeen ? new Date(data.lastSeen).toISOString() : undefined,
        images: images.map((img) => img.file),
        lat: pin?.[0] ?? 20.5937,
        lng: pin?.[1] ?? 78.9629,
        status: "missing",
        age: data.age,
        gender: data.gender,
        clothing: data.clothing,
        species: data.species,
        breed: data.breed,
        color: data.color,
        vehicleType: data.vehicleType,
        vehicleNumber: data.vehicleNumber,
        manufacturer: data.manufacturer,
        model: data.model,
        year: data.year,
        policeComplaint: data.policeComplaint,
        firNumber: data.firNumber,
        policeStation: data.policeStation,
        isCityWide: isCityWide,
      });
    },
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: ["reports"] });
      qc.invalidateQueries({ queryKey: ["stats"] });
      toast.success("Report submitted — nearby community notified");
      router.navigate({ to: "/reports/$id", params: { id: r.id } });
    },
    onError: (e) => {
      const apiErr = extractApiError(e);
      setFormError(apiErr);
      if (apiErr.fieldErrors) {
        for (const [field, msg] of Object.entries(apiErr.fieldErrors)) {
          setError(field as any, { type: "server", message: msg });
        }
      }
      toastApiError(e, "Failed to submit report");
    },
  });

  const onFiles = (files: FileList | null) => {
    if (!files) return;
    const newFiles = Array.from(files).slice(0, 5 - images.length);
    for (const f of newFiles) {
      if (f.size > 5 * 1024 * 1024) {
        toast.error(`Image ${f.name} exceeds 5MB`);
        return;
      }
    }
    const mapped = newFiles.map((f) => ({ file: f, url: URL.createObjectURL(f) }));
    setImages((s) => [...s, ...mapped]);
  };

  const cityValue = watch("city");

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <Link
        to="/reports/new"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="h-4 w-4" /> Change category
      </Link>
      <h1 className="mt-3 text-3xl font-bold tracking-tight">New: {TYPE_LABEL[type]}</h1>
      <p className="mt-1 text-muted-foreground">
        Fields marked with * are required. The more details, the better the chances.
      </p>

      <ErrorBanner
        error={formError}
        onDismiss={() => setFormError(null)}
        className="mt-6 mb-2"
      />

      <form
        onSubmit={handleSubmit((d) => {
          setFormError(null);
          mutation.mutate(d);
        })}
        className="mt-8 space-y-6"
      >
        <Section title="Overview">
          <Field label="Title *" error={errors.title?.message}>
            <Input
              placeholder={type === "person" ? "Missing: Ravi Kumar, 34" : "Short summary"}
              {...register("title")}
            />
          </Field>
          <Field label="Description *" error={errors.description?.message} full>
            <Textarea
              rows={4}
              placeholder="What happened, identifying features, anything unusual…"
              {...register("description")}
            />
          </Field>
          <Field label="Contact number *" error={errors.contact?.message}>
            <Input placeholder="+91 …" {...register("contact")} />
          </Field>
          <Field label="Reward (₹ optional)">
            <Input type="number" min="0" placeholder="Optional reward amount" {...register("reward")} />
          </Field>
          <Field label="Last seen (date & time)" full>
            <Input type="datetime-local" {...register("lastSeen")} />
          </Field>
        </Section>

        {type === "person" && (
          <Section title="Person details">
            <Field label="Age">
              <Input type="number" {...register("age")} />
            </Field>
            <Field label="Gender">
              <Input {...register("gender")} />
            </Field>
            <Field label="Clothing / distinguishing marks" full>
              <Input {...register("clothing")} />
            </Field>
            <Field label="Police complaint number">
              <Input {...register("policeComplaint")} />
            </Field>
          </Section>
        )}

        {type === "pet" && (
          <Section title="Pet details">
            <Field label="Species">
              <Input placeholder="Dog, Cat…" {...register("species")} />
            </Field>
            <Field label="Breed">
              <Input {...register("breed")} />
            </Field>
            <Field label="Color">
              <Input {...register("color")} />
            </Field>
            <Field label="Age">
              <Input type="number" {...register("age")} />
            </Field>
            <Field label="Gender">
              <Input {...register("gender")} />
            </Field>
          </Section>
        )}

        {(type === "lost-vehicle" || type === "stolen-vehicle") && (
          <Section title="Vehicle details">
            <Field label="Type">
              <Input placeholder="Bike / Car / Truck…" {...register("vehicleType")} />
            </Field>
            <Field label="Number plate">
              <Input {...register("vehicleNumber")} />
            </Field>
            <Field label="Manufacturer">
              <Input {...register("manufacturer")} />
            </Field>
            <Field label="Model">
              <Input {...register("model")} />
            </Field>
            <Field label="Year">
              <Input type="number" {...register("year")} />
            </Field>
            <Field label="Color">
              <Input {...register("color")} />
            </Field>
            {type === "lost-vehicle" && (
              <Field label="Police complaint number">
                <Input {...register("policeComplaint")} />
              </Field>
            )}
            {type === "stolen-vehicle" && (
              <>
                <Field label="FIR number">
                  <Input {...register("firNumber")} />
                </Field>
                <Field label="Police station">
                  <Input {...register("policeStation")} />
                </Field>
              </>
            )}
          </Section>
        )}

        <Section title="Photos">
          <div className="col-span-full">
            <label
              htmlFor="images"
              className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-border bg-muted/40 p-8 text-center transition hover:border-primary hover:bg-muted"
            >
              <ImagePlus className="h-6 w-6 text-muted-foreground" />
              <span className="text-sm font-medium">Click to upload · up to 5 images</span>
              <span className="text-xs text-muted-foreground">PNG, JPG — max 5MB each</span>
            </label>
            <input
              id="images"
              type="file"
              accept="image/*"
              multiple
              className="hidden"
              onChange={(e) => onFiles(e.target.files)}
            />
            {images.length > 0 && (
              <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
                {images.map((img, i) => (
                  <div
                    key={img.url}
                    className="group relative aspect-square overflow-hidden rounded-xl border border-border"
                  >
                    <img src={img.url} alt="" className="h-full w-full object-cover" />
                    <button
                      type="button"
                      onClick={() => setImages((s) => s.filter((_, j) => j !== i))}
                      className="absolute right-1 top-1 rounded-full bg-black/60 p-1 text-white opacity-0 transition group-hover:opacity-100"
                    >
                      <X className="h-3.5 w-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Section>

        <Section title="Location">
          <div className="col-span-full space-y-4">
            <div className="inline-flex rounded-xl border border-border bg-muted/40 p-1 text-sm">
              <button
                type="button"
                onClick={() => setLocationMode("gps")}
                className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-medium transition ${locationMode === "gps" ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"}`}
              >
                <LocateFixed className="h-4 w-4" /> Use current location
              </button>
              <button
                type="button"
                onClick={() => setLocationMode("manual")}
                className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-medium transition ${locationMode === "manual" ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"}`}
              >
                <PencilLine className="h-4 w-4" /> Enter manually
              </button>
            </div>

            {locationMode === "gps" && (
              <div className="rounded-2xl border border-border/60 bg-muted/30 p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="text-sm">
                    <div className="font-medium">Detect via your device / Google Maps</div>
                    <div className="text-muted-foreground">
                      We'll capture GPS coordinates and full address, and send them with your
                      report.
                    </div>
                  </div>
                  <Button
                    type="button"
                    onClick={handleDetect}
                    disabled={detecting}
                    className="rounded-xl"
                  >
                    {detecting ? (
                      <>
                        <Loader2 className="mr-1.5 h-4 w-4 animate-spin" /> Detecting…
                      </>
                    ) : (
                      <>
                        <LocateFixed className="mr-1.5 h-4 w-4" /> Detect location
                      </>
                    )}
                  </Button>
                </div>
                {detected && (
                  <div className="mt-3 grid gap-2 rounded-xl border border-border/60 bg-background p-3 text-sm sm:grid-cols-2">
                    <InfoRow
                      label="Address"
                      value={detected.address ?? detected.displayName ?? "—"}
                      full
                    />
                    <InfoRow label="City" value={detected.city ?? cityValue ?? "—"} />
                    <InfoRow label="State" value={detected.state ?? "—"} />
                    <InfoRow label="Postcode" value={detected.postcode ?? "—"} />
                    <InfoRow label="Country" value={detected.country ?? "—"} />
                    <InfoRow
                      label="Coordinates"
                      value={`${detected.lat.toFixed(5)}, ${detected.lng.toFixed(5)}`}
                      full
                    />
                  </div>
                )}
              </div>
            )}

            {locationMode === "manual" && (
              <div className="grid gap-4 sm:grid-cols-2">
                <Field label="Address line" full>
                  <Input placeholder="Street, area…" {...register("address")} />
                </Field>
                <Field label="City *" error={errors.city?.message}>
                  <Input {...register("city")} />
                </Field>
                <Field label="State">
                  <Input {...register("state")} />
                </Field>
                <Field label="Postcode">
                  <Input {...register("postcode")} />
                </Field>
                <Field label="Country">
                  <Input {...register("country")} />
                </Field>
              </div>
            )}

            <p className="flex items-center gap-1 text-sm text-muted-foreground">
              <MapPin className="h-4 w-4" />{" "}
              {pin
                ? `Pinned at ${pin[0].toFixed(5)}, ${pin[1].toFixed(5)}`
                : "Click on the map to fine-tune the pin."}
            </p>
            <ClientMap pickable marker={pin} onPick={handleMapPick} height={360} />
          </div>
        </Section>

        <Section title="Alert Options">
          <div className="col-span-full flex items-start gap-4 rounded-xl border border-border bg-muted/20 p-4">
            <Switch checked={isCityWide} onCheckedChange={setIsCityWide} id="cityWide" />
            <div>
              <Label htmlFor="cityWide" className="text-base font-semibold">
                City-Wide Alert (₹100)
              </Label>
              <p className="text-sm text-muted-foreground">
                By default, alerts are sent to users within a 20km radius. Enable this to notify
                everyone in the city. (Mock Payment applied)
              </p>
            </div>
          </div>
        </Section>

        <div className="flex justify-end gap-2">
          <Link to="/reports">
            <Button type="button" variant="outline" className="rounded-xl">
              Cancel
            </Button>
          </Link>
          <Button
            type="submit"
            className="rounded-xl"
            disabled={isSubmitting || mutation.isPending}
          >
            {mutation.isPending ? "Submitting…" : "Submit report"}
          </Button>
        </div>
      </form>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-2xl border border-border/60 bg-card p-6 shadow-[var(--shadow-soft)]">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        {title}
      </h2>
      <div className="mt-4 grid gap-4 sm:grid-cols-2">{children}</div>
    </div>
  );
}
function Field({
  label,
  error,
  full,
  children,
}: {
  label: string;
  error?: string;
  full?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div className={full ? "sm:col-span-2" : ""}>
      <Label className="mb-1.5 block text-sm">{label}</Label>
      {children}
      {error && <p className="mt-1 text-xs text-destructive">{error}</p>}
    </div>
  );
}
function InfoRow({ label, value, full }: { label: string; value: string; full?: boolean }) {
  return (
    <div className={full ? "sm:col-span-2" : ""}>
      <div className="text-xs uppercase tracking-wider text-muted-foreground">{label}</div>
      <div className="mt-0.5 break-words text-sm font-medium">{value}</div>
    </div>
  );
}
