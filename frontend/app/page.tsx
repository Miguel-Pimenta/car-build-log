"use client";

import { useState } from "react";
import Link from "next/link";
import { useVehicles } from "@/hooks/use-vehicles";
import StatusBadge from "@/components/StatusBadge";
import { useDebounce } from "@/hooks/use-debounce";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { VEHICLE_STATUSES, VehicleStatus } from "@/lib/types";

export default function HomePage() {
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebounce(search, 300);
  const [status, setStatus] = useState<VehicleStatus | "ALL">("ALL");

  const {
    data: vehicles,
    isLoading,
    isError,
    error,
  } = useVehicles(debouncedSearch, status === "ALL" ? undefined : status);

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">My Vehicles</h1>
        <Link
          href="/vehicles/new"
          className="bg-blue-600 text-white px-4 py-2 rounded"
        >
          + Add vehicle
        </Link>
      </div>

      <Select
        value={status}
        onValueChange={(v) => setStatus(v as VehicleStatus | "ALL")}
      >
        <SelectTrigger className="w-48 mb-4">
          <SelectValue /> {/* shows the current selection */}
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">All Statuses</SelectItem>
          {VEHICLE_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              {s}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <input
        placeholder="Search by make or model…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="border rounded px-3 py-1.5 w-full mb-4"
        aria-label="Search vehicles"
      />

      {isError ? (
        <p className="text-red-600">
          Could not load vehicles: {error?.message}
        </p>
      ) : isLoading ? (
        <p>Loading…</p>
      ) : vehicles?.length === 0 ? (
        <p className="text-gray-500">
          {debouncedSearch || status !== "ALL"
            ? "No vehicles match your filters."
            : "No vehicles yet. Add your first one!"}
        </p>
      ) : (
        <ul className="space-y-3">
          {vehicles?.map((vehicle) => (
            <li key={vehicle.id}>
              <Link
                href={`/vehicles/${vehicle.id}`}
                className="block bg-white border rounded p-4 hover:shadow"
              >
                <div className="flex items-center justify-between gap-2">
                  <p className="font-semibold">
                    {vehicle.year} {vehicle.make} {vehicle.model}
                  </p>
                  <StatusBadge status={vehicle.status} />
                </div>
                <p className="text-sm text-gray-500">{vehicle.engineCode}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
