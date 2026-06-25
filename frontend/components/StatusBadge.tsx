import type { VehicleStatus } from "@/lib/types";

// Maps each status to its Tailwind colour classes. Using Record<VehicleStatus, ...>
// means TypeScript will complain if we ever add a status and forget a colour here.
const STYLES: Record<VehicleStatus, string> = {
  PROJECT: "bg-blue-100 text-blue-800",
  DAILY: "bg-green-100 text-green-800",
  SOLD: "bg-gray-200 text-gray-700",
};

export default function StatusBadge({ status }: { status: VehicleStatus }) {
  return (
    <span
      className={`inline-block rounded px-2 py-0.5 text-xs font-medium ${STYLES[status]}`}
    >
      {status}
    </span>
  );
}
