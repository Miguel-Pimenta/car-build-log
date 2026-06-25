import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { VehicleStatus } from "@/lib/types";

const STATUS_CLASSES: Record<VehicleStatus, string> = {
  PROJECT: "bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-100",
  DAILY: "bg-green-100 text-green-800 border-green-200 hover:bg-green-100",
  SOLD: "bg-red-100 text-red-800 border-red-200 hover:bg-red-100",
};

interface StatusBadgeProps {
  status: VehicleStatus;
  className?: string;
}

export default function StatusBadge({ status, className }: StatusBadgeProps) {
  return (
    <Badge variant="outline" className={cn(STATUS_CLASSES[status], className)}>
      {status}
    </Badge>
  );
}
