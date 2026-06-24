"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import VehicleForm from "@/components/VehicleForm";
import { getVehicle, updateVehicle } from "@/lib/api";
import type { VehicleRequest } from "@/lib/types";

export default function EditVehiclePage() {
  const router = useRouter();
  const params = useParams(); // reads values from the URL, e.g. the {id} part
  const id = params.id as string;

  const [initialValue, setInitialValue] = useState<VehicleRequest | null>(null);
  const [error, setError] = useState("");

  // Load the existing vehicle once, so we can pre-fill the form with its values.
  useEffect(() => {
    getVehicle(id)
      .then((v) =>
        setInitialValue({
          make: v.make,
          model: v.model,
          year: v.year,
          engineCode: v.engineCode,
          notes: v.notes,
        }),
      )
      .catch((err) => setError(err.message));
  }, [id]);

  async function handleUpdate(data: VehicleRequest) {
    await updateVehicle(id, data);
    router.push(`/vehicles/${id}`);
  }

  if (error) return <p className="text-red-600">{error}</p>;
  if (!initialValue) return <p>Loading…</p>;

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Edit vehicle</h1>
      <VehicleForm
        initialValue={initialValue}
        onSubmit={handleUpdate}
        submitLabel="Save changes"
      />
    </div>
  );
}
