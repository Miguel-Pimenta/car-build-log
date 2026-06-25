"use client"; // This component runs in the browser, so it can use React hooks.

import { useState } from "react";
import Link from "next/link";
import { useVehicles } from "@/hooks/use-vehicles";
import StatusBadge from "@/components/StatusBadge";
import { useDebounce } from "@/hooks/use-debounce";

// BEFORE (manual data fetching):
//   useEffect + setTimeout + setLoading/setError/setVehicles — ~20 lines of
//   boilerplate per data fetch, no caching, no background refetch.
//
// AFTER (TanStack Query):
//   const { data, isLoading, isError } = useVehicles(debouncedSearch)
//   Query result is cached, refetched when the window regains focus, and
//   shared with any other component that calls the same hook.

export default function HomePage() {
  // `search` tracks every keystroke; `debouncedSearch` only updates after
  // the user pauses typing for 300 ms. We pass `debouncedSearch` to the query
  // so we don't fire an API request on every character.
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebounce(search, 300);

  // `useVehicles` wraps TanStack Query's `useQuery`. It returns:
  //   data        → the array of vehicles (undefined while loading)
  //   isLoading   → true on the very first fetch (no cached data yet)
  //   isError     → true if the fetch threw
  //   error       → the Error object when isError is true
  const { data: vehicles, isLoading, isError, error } = useVehicles(debouncedSearch);

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">My Vehicles</h1>
        <Link
          href="/vehicles/new"
          className="bg-blue-600 text-white px-4 py-2 rounded"
        >
          + Add vehicle
        </Link>
      </div>

      {/* Typing here updates `search`. After 300ms of no typing, `debouncedSearch`
          updates too, which changes the query key and triggers a new fetch. */}
      <input
        placeholder="Search by make or model…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="border rounded px-3 py-1.5 w-full mb-4"
        aria-label="Search vehicles"
      />

      {/* The search input stays mounted; only this area below re-renders. */}
      {isError ? (
        <p className="text-red-600">
          Could not load vehicles: {error?.message}
        </p>
      ) : isLoading ? (
        <p>Loading…</p>
      ) : vehicles?.length === 0 ? (
        // Preserve the original two distinct empty states.
        <p className="text-gray-500">
          {debouncedSearch
            ? `No vehicles match "${debouncedSearch}".`
            : "No vehicles yet. Add your first one!"}
        </p>
      ) : (
        <ul className="space-y-3">
          {/* Turn each vehicle in the array into a list item. `key` helps React
              know which item changed/moved when the list updates. */}
          {vehicles?.map((vehicle) => (
            <li key={vehicle.id}>
              <Link
                href={`/vehicles/${vehicle.id}`}
                className="block bg-white border rounded p-4 hover:shadow"
              >
                <div className="flex items-center justify-between gap-2">
                  <p className="font-semibold">
                    {vehicle.year} {vehicle.make} {vehicle.model}
                  </p>
                  <StatusBadge status={vehicle.status} />
                </div>
                <p className="text-sm text-gray-500">{vehicle.engineCode}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
