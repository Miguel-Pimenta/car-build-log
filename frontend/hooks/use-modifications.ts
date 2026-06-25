import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createModification, deleteModification } from "@/lib/api";
import type { ModificationRequest, ModificationResponse } from "@/lib/types";
import { vehicleKeys } from "./use-vehicles";

export function useCreateModification(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation<ModificationResponse, Error, ModificationRequest>({
    mutationFn: (data) => createModification(vehicleId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.modifications(vehicleId),
      });
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.summary(vehicleId),
      });
    },
  });
}

export function useDeleteModification(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (modId) => deleteModification(modId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.modifications(vehicleId),
      });
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.summary(vehicleId),
      });
    },
  });
}
