// Custom hooks that wrap TanStack Query calls for vehicles.
//
// Why extract these into their own file?
//   Separation of concerns: the page component decides *what to render*;
//   these hooks decide *how to fetch/mutate the data*. If you ever change the
//   API shape, caching strategy, or add optimistic updates, you change it here —
//   not scattered across every component that needs vehicles.
//
// Query keys are an array like ["vehicles", search]. TanStack Query uses this
// array as a cache key, so ["vehicles", "civic"] and ["vehicles", "bmw"] are
// stored separately and don't overwrite each other.
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createVehicle, getVehicles, getVehicle, updateVehicle } from "@/lib/api";
import type { VehicleRequest, VehicleResponse } from "@/lib/types";

// Centralised query key factory. All vehicle-related queries hang off
// `vehicleKeys` so you can invalidate a whole subtree at once.
//
//   vehicleKeys.all                    → ["vehicles"]
//   vehicleKeys.list("civic")          → ["vehicles", "civic"]
//   vehicleKeys.detail("abc-123")      → ["vehicles", "abc-123", "detail"]
//   vehicleKeys.summary("abc-123")     → ["vehicles", "abc-123", "summary"]
//   vehicleKeys.modifications("abc")   → ["vehicles", "abc-123", "modifications"]
//   vehicleKeys.dyno("abc-123")        → ["vehicles", "abc-123", "dyno"]
//
// Invalidating vehicleKeys.all() will refetch every vehicle query (list + all details).
export const vehicleKeys = {
  all: ["vehicles"] as const,
  list: (search: string) => ["vehicles", search] as const,
  detail: (id: string) => ["vehicles", id, "detail"] as const,
  summary: (id: string) => ["vehicles", id, "summary"] as const,
  modifications: (id: string) => ["vehicles", id, "modifications"] as const,
  dyno: (id: string) => ["vehicles", id, "dyno"] as const,
};

export function useVehicles(search: string) {
  return useQuery<VehicleResponse[], Error>({
    queryKey: vehicleKeys.list(search),
    // `queryFn` is the function TanStack Query calls to fetch data.
    // It re-runs automatically whenever `queryKey` changes (i.e. when search changes).
    queryFn: async () => {
      const page = await getVehicles(search);
      // We only use the `content` array — the pagination fields are not needed
      // because we always fetch all results at once (size=100).
      return page.content;
    },
  });
}

// Fetch a single vehicle by id.
export function useVehicle(id: string) {
  return useQuery<VehicleResponse, Error>({
    queryKey: vehicleKeys.detail(id),
    queryFn: () => getVehicle(id),
  });
}

// Create a new vehicle. On success the vehicles list is invalidated so it
// refetches automatically — the caller gets the created vehicle back to
// navigate to it.
export function useCreateVehicle() {
  const queryClient = useQueryClient();
  return useMutation<VehicleResponse, Error, VehicleRequest>({
    mutationFn: (data) => createVehicle(data),
    onSuccess: () => {
      // Invalidate the whole list so the new vehicle appears there.
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}

// Update an existing vehicle. On success both the detail cache and the list
// are invalidated so every view that shows this vehicle gets fresh data.
export function useUpdateVehicle(id: string) {
  const queryClient = useQueryClient();
  return useMutation<VehicleResponse, Error, VehicleRequest>({
    mutationFn: (data) => updateVehicle(id, data),
    onSuccess: () => {
      // Invalidate this vehicle's detail entry and the whole list.
      queryClient.invalidateQueries({ queryKey: vehicleKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}
