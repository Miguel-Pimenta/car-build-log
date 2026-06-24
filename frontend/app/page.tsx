"use client"; // This component runs in the browser, so it can use React hooks.

import { useEffect, useState } from "react";
import Link from "next/link";
import { getVehicles } from "@/lib/api";
import type { VehicleResponse } from "@/lib/types";

export default function HomePage() {
  // `useState` gives us a value and a function to change it. When we change it,
  // React re-renders this component.
  const [vehicles, setVehicles] = useState<VehicleResponse[]>([]); // the data
  const [loading, setLoading] = useState(true); // are we still loading?
  const [error, setError] = useState(""); // any error message
  const [search, setSearch] = useState(""); // what's typed in the search box

  // This effect re-runs whenever `search` changes (note the [search] at the end).
  // So as you type, the list re-fetches from the backend with your search term.
  useEffect(() => {
    // Debounce: wait 300ms after the last keystroke before calling the API, so
    // we don't fire a request on every single character.
    const timer = setTimeout(() => {
      setLoading(true);
      setError("");
      getVehicles(search)
        .then((page) => setVehicles(page.content))
        .catch((err) => setError(err.message))
        .finally(() => setLoading(false));
    }, 300);

    // If `search` changes again before 300ms passes, cancel the pending call.
    return () => clearTimeout(timer);
  }, [search]);

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

      {/* Typing here updates `search`, which re-runs the effect above. */}
      <input
        placeholder="Search by make or model…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="border rounded px-3 py-1.5 w-full mb-4"
      />

      {/* The search box above stays mounted; only this area below changes. */}
      {error ? (
        <p className="text-red-600">Could not load vehicles: {error}</p>
      ) : loading ? (
        <p>Loading…</p>
      ) : vehicles.length === 0 ? (
        <p className="text-gray-500">
          {search
            ? `No vehicles match “${search}”.`
            : "No vehicles yet. Add your first one!"}
        </p>
      ) : (
        <ul className="space-y-3">
          {/* Turn each vehicle in the array into a list item. `key` helps React. */}
          {vehicles.map((vehicle) => (
            <li key={vehicle.id}>
              <Link
                href={`/vehicles/${vehicle.id}`}
                className="block bg-white border rounded p-4 hover:shadow"
              >
                <p className="font-semibold">
                  {vehicle.year} {vehicle.make} {vehicle.model}
                </p>
                <p className="text-sm text-gray-500">{vehicle.engineCode}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
