// StatusBadge wraps the shadcn <Badge> with vehicle-specific colour semantics.
//
// Why not use Badge's built-in variants directly everywhere?
//   Those variants (default, secondary, destructive…) are generic design tokens.
//   StatusBadge adds *domain meaning*: "PROJECT = blue" is a business rule that
//   should live in one place. If you add a new status tomorrow, you update the
//   map here and it propagates everywhere.
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { VehicleStatus } from "@/lib/types";

// Maps each status to Tailwind colour classes that override the badge's default
// colours. `cn` merges them with whatever base classes Badge already applies.
const STATUS_CLASSES: Record<VehicleStatus, string> = {
  PROJECT: "bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-100",
  DAILY: "bg-green-100 text-green-800 border-green-200 hover:bg-green-100",
  SOLD: "bg-gray-100 text-gray-700 border-gray-200 hover:bg-gray-100",
};

interface StatusBadgeProps {
  status: VehicleStatus;
  className?: string;
}

export default function StatusBadge({ status, className }: StatusBadgeProps) {
  return (
    // `variant="outline"` gives us a clean base (border visible, no dark bg)
    // and then STATUS_CLASSES overrides the colours for each status.
    <Badge
      variant="outline"
      className={cn(STATUS_CLASSES[status], className)}
    >
      {status}
    </Badge>
  );
}
