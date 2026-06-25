export type VehicleStatus =
  | "PROJECT"
  | "DAILY"
  | "SOLD";

export const VEHICLE_STATUSES: VehicleStatus[] = ["PROJECT", "DAILY", "SOLD"];

export type ModificationCategory =
  | "ENGINE"
  | "EXHAUST"
  | "INTAKE"
  | "SUSPENSION"
  | "BRAKES"
  | "TUNING"
  | "COSMETIC"
  | "OTHER";

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

export interface VehicleRequest {
  make: string;
  model: string;
  year: number;
  engineCode: string;
  status: VehicleStatus;
  notes?: string;
}

export interface ModificationRequest {
  category: ModificationCategory;
  name: string;
  partNumber?: string;
  cost: number;
  installedAt: string;
  mileageKmAtInstall: number;
}

export interface DynoRequest {
  powerHp: number;
  torqueNm: number;
  measuredAt: string;
  notes?: string;
}

export interface VehicleResponse {
  id: string;
  make: string;
  model: string;
  year: number;
  engineCode: string;
  status: VehicleStatus;
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

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
