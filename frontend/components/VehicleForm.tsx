"use client";

import { VEHICLE_STATUSES, VehicleRequest, VehicleStatus } from "@/lib/types";
import { useState } from "react";

// A reusable form for BOTH creating and editing a vehicle.
// It receives three props from its parent:
//   initialValue - pre-fill the fields (used when editing); undefined when adding
//   onSubmit     - a function the parent gives us to run when the form is submitted
//   submitLabel  - the text on the button
export default function VehicleForm({
  initialValue,
  onSubmit,
  submitLabel,
}: {
  initialValue?: VehicleRequest;
  onSubmit: (data: VehicleRequest) => Promise<void>;
  submitLabel: string;
}) {
  // "Controlled inputs": each field's value lives in state, and the input shows it.
  const [make, setMake] = useState(initialValue?.make ?? "");
  const [model, setModel] = useState(initialValue?.model ?? "");
  const [year, setYear] = useState(initialValue?.year?.toString() ?? "");
  const [engineCode, setEngineCode] = useState(initialValue?.engineCode ?? "");
  // Status is one of a fixed set, so new vehicles default to PROJECT.
  const [status, setStatus] = useState<VehicleStatus>(
    initialValue?.status ?? "PROJECT",
  );
  const [notes, setNotes] = useState(initialValue?.notes ?? "");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault(); // stop the browser reloading the page on submit
    setSaving(true);
    setError("");
    try {
      await onSubmit({
        make,
        model,
        year: Number(year), // inputs are always text, so convert to a number
        engineCode,
        status,
        notes: notes || undefined,
      });
      // On success the parent navigates away, so there's nothing else to do here.
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
      setSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-3 bg-white border rounded p-4"
    >
      {error && <p className="text-red-600">{error}</p>}
      <Field label="Make" value={make} onChange={setMake} />
      <Field label="Model" value={model} onChange={setModel} />
      <Field label="Year" type="number" value={year} onChange={setYear} />
      <Field label="Engine code" value={engineCode} onChange={setEngineCode} />

      {/* A controlled <select> - same pattern as the category dropdown on the
          detail page. `value` shows the current status; onChange updates it. */}
      <label className="block">
        <span className="block text-sm font-medium mb-1">Status</span>
        <select
          value={status}
          onChange={(event) => setStatus(event.target.value as VehicleStatus)}
          className="w-full border rounded px-3 py-2"
        >
          {VEHICLE_STATUSES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </label>

      <Field label="Notes" value={notes} onChange={setNotes} />
      <button
        type="submit"
        disabled={saving}
        className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50"
      >
        {saving ? "Saving…" : submitLabel}
      </button>
    </form>
  );
}

// A small helper component for one labelled text input, so the form above stays
// short. It's only used here, so it lives in the same file.
function Field({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
}) {
  return (
    <label className="block">
      <span className="block text-sm font-medium mb-1">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full border rounded px-3 py-2"
      />
    </label>
  );
}
