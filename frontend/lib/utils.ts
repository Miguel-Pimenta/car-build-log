// `cn` merges Tailwind class names intelligently.
// It uses `clsx` to handle conditional classes and `tailwind-merge` to resolve
// conflicts (e.g. if you pass both `px-2` and `px-4`, it keeps only `px-4`).
// Every shadcn component uses this internally, and you can use it in your own
// components too when you need to merge class names conditionally.
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
