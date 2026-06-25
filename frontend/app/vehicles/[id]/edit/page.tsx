"use client";

import { useParams, useRouter } from "next/navigation";
import VehicleForm from "@/components/VehicleForm";
import { useVehicle, useUpdateVehicle } from "@/hooks/use-vehicles";
import type { VehicleRequest } from "@/lib/types";

export default function EditVehiclePage() {
  const router = useRouter();
  const params = useParams();
  const id = params.id as string;

  const { data: vehicle, isLoading, error } = useVehicle(id);

  const updateVehicle = useUpdateVehicle(id);

  async function handleUpdate(data: VehicleRequest) {
    await updateVehicle.mutateAsync(data);
    router.push(`/vehicles/${id}`);
  }

  if (isLoading) return <p>Loading…</p>;
  if (error) return <p className="text-red-600">{error.message}</p>;
  if (!vehicle) return null;

  const initialValue: VehicleRequest = {
    make: vehicle.make,
    model: vehicle.model,
    year: vehicle.year,
    engineCode: vehicle.engineCode,
    status: vehicle.status,
    notes: vehicle.notes,
  };

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
