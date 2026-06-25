"use client";

import { useParams, useRouter } from "next/navigation";
import VehicleForm from "@/components/VehicleForm";
import { useVehicle, useUpdateVehicle } from "@/hooks/use-vehicles";
import type { VehicleRequest } from "@/lib/types";

export default function EditVehiclePage() {
  const router = useRouter();
  const params = useParams();
  const id = params.id as string;

  // useVehicle replaces the manual useEffect + useState that fetched the vehicle
  // for pre-filling the form. TanStack Query handles loading / error states and
  // caches the result — if the user navigated from the detail page the data is
  // already there and no extra network call is made.
  const { data: vehicle, isLoading, error } = useVehicle(id);

  // useMutation wraps the update call. On success the hook invalidates the
  // vehicle's detail cache and the list cache (see use-vehicles.ts).
  const updateVehicle = useUpdateVehicle(id);

  async function handleUpdate(data: VehicleRequest) {
    await updateVehicle.mutateAsync(data);
    router.push(`/vehicles/${id}`);
  }

  if (isLoading) return <p>Loading…</p>;
  if (error) return <p className="text-red-600">{error.message}</p>;
  if (!vehicle) return null;

  // Build the VehicleRequest shape from the full VehicleResponse, so VehicleForm
  // gets exactly the fields it expects (no extra backend-only fields like createdAt).
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
