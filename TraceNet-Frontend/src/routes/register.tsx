import { createFileRoute, Link, useRouter } from "@tanstack/react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useState } from "react";
import { useAuth } from "../lib/auth-context";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { toast } from "sonner";
import { extractApiError, toastApiError, type ApiErrorInfo } from "../lib/api-error";
import { ErrorBanner } from "../components/ui/error-banner";

export const Route = createFileRoute("/register")({
  head: () => ({
    meta: [
      { title: "Create account — TraceNest" },
      {
        name: "description",
        content: "Join TraceNest to help reunite lost pets, missing persons, and stolen vehicles.",
      },
    ],
  }),
  component: RegisterPage,
});

const schema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Name must be at least 2 characters")
    .max(80, "Name cannot exceed 80 characters"),
  email: z.string().trim().email("Enter a valid email address"),
  password: z
    .string()
    .min(8, "Password must be at least 8 characters")
    .regex(/^(?=.*[A-Za-z])(?=.*\d)/, "Password must contain at least 1 letter and 1 number"),
});
type FormData = z.infer<typeof schema>;

function RegisterPage() {
  const { register: signup } = useAuth();
  const router = useRouter();
  const [formError, setFormError] = useState<ApiErrorInfo | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setFormError(null);
    try {
      await signup(data.name.trim(), data.email.trim(), data.password);
      toast.success("Account created — welcome to TraceNest!");
      router.navigate({ to: "/dashboard" });
    } catch (err: unknown) {
      const apiErr = extractApiError(err);
      setFormError(apiErr);

      if (apiErr.code === "EMAIL_TAKEN" || apiErr.status === 409) {
        setError("email", { type: "server", message: apiErr.message });
      }

      if (apiErr.fieldErrors) {
        for (const [field, msg] of Object.entries(apiErr.fieldErrors)) {
          if (field === "name" || field === "email" || field === "password") {
            setError(field as keyof FormData, { type: "server", message: msg });
          }
        }
      }

      toastApiError(err, "Registration failed");
    }
  };

  return (
    <div className="mx-auto flex min-h-[calc(100vh-8rem)] max-w-md items-center px-4 py-10">
      <div className="w-full rounded-2xl border border-border/60 bg-card p-8 shadow-[var(--shadow-soft)]">
        <h1 className="text-2xl font-bold">Create your account</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Start reporting and receive nearby alerts.
        </p>

        <div className="mt-6">
          <ErrorBanner
            error={formError}
            onDismiss={() => setFormError(null)}
            className="mb-4"
          />
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <Label htmlFor="name">Full name</Label>
            <Input id="name" placeholder="John Doe" {...register("name")} />
            {errors.name && <p className="mt-1 text-xs text-destructive">{errors.name.message}</p>}
          </div>
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" placeholder="you@example.com" {...register("email")} />
            {errors.email && (
              <p className="mt-1 text-xs text-destructive">{errors.email.message}</p>
            )}
          </div>
          <div>
            <Label htmlFor="password">Password</Label>
            <Input id="password" type="password" placeholder="••••••••" {...register("password")} />
            <p className="mt-1 text-[11px] text-muted-foreground">
              Must be at least 8 characters with at least 1 letter and 1 number.
            </p>
            {errors.password && (
              <p className="mt-1 text-xs text-destructive">{errors.password.message}</p>
            )}
          </div>
          <Button type="submit" className="w-full rounded-xl" disabled={isSubmitting}>
            {isSubmitting ? "Creating account…" : "Create account"}
          </Button>
        </form>
        <p className="mt-5 text-center text-sm text-muted-foreground">
          Have an account?{" "}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
