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

export function useDeleteVehicle() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, string>({
    mutationFn: (id) => deleteVehicle(id),
    onSuccess: (_data, id) => {
      queryClient.removeQueries({ queryKey: vehicleKeys.detail(id) });
      queryClient.removeQueries({ queryKey: vehicleKeys.summary(id) });
      queryClient.removeQueries({ queryKey: vehicleKeys.modifications(id) });
      queryClient.removeQueries({ queryKey: vehicleKeys.dyno(id) });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}
