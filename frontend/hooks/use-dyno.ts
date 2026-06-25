import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createDynoResult } from "@/lib/api";
import type { DynoRequest, DynoResponse } from "@/lib/types";
import { vehicleKeys } from "./use-vehicles";

export function useCreateDynoResult(vehicleId: string) {
  const queryClient = useQueryClient();
  return useMutation<DynoResponse, Error, DynoRequest>({
    mutationFn: (data) => createDynoResult(vehicleId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.dyno(vehicleId) });
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.summary(vehicleId),
      });
    },
  });
}
