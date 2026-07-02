"use client";

import { useEffect, useState } from "react";

export function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    // Schedule the update for `delay` ms later...
    const timer = setTimeout(() => setDebouncedValue(value), delay);

    // ...but the cleanup cancels it whenever `value` changes again. Fast typing keeps
    // resetting the timer, so the value only "settles" once the user pauses = debounce.
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}
