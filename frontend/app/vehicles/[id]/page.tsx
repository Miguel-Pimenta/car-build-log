"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  getVehicle,
  getVehicleSummary,
  getModifications,
  getDynoResults,
  createModification,
  deleteModification,
  createDynoResult,
  deleteVehicle,
} from "@/lib/api";
import StatusBadge from "@/components/StatusBadge";
import { MODIFICATION_CATEGORIES } from "@/lib/types";
import type {
  VehicleResponse,
  VehicleSummaryResponse,
  ModificationResponse,
  DynoResponse,
  ModificationCategory,
} from "@/lib/types";

export default function VehicleDetailPage() {
  const params = useParams(); // reads the {id} from the URL
  const id = params.id as string;
  const router = useRouter();

  // One piece of state per thing we load from the backend.
  const [vehicle, setVehicle] = useState<VehicleResponse | null>(null);
  const [summary, setSummary] = useState<VehicleSummaryResponse | null>(null);
  const [mods, setMods] = useState<ModificationResponse[]>([]);
  const [dynos, setDynos] = useState<DynoResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Bumping `refreshKey` re-runs the effect below, which re-loads everything.
  // We call reload() after adding/deleting so the screen stays up to date.
  const [refreshKey, setRefreshKey] = useState(0);
  function reload() {
    setRefreshKey((key) => key + 1);
  }

  useEffect(() => {
    async function load() {
      try {
        setVehicle(await getVehicle(id));
        setSummary(await getVehicleSummary(id));
        setMods(await getModifications(id));
        setDynos(await getDynoResults(id));
      } catch (err) {
        setError(err instanceof Error ? err.message : "Could not load vehicle");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id, refreshKey]);

  async function handleDeleteVehicle() {
    if (!window.confirm("Delete this vehicle and everything in it?")) return;
    await deleteVehicle(id);
    router.push("/"); // back to the list
  }

  async function handleDeleteMod(modId: string) {
    await deleteModification(modId);
    reload();
  }

  if (loading) return <p>Loading…</p>;
  if (error) return <p className="text-red-600">{error}</p>;
  if (!vehicle) return null;

  return (
    <div className="space-y-8">
      {/* ---- Vehicle header ---- */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold">
              {vehicle.year} {vehicle.make} {vehicle.model}
            </h1>
            <StatusBadge status={vehicle.status} />
          </div>
          <p className="text-gray-500">{vehicle.engineCode}</p>
          {vehicle.notes && (
            <p className="mt-1 text-gray-600">{vehicle.notes}</p>
          )}
        </div>
        <div className="flex gap-2 shrink-0">
          <Link
            href={`/vehicles/${id}/edit`}
            className="border px-3 py-1.5 rounded"
          >
            Edit
          </Link>
          <button
            onClick={handleDeleteVehicle}
            className="border border-red-300 text-red-700 px-3 py-1.5 rounded"
          >
            Delete
          </button>
        </div>
      </div>

      {/* ---- Summary ---- */}
      {summary && (
        <section>
          <h2 className="text-lg font-semibold mb-2">Summary</h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <Stat label="Modifications" value={summary.totalModifications} />
            <Stat label="Total spend" value={formatMoney(summary.totalSpend)} />
            <Stat
              label="Power"
              value={
                summary.currentPowerHp !== null
                  ? `${summary.currentPowerHp} HP`
                  : "—"
              }
            />
            <Stat
              label="Torque"
              value={
                summary.currentTorqueNm !== null
                  ? `${summary.currentTorqueNm} Nm`
                  : "—"
              }
            />
          </div>
        </section>
      )}

      {/* ---- Modifications ---- */}
      <section>
        <h2 className="text-lg font-semibold mb-2">
          Modifications ({mods.length})
        </h2>
        {mods.length === 0 ? (
          <p className="text-gray-500 mb-3">No modifications yet.</p>
        ) : (
          <ul className="space-y-2 mb-3">
            {mods.map((mod) => (
              <li
                key={mod.id}
                className="bg-white border rounded p-3 flex justify-between items-start gap-3"
              >
                <div>
                  <p className="font-medium">
                    {mod.name}{" "}
                    <span className="text-xs text-gray-500">
                      ({mod.category})
                    </span>
                  </p>
                  <p className="text-sm text-gray-500">
                    {formatMoney(mod.cost)} · {mod.installedAt} ·{" "}
                    {mod.mileageKmAtInstall} km
                  </p>
                </div>
                <button
                  onClick={() => handleDeleteMod(mod.id)}
                  className="text-red-600 text-sm shrink-0"
                >
                  Delete
                </button>
              </li>
            ))}
          </ul>
        )}
        <AddModificationForm vehicleId={id} onAdded={reload} />
      </section>

      {/* ---- Dyno results ---- */}
      <section>
        <h2 className="text-lg font-semibold mb-2">
          Dyno results ({dynos.length})
        </h2>
        {dynos.length === 0 ? (
          <p className="text-gray-500 mb-3">No dyno runs yet.</p>
        ) : (
          <ul className="space-y-2 mb-3">
            {dynos.map((dyno) => (
              <li key={dyno.id} className="bg-white border rounded p-3">
                <p className="font-medium">
                  {dyno.powerHp} HP · {dyno.torqueNm} Nm
                </p>
                <p className="text-sm text-gray-500">
                  {dyno.measuredAt}
                  {dyno.notes ? ` · ${dyno.notes}` : ""}
                </p>
              </li>
            ))}
          </ul>
        )}
        <AddDynoForm vehicleId={id} onAdded={reload} />
      </section>
    </div>
  );
}

// ---- Small helpers used only on this page ----

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="bg-white border rounded p-3">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-xl font-bold">{value}</p>
    </div>
  );
}

function formatMoney(value: number): string {
  return "€" + Number(value).toFixed(2);
}

// Form to add a modification. It keeps its own state for the inputs, and calls
// onAdded() after a successful save so the parent page reloads its lists.
function AddModificationForm({
  vehicleId,
  onAdded,
}: {
  vehicleId: string;
  onAdded: () => void;
}) {
  const [category, setCategory] = useState<ModificationCategory>("ENGINE");
  const [name, setName] = useState("");
  const [cost, setCost] = useState("");
  const [installedAt, setInstalledAt] = useState("");
  const [mileage, setMileage] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    try {
      await createModification(vehicleId, {
        category,
        name,
        cost: Number(cost),
        installedAt,
        mileageKmAtInstall: Number(mileage),
      });
      // Clear the inputs after a successful add.
      setName("");
      setCost("");
      setInstalledAt("");
      setMileage("");
      onAdded();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Could not add modification",
      );
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white border rounded p-3 space-y-2"
    >
      <p className="font-medium text-sm">Add a modification</p>
      {error && <p className="text-red-600 text-sm">{error}</p>}
      <div className="grid sm:grid-cols-2 gap-2">
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value as ModificationCategory)}
          className="border rounded px-2 py-1.5"
        >
          {MODIFICATION_CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <input
          placeholder="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
        <input
          placeholder="Cost (€)"
          type="number"
          value={cost}
          onChange={(e) => setCost(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
        <input
          type="date"
          value={installedAt}
          onChange={(e) => setInstalledAt(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
        <input
          placeholder="Mileage (km)"
          type="number"
          value={mileage}
          onChange={(e) => setMileage(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
      </div>
      <button className="bg-blue-600 text-white px-3 py-1.5 rounded text-sm">
        Add
      </button>
    </form>
  );
}

function AddDynoForm({
  vehicleId,
  onAdded,
}: {
  vehicleId: string;
  onAdded: () => void;
}) {
  const [powerHp, setPowerHp] = useState("");
  const [torqueNm, setTorqueNm] = useState("");
  const [measuredAt, setMeasuredAt] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    try {
      await createDynoResult(vehicleId, {
        powerHp: Number(powerHp),
        torqueNm: Number(torqueNm),
        measuredAt,
        notes: notes || undefined,
      });
      setPowerHp("");
      setTorqueNm("");
      setMeasuredAt("");
      setNotes("");
      onAdded();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Could not add dyno result",
      );
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white border rounded p-3 space-y-2"
    >
      <p className="font-medium text-sm">Add a dyno result</p>
      {error && <p className="text-red-600 text-sm">{error}</p>}
      <div className="grid sm:grid-cols-2 gap-2">
        <input
          placeholder="Power (HP)"
          type="number"
          value={powerHp}
          onChange={(e) => setPowerHp(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
        <input
          placeholder="Torque (Nm)"
          type="number"
          value={torqueNm}
          onChange={(e) => setTorqueNm(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
        <input
          type="date"
          value={measuredAt}
          onChange={(e) => setMeasuredAt(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
        <input
          placeholder="Notes"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          className="border rounded px-2 py-1.5"
        />
      </div>
      <button className="bg-blue-600 text-white px-3 py-1.5 rounded text-sm">
        Add
      </button>
    </form>
  );
}
