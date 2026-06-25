// A tiny hook that delays updating a value until the user stops changing it.
//
// How it works:
//   Every time `value` changes, we set a timer. If `value` changes again before
//   the delay passes, we cancel the old timer and start a new one. So the
//   "debounced" value only updates when `value` is stable for `delay` ms.
//
// This is cleaner than putting setTimeout/clearTimeout directly in a page:
//   - reusable across any component
//   - the cleanup is handled by useEffect, so no memory leaks
"use client";

import { useEffect, useState } from "react";

export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    // Start a timer. When it fires, update debouncedValue.
    const timer = setTimeout(() => setDebouncedValue(value), delay);

    // If `value` changes before `delay` ms, React runs this cleanup function
    // to cancel the pending timer before starting a new one.
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}
