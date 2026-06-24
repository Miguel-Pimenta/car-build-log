"use client";

import { useRouter } from "next/navigation";
import VehicleForm from "@/components/VehicleForm";
import { createVehicle } from "@/lib/api";
import type { VehicleRequest } from "@/lib/types";

export default function NewVehiclePage() {
  const router = useRouter(); // lets us send the user to another page after saving

  // The form calls this when it's submitted. We create the vehicle, then go to it.
  async function handleCreate(data: VehicleRequest) {
    const created = await createVehicle(data);
    router.push(`/vehicles/${created.id}`);
  }

  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Add a vehicle</h1>
      <VehicleForm onSubmit={handleCreate} submitLabel="Create vehicle" />
    </div>
  );
}
