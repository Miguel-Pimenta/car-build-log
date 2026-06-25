"use client";

import { useRouter } from "next/navigation";
import VehicleForm from "@/components/VehicleForm";
import { useCreateVehicle } from "@/hooks/use-vehicles";
import type { VehicleRequest } from "@/lib/types";

export default function NewVehiclePage() {
  const router = useRouter();

  // useMutation wraps the create call so TanStack Query can handle loading
  // state and errors, and so we can invalidate the list cache on success.
  const createVehicle = useCreateVehicle();

  // VehicleForm calls this with the validated form data when submitted.
  // We return a Promise so the form knows when to clear its saving state.
  async function handleCreate(data: VehicleRequest) {
    const created = await createVehicle.mutateAsync(data);
    // mutateAsync resolves with the new vehicle — navigate straight to it.
    router.push(`/vehicles/${created.id}`);
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Add a vehicle</h1>
      <VehicleForm onSubmit={handleCreate} submitLabel="Create vehicle" />
    </div>
  );
}
