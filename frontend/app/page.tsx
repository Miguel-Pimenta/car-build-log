'use client'; // This component runs in the browser, so it can use React hooks.

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { getVehicles } from '@/lib/api';
import type { VehicleResponse } from '@/lib/types';

export default function HomePage() {
  // `useState` gives us a value and a function to change it. When we change it,
  // React re-renders this component. We track three things:
  const [vehicles, setVehicles] = useState<VehicleResponse[]>([]); // the data
  const [loading, setLoading] = useState(true); // are we still loading?
  const [error, setError] = useState(''); // any error message

  // `useEffect` with an empty list [] runs ONCE, right after the first render.
  // It's the usual place to load data from an API.
  useEffect(() => {
    getVehicles()
      .then((page) => setVehicles(page.content))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading…</p>;
  if (error) return <p className="text-red-600">Could not load vehicles: {error}</p>;

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">My Vehicles</h1>
        <Link href="/vehicles/new" className="bg-blue-600 text-white px-4 py-2 rounded">
          + Add vehicle
        </Link>
      </div>

      {vehicles.length === 0 ? (
        <p className="text-gray-500">No vehicles yet. Add your first one!</p>
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
