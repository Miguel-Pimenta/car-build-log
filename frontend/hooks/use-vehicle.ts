// Hooks for reading and mutating a single vehicle and its related data.
//
// Each hook is a thin wrapper around TanStack Query's useQuery / useMutation.
// The important pattern here is `queryClient.invalidateQueries` in `onSuccess`:
//
//   OLD pattern (manual refreshKey):
//     setRefreshKey(key => key + 1)  →  useEffect re-runs  →  4 sequential fetches
//
//   NEW pattern (invalidation):
//     queryClient.invalidateQueries(key)  →  only the affected query refetches,
//     in the background, and the UI updates automatically via the cached data.
//
//   This is better because:
//     - Only the changed slice of data is re-fetched (not everything at once).
//     - Other open tabs/components that use the same key also update.
//     - No manual state management needed.

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  deleteVehicle,
  getModifications,
  getDynoResults,
  getVehicleSummary,
} from "@/lib/api";
import type {
  DynoResponse,
  ModificationResponse,
  VehicleSummaryResponse,
} from "@/lib/types";
import { vehicleKeys } from "./use-vehicles";

// ---- Reads ----

export function useVehicleSummary(id: string) {
  return useQuery<VehicleSummaryResponse, Error>({
    queryKey: vehicleKeys.summary(id),
    queryFn: () => getVehicleSummary(id),
  });
}

export function useModifications(vehicleId: string) {
  return useQuery<ModificationResponse[], Error>({
    queryKey: vehicleKeys.modifications(vehicleId),
    queryFn: () => getModifications(vehicleId),
  });
}

export function useDynoResults(vehicleId: string) {
  return useQuery<DynoResponse[], Error>({
    queryKey: vehicleKeys.dyno(vehicleId),
    queryFn: () => getDynoResults(vehicleId),
  });
}

// ---- Mutations ----

// Delete the whole vehicle. The caller is expected to navigate away in onSuccess.
// We don't invalidate here because the vehicle is gone — the list will update
// when the user lands on it after navigation.
export function useDeleteVehicle() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => deleteVehicle(id),
    onSuccess: (_data, id) => {
      // Remove this vehicle's cached entries so stale data never shows up if
      // the user navigates back or presses the browser back button.
      queryClient.removeQueries({ queryKey: vehicleKeys.detail(id) });
      queryClient.removeQueries({ queryKey: vehicleKeys.summary(id) });
      queryClient.removeQueries({ queryKey: vehicleKeys.modifications(id) });
      queryClient.removeQueries({ queryKey: vehicleKeys.dyno(id) });
      // Invalidate the list so the deleted vehicle disappears there too.
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}
