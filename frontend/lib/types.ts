// TypeScript types that describe the data the backend sends and receives.
// They don't do anything at runtime - they just help the editor catch mistakes.

export type ModificationCategory =
  | "ENGINE"
  | "EXHAUST"
  | "INTAKE"
  | "SUSPENSION"
  | "BRAKES"
  | "TUNING"
  | "COSMETIC"
  | "OTHER";

// A plain list of the categories above, so we can build a dropdown from it.
export const MODIFICATION_CATEGORIES: ModificationCategory[] = [
  "ENGINE",
  "EXHAUST",
  "INTAKE",
  "SUSPENSION",
  "BRAKES",
  "TUNING",
  "COSMETIC",
  "OTHER",
];

// ---- What we SEND to the backend ----

export interface VehicleRequest {
  make: string;
  model: string;
  year: number;
  engineCode: string;
  notes?: string;
}

export interface ModificationRequest {
  category: ModificationCategory;
  name: string;
  partNumber?: string;
  cost: number;
  installedAt: string; // a date like "2024-03-10"
  mileageKmAtInstall: number;
}

export interface DynoRequest {
  powerHp: number;
  torqueNm: number;
  measuredAt: string;
  notes?: string;
}

// ---- What we GET BACK from the backend ----

export interface VehicleResponse {
  id: string;
  make: string;
  model: string;
  year: number;
  engineCode: string;
  notes?: string;
  createdAt: string;
}

export interface ModificationResponse {
  id: string;
  vehicleId: string;
  category: ModificationCategory;
  name: string;
  partNumber?: string;
  cost: number;
  installedAt: string;
  mileageKmAtInstall: number;
  createdAt: string;
}

export interface DynoResponse {
  id: string;
  vehicleId: string;
  powerHp: number;
  torqueNm: number;
  measuredAt: string;
  notes?: string;
  createdAt: string;
}

export interface VehicleSummaryResponse {
  vehicleId: string;
  totalModifications: number;
  totalSpend: number;
  spendByCategory: Record<ModificationCategory, number>;
  latestDyno: { powerHp: number; torqueNm: number; measuredAt: string } | null;
  currentPowerHp: number | null;
  currentTorqueNm: number | null;
}

// The backend returns lists in "pages". We only use `content` (the array).
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
