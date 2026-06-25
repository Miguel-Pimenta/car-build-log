// Hooks for creating and deleting modifications on a vehicle.
//
// Why separate from use-vehicle.ts?
//   These hooks care about their own invalidation scope. After adding or
//   removing a modification we need to refetch both the modifications list
//   *and* the summary (because totalModifications and totalSpend change).
//   Keeping the logic here makes that coupling explicit and easy to find.

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createModification, deleteModification } from "@/lib/api";
import type { ModificationRequest, ModificationResponse } from "@/lib/types";
import { vehicleKeys } from "./use-vehicles";

// Add a modification to a vehicle. On success, the modifications list and
// the summary are both invalidated — the summary shows totalSpend etc. which
// changes whenever a modification is added.
export function useCreateModification(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation<ModificationResponse, Error, ModificationRequest>({
    mutationFn: (data) => createModification(vehicleId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.modifications(vehicleId),
      });
      // Summary includes totalModifications and totalSpend — both change here.
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.summary(vehicleId),
      });
    },
  });
}

// Delete a modification. We need the vehicleId to know which cache entries
// to invalidate (modifications and summary for that vehicle).
export function useDeleteModification(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (modId) => deleteModification(modId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.modifications(vehicleId),
      });
      // Summary changes for the same reason as on create.
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.summary(vehicleId),
      });
    },
  });
}
