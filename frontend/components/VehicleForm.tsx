"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { VEHICLE_STATUSES, type VehicleRequest } from "@/lib/types";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const vehicleSchema = z.object({
  make: z.string().min(1, "Make is required").max(100),
  model: z.string().min(1, "Model is required").max(100),

  year: z.coerce
    .number({ message: "Year must be a number" })
    .int("Year must be a whole number")
    .min(1900, "Year must be 1900 or later")
    .max(2100, "Year must be 2100 or earlier"),

  engineCode: z.string().min(1, "Engine code is required").max(50),

  status: z.enum(["PROJECT", "DAILY", "SOLD"] as const),

  notes: z
    .string()
    .max(2000, "Notes must be 2000 characters or fewer")
    .optional()
    .transform((v) => (v === "" ? undefined : v)),
});

type VehicleFormInput = z.input<typeof vehicleSchema>;

type VehicleFormOutput = z.output<typeof vehicleSchema>;

interface VehicleFormProps {
  initialValue?: VehicleRequest;
  onSubmit: (data: VehicleRequest) => Promise<void>;
  submitLabel: string;
}

export default function VehicleForm({
  initialValue,
  onSubmit,
  submitLabel,
}: VehicleFormProps) {
  const [submitError, setSubmitError] = useState("");

  const form = useForm<VehicleFormInput>({
    resolver: zodResolver(vehicleSchema),
    defaultValues: {
      make: initialValue?.make ?? "",
      model: initialValue?.model ?? "",
      year: initialValue?.year !== undefined ? String(initialValue.year) : "",
      engineCode: initialValue?.engineCode ?? "",
      status: initialValue?.status ?? "PROJECT",
      notes: initialValue?.notes ?? "",
    },
  });

  async function handleValidSubmit(values: VehicleFormOutput) {
    setSubmitError("");
    try {
      await onSubmit(values as VehicleRequest);
    } catch (err) {
      setSubmitError(
        err instanceof Error ? err.message : "Something went wrong",
      );
    }
  }

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(
          handleValidSubmit as Parameters<typeof form.handleSubmit>[0],
        )}
        className="space-y-4 bg-white border rounded p-4"
      >
        {submitError && (
          <p className="text-sm font-medium text-destructive">{submitError}</p>
        )}

        <FormField
          control={form.control}
          name="make"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Make</FormLabel>
              <FormControl>
                <Input placeholder="e.g. Toyota" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="model"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Model</FormLabel>
              <FormControl>
                <Input placeholder="e.g. Supra" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="year"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Year</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  placeholder="e.g. 1993"
                  name={field.name}
                  ref={field.ref}
                  onBlur={field.onBlur}
                  disabled={field.disabled}
                  value={field.value !== undefined ? String(field.value) : ""}
                  onChange={field.onChange}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="engineCode"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Engine code</FormLabel>
              <FormControl>
                <Input placeholder="e.g. 2JZ-GTE" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="status"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Status</FormLabel>
              <FormControl>
                <Select value={field.value} onValueChange={field.onChange}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {VEHICLE_STATUSES.map((option) => (
                      <SelectItem key={option} value={option}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="notes"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Notes</FormLabel>
              <FormControl>
                <textarea
                  className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  placeholder="Optional notes about this vehicle…"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? "Saving…" : submitLabel}
        </Button>
      </form>
    </Form>
  );
}
