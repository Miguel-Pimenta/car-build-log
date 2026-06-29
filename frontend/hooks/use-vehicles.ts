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
  return useQuery<VehicleResponse[], Error>({
    queryKey: vehicleKeys.list(search, status),
    queryFn: async () => {
      const page = await getVehicles(search, status);
      return page.content;
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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.detail(id) });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}
