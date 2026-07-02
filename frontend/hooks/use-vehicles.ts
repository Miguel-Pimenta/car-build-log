import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createVehicle,
  getVehicles,
  getVehicle,
  updateVehicle,
} from "@/lib/api";
import type {
  VehicleRequest,
  VehicleResponse,
  VehicleStatus,
} from "@/lib/types";

// Query-key factory: one place that builds every cache key, so keys stay consistent
// across reads and invalidations. All keys start with "vehicles" so invalidating
// vehicleKeys.all (below) wipes the whole subtree in one call.
export const vehicleKeys = {
  all: ["vehicles"] as const,
  list: (search: string, status?: VehicleStatus) =>
    ["vehicles", search, status] as const,
  detail: (id: string) => ["vehicles", id, "detail"] as const,
  summary: (id: string) => ["vehicles", id, "summary"] as const,
  modifications: (id: string) => ["vehicles", id, "modifications"] as const,
  dyno: (id: string) => ["vehicles", id, "dyno"] as const,
};

export function useVehicles(search: string, status?: VehicleStatus) {
  // useQuery = read/GET. The queryKey is the cache identity: change search/status
  // and it becomes a different key, so Query refetches automatically for the new filters.
  return useQuery<VehicleResponse[], Error>({
    queryKey: vehicleKeys.list(search, status),
    queryFn: async () => {
      const page = await getVehicles(search, status);
      return page.content; // unwrap the paginated envelope down to just the rows
    },
  });
}

export function useVehicle(id: string) {
  return useQuery<VehicleResponse, Error>({
    queryKey: vehicleKeys.detail(id),
    queryFn: () => getVehicle(id),
  });
}

export function useCreateVehicle() {
  const queryClient = useQueryClient();
  // useMutation = write/POST. On success, invalidate every "vehicles" key so any
  // cached list refetches and shows the new row.
  return useMutation<VehicleResponse, Error, VehicleRequest>({
    mutationFn: (data) => createVehicle(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}

export function useUpdateVehicle(id: string) {
  const queryClient = useQueryClient();
  return useMutation<VehicleResponse, Error, VehicleRequest>({
    mutationFn: (data) => updateVehicle(id, data),
    // Invalidate both this vehicle's detail (the edited one) and the lists (its
    // summary row may have changed), forcing both to refetch.
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}
