import type {
  DynoRequest,
  DynoResponse,
  ModificationRequest,
  ModificationResponse,
  PageResponse,
  VehicleRequest,
  VehicleResponse,
  VehicleStatus,
  VehicleSummaryResponse,
} from "./types";

const BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1";

// ---- Auth token (kept in the browser so it survives refreshes) ----
const TOKEN_KEY = "carbuildlog.token";

function getToken(): string | null {
  if (typeof window === "undefined") return null; // no localStorage on the server
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  const response = await fetch(BASE + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      // Attach JWT as a Bearer header so the API knows who's calling; spread only when present
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers, // caller-supplied headers win (spread last)
    },
  });

  // 401 = not logged in / token expired. Bounce to the login page — but NOT
  // while the user is literally trying to log in (so the form can show the error).
  if (response.status === 401 && !path.startsWith("/auth")) {
    logout();
    if (typeof window !== "undefined") window.location.href = "/login";
  }

  if (!response.ok) {
    // Try to surface the API's error message; .catch guards against non-JSON error bodies
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed (${response.status})`);
  }

  if (response.status === 204) return undefined as T; // 204 No Content has no body to parse

  return response.json();
}

// ---- Auth ----

interface AuthResponse {
  token: string;
}

export async function login(username: string, password: string): Promise<void> {
  const res = await request<AuthResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  setToken(res.token);
}

export async function register(data: {
  username: string;
  email: string;
  password: string;
  name?: string;
}): Promise<void> {
  const res = await request<AuthResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify(data),
  });
  setToken(res.token);
}

// ---- Vehicles ----

export function getVehicles(search?: string, status?: VehicleStatus) {
  let path = "/vehicles?page=0&size=100";
  if (search && search.trim() !== "") {
    path += `&search=${encodeURIComponent(search.trim())}`;
  }
  if (status) {
    path += `&status=${status}`;
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
  return request<ModificationResponse[]>(`/vehicles/${vehicleId}/modifications`);
}

export function createModification(vehicleId: string, data: ModificationRequest) {
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
