// Hook for creating a dyno result for a vehicle.
//
// Adding a dyno run changes both the dyno list *and* the summary
// (latestDyno / currentPowerHp / currentTorqueNm), so both keys are
// invalidated in onSuccess.

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
      // Summary includes latestDyno, currentPowerHp, currentTorqueNm — all
      // change when a new dyno run is recorded.
      queryClient.invalidateQueries({
        queryKey: vehicleKeys.summary(vehicleId),
      });
    },
  });
}
