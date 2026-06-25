import type {
  DynoRequest,
  DynoResponse,
  ModificationRequest,
  ModificationResponse,
  PageResponse,
  VehicleRequest,
  VehicleResponse,
  VehicleSummaryResponse,
} from "./types";

const BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(BASE + path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed (${response.status})`);
  }

  if (response.status === 204) return undefined as T;

  return response.json();
}

// ---- Vehicles ----

export function getVehicles(search?: string) {
  let path = "/vehicles?page=0&size=100";
  if (search && search.trim() !== "") {
    path += `&search=${encodeURIComponent(search.trim())}`;
  }
  return request<PageResponse<VehicleResponse>>(path);
}

export function getVehicle(id: string) {
  return request<VehicleResponse>(`/vehicles/${id}`);
}

export function createVehicle(data: VehicleRequest) {
  return request<VehicleResponse>("/vehicles", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function updateVehicle(id: string, data: VehicleRequest) {
  return request<VehicleResponse>(`/vehicles/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export function deleteVehicle(id: string) {
  return request<void>(`/vehicles/${id}`, { method: "DELETE" });
}

// ---- Modifications ----

export function getModifications(vehicleId: string) {
  return request<ModificationResponse[]>(
    `/vehicles/${vehicleId}/modifications`,
  );
}

export function createModification(
  vehicleId: string,
  data: ModificationRequest,
) {
  return request<ModificationResponse>(`/vehicles/${vehicleId}/modifications`, {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function deleteModification(id: string) {
  return request<void>(`/modifications/${id}`, { method: "DELETE" });
}

// ---- Dyno results ----

export function getDynoResults(vehicleId: string) {
  return request<DynoResponse[]>(`/vehicles/${vehicleId}/dyno`);
}

export function createDynoResult(vehicleId: string, data: DynoRequest) {
  return request<DynoResponse>(`/vehicles/${vehicleId}/dyno`, {
    method: "POST",
    body: JSON.stringify(data),
  });
}

// ---- Summary ----

export function getVehicleSummary(id: string) {
  return request<VehicleSummaryResponse>(`/vehicles/${id}/summary`);
}
