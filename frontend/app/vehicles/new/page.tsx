"use client";

import { useRouter } from "next/navigation";
import VehicleForm from "@/components/VehicleForm";
import { useCreateVehicle } from "@/hooks/use-vehicles";
import type { VehicleRequest } from "@/lib/types";

export default function NewVehiclePage() {
  const router = useRouter();

  const createVehicle = useCreateVehicle();

  async function handleCreate(data: VehicleRequest) {
    const created = await createVehicle.mutateAsync(data);
    router.push(`/vehicles/${created.id}`);
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Add a vehicle</h1>
      <VehicleForm onSubmit={handleCreate} submitLabel="Create vehicle" />
    </div>
  );
}
