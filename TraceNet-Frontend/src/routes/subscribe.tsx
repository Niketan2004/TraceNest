/**
 * @file subscribe.tsx
 * @description Public emergency alert subscription route. Allows community members to
 * drop a location pin, enter contact details, verify their mobile phone number via 6-digit SMS OTP,
 * and receive real-time proximity alerts for nearby missing persons, lost pets, and stolen vehicles.
 */

import { createFileRoute, Link, useRouter } from "@tanstack/react-router";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { LocateFixed, PencilLine, MapPin, Loader2, BellRing } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { ClientMap } from "../components/maps/ClientMap";
import { detectLocation, reverseGeocode, type GeoAddress } from "../lib/geocode";
import { sendOtp, verifyOtp, subscribeForAlerts } from "../services/subscription.service";
import { extractApiError, toastApiError, type ApiErrorInfo } from "../lib/api-error";
import { ErrorBanner } from "../components/ui/error-banner";

export const Route = createFileRoute("/subscribe")({
  component: SubscribePage,
});

const mobileRegex = /^[6-9]\d{9}$/;

const schema = z.object({
  fullName: z.string().min(2, "Full name is required"),
  email: z.string().email("Invalid email address"),
  mobileNumber: z
    .string()
    .optional()
    .refine((val) => !val || mobileRegex.test(val), {
      message: "Must be a valid 10-digit Indian mobile number",
    }),
  city: z.string().min(2, "City is required"),
  address: z.string().min(5, "Address is required"),
});

type FormInput = z.infer<typeof schema>;

function SubscribePage() {
  const router = useRouter();
  const [pin, setPin] = useState<[number, number] | null>(null);
  const [locationMode, setLocationMode] = useState<"gps" | "manual">("gps");
  const [detecting, setDetecting] = useState(false);
  const [detected, setDetected] = useState<GeoAddress | null>(null);

  const [otpSent, setOtpSent] = useState(false);
  const [otpVerified, setOtpVerified] = useState(false);
  const [otpValue, setOtpValue] = useState("");
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const [formError, setFormError] = useState<ApiErrorInfo | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormInput>({
    resolver: zodResolver(schema),
  });

  const mobileValue = watch("mobileNumber");
  const cityValue = watch("city");

  const applyGeo = (g: GeoAddress) => {
    setPin([g.lat, g.lng]);
    setDetected(g);
    if (g.city) setValue("city", g.city, { shouldValidate: true });
    if (g.address) setValue("address", g.address);
  };

  const handleDetect = async () => {
    setDetecting(true);
    try {
      const g = await detectLocation();
      applyGeo(g);
      toast.success("Location detected");
    } catch (e) {
      toast.error("Location permission denied or failed.");
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

  const handleSendOtp = async () => {
    if (!mobileValue || !mobileRegex.test(mobileValue)) {
      toast.error("Please enter a valid 10-digit mobile number");
      return;
    }
    setIsSendingOtp(true);
    try {
      const formatted = mobileValue.startsWith("+91") ? mobileValue : "+91" + mobileValue;
      await sendOtp(formatted);
      setOtpSent(true);
      toast.success("OTP sent successfully");
    } catch (e) {
      toastApiError(e, "Failed to send OTP");
    } finally {
      setIsSendingOtp(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (!otpValue || otpValue.length < 6 || !mobileValue) return;
    try {
      const formatted = mobileValue.startsWith("+91") ? mobileValue : "+91" + mobileValue;
      await verifyOtp(formatted, otpValue);
      setOtpVerified(true);
      toast.success("Mobile number verified!");
    } catch (e) {
      toastApiError(e, "Invalid or expired OTP");
    }
  };

  const onSubmit = async (data: FormInput) => {
    if (data.mobileNumber && !otpVerified) {
      toast.error("Please verify your mobile number with OTP first");
      return;
    }
    if (!pin) {
      toast.error("Please select a location on the map");
      return;
    }

    setFormError(null);
    try {
      await subscribeForAlerts({
        ...data,
        mobileNumber: data.mobileNumber
          ? data.mobileNumber.startsWith("+91")
            ? data.mobileNumber
            : "+91" + data.mobileNumber
          : undefined,
        lat: pin?.[0],
        lng: pin?.[1],
      });
      toast.success("Successfully subscribed to alerts!");
      router.navigate({ to: "/" });
    } catch (e) {
      const apiErr = extractApiError(e);
      setFormError(apiErr);
      if (apiErr.fieldErrors) {
        for (const [field, msg] of Object.entries(apiErr.fieldErrors)) {
          if (field in schema.shape) {
            setError(field as keyof FormInput, { type: "server", message: msg });
          }
        }
      }
      toastApiError(e, "Failed to subscribe");
    }
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <div className="mb-8 text-center">
        <BellRing className="mx-auto h-12 w-12 text-primary" />
        <h1 className="mt-4 text-3xl font-bold tracking-tight">Get Local Alerts</h1>
        <p className="mt-2 text-muted-foreground">
          Subscribe for free to receive Email and SMS notifications when a missing person, pet, or
          vehicle is reported in your 20km area.
        </p>
      </div>

      <ErrorBanner
        error={formError}
        onDismiss={() => setFormError(null)}
        className="mb-6"
      />

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
        <div className="rounded-2xl border border-border/60 bg-card p-6 shadow-sm">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4">
            Personal Details
          </h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <Label className="mb-1.5 block text-sm">Full Name *</Label>
              <Input placeholder="John Doe" {...register("fullName")} />
              {errors.fullName && (
                <p className="mt-1 text-xs text-destructive">{errors.fullName.message}</p>
              )}
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 block text-sm">Email Address *</Label>
              <Input type="email" placeholder="john@example.com" {...register("email")} />
              {errors.email && (
                <p className="mt-1 text-xs text-destructive">{errors.email.message}</p>
              )}
            </div>

            <div className="sm:col-span-2">
              <Label className="mb-1.5 block text-sm">Mobile Number (Optional)</Label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                    +91
                  </span>
                  <Input
                    type="tel"
                    className="pl-10"
                    placeholder="9876543210"
                    disabled={otpVerified}
                    {...register("mobileNumber")}
                  />
                </div>
                {mobileValue && !otpVerified && (
                  <Button type="button" onClick={handleSendOtp} disabled={isSendingOtp}>
                    {isSendingOtp ? "Sending..." : otpSent ? "Resend OTP" : "Send OTP"}
                  </Button>
                )}
                {otpVerified && (
                  <Button
                    type="button"
                    disabled
                    variant="outline"
                    className="text-green-600 border-green-600"
                  >
                    Verified ✓
                  </Button>
                )}
              </div>
              {errors.mobileNumber && (
                <p className="mt-1 text-xs text-destructive">{errors.mobileNumber.message}</p>
              )}
            </div>

            {otpSent && !otpVerified && (
              <div className="sm:col-span-2 rounded-lg bg-muted/40 p-4 border border-border">
                <Label className="mb-1.5 block text-sm">Enter 6-digit OTP</Label>
                <div className="flex gap-2">
                  <Input
                    type="text"
                    maxLength={6}
                    placeholder="123456"
                    value={otpValue}
                    onChange={(e) => setOtpValue(e.target.value)}
                  />
                  <Button type="button" onClick={handleVerifyOtp}>
                    Verify
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="rounded-2xl border border-border/60 bg-card p-6 shadow-sm">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4">
            Location Coverage
          </h2>
          <div className="space-y-4">
            <div className="inline-flex rounded-xl border border-border bg-muted/40 p-1 text-sm">
              <button
                type="button"
                onClick={() => setLocationMode("gps")}
                className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 font-medium transition ${locationMode === "gps" ? "bg-background text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"}`}
              >
                <LocateFixed className="h-4 w-4" /> Auto-detect
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
                    <div className="font-medium">Detect your area</div>
                    <div className="text-muted-foreground">
                      We'll set your coverage zone to 20km around this point.
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
              </div>
            )}

            {locationMode === "manual" && (
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <Label className="mb-1.5 block text-sm">Address *</Label>
                  <Input placeholder="Street, area…" {...register("address")} />
                  {errors.address && (
                    <p className="mt-1 text-xs text-destructive">{errors.address.message}</p>
                  )}
                </div>
                <div className="sm:col-span-2">
                  <Label className="mb-1.5 block text-sm">City *</Label>
                  <Input {...register("city")} />
                  {errors.city && (
                    <p className="mt-1 text-xs text-destructive">{errors.city.message}</p>
                  )}
                </div>
              </div>
            )}

            <p className="flex items-center gap-1 text-sm text-muted-foreground mt-4">
              <MapPin className="h-4 w-4" />{" "}
              {pin
                ? `Monitoring 20km around ${pin[0].toFixed(5)}, ${pin[1].toFixed(5)}`
                : "Click on the map to drop a pin."}
            </p>
            <ClientMap pickable marker={pin} onPick={handleMapPick} height={300} />
          </div>
        </div>

        <div className="flex justify-end gap-3">
          <Link to="/">
            <Button type="button" variant="outline" className="rounded-xl">
              Cancel
            </Button>
          </Link>
          <Button
            type="submit"
            className="rounded-xl"
            disabled={isSubmitting || (!!mobileValue && !otpVerified)}
          >
            {isSubmitting ? "Subscribing..." : "Subscribe Now"}
          </Button>
        </div>
      </form>
    </div>
  );
}
