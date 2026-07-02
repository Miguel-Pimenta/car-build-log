import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

// cn = merge Tailwind classes: clsx joins/handles conditionals, twMerge then
// dedupes conflicts so a later class wins (e.g. "px-2 px-4" resolves to "px-4").
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
