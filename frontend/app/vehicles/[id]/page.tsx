"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import StatusBadge from "@/components/StatusBadge";
import { MODIFICATION_CATEGORIES } from "@/lib/types";
import type { ModificationCategory } from "@/lib/types";
import { useVehicle } from "@/hooks/use-vehicles";
import {
  useVehicleSummary,
  useModifications,
  useDynoResults,
  useDeleteVehicle,
} from "@/hooks/use-vehicle";
import {
  useCreateModification,
  useDeleteModification,
} from "@/hooks/use-modifications";
import { useCreateDynoResult } from "@/hooks/use-dyno";

export default function VehicleDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const router = useRouter();

  // Four separate useQuery hooks replace the single useEffect that loaded
  // everything sequentially. Each query is cached independently, so when you
  // add a modification only the modifications and summary queries refetch —
  // not the vehicle header or dyno list.
  const { data: vehicle, isLoading: vehicleLoading, error: vehicleError } = useVehicle(id);
  const { data: summary } = useVehicleSummary(id);
  const { data: mods = [], isLoading: modsLoading } = useModifications(id);
  const { data: dynos = [], isLoading: dynosLoading } = useDynoResults(id);

  const deleteVehicle = useDeleteVehicle();

  async function handleDeleteVehicle() {
    if (!window.confirm("Delete this vehicle and everything in it?")) return;
    await deleteVehicle.mutateAsync(id);
    // Navigate home after deletion. The hook already removes cached entries.
    router.push("/");
  }

  // Show a loading state until at least the vehicle header is ready.
  if (vehicleLoading) return <p>Loading…</p>;
  if (vehicleError) return <p className="text-red-600">{vehicleError.message}</p>;
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
          Modifications ({modsLoading ? "…" : mods.length})
        </h2>
        {!modsLoading && mods.length === 0 ? (
          <p className="text-gray-500 mb-3">No modifications yet.</p>
        ) : (
          <ul className="space-y-2 mb-3">
            {mods.map((mod) => (
              <ModificationRow key={mod.id} mod={mod} vehicleId={id} />
            ))}
          </ul>
        )}
        <AddModificationForm vehicleId={id} />
      </section>

      {/* ---- Dyno results ---- */}
      <section>
        <h2 className="text-lg font-semibold mb-2">
          Dyno results ({dynosLoading ? "…" : dynos.length})
        </h2>
        {!dynosLoading && dynos.length === 0 ? (
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
        <AddDynoForm vehicleId={id} />
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

// Extracted into its own component so it can call useDeleteModification
// directly — hooks must be called at the top level of a component, not inside
// a .map() callback.
function ModificationRow({
  mod,
  vehicleId,
}: {
  mod: { id: string; name: string; category: string; cost: number; installedAt: string; mileageKmAtInstall: number };
  vehicleId: string;
}) {
  const deleteMod = useDeleteModification(vehicleId);

  return (
    <li className="bg-white border rounded p-3 flex justify-between items-start gap-3">
      <div>
        <p className="font-medium">
          {mod.name}{" "}
          <span className="text-xs text-gray-500">({mod.category})</span>
        </p>
        <p className="text-sm text-gray-500">
          {formatMoney(mod.cost)} · {mod.installedAt} · {mod.mileageKmAtInstall} km
        </p>
      </div>
      <button
        onClick={() => deleteMod.mutate(mod.id)}
        disabled={deleteMod.isPending}
        className="text-red-600 text-sm shrink-0 disabled:opacity-50"
      >
        {deleteMod.isPending ? "Deleting…" : "Delete"}
      </button>
    </li>
  );
}

// Form to add a modification. State lives here; after a successful save
// useMutation's onSuccess invalidates the cache — no manual reload() call needed.
function AddModificationForm({ vehicleId }: { vehicleId: string }) {
  const [category, setCategory] = useState<ModificationCategory>("ENGINE");
  const [name, setName] = useState("");
  const [cost, setCost] = useState("");
  const [installedAt, setInstalledAt] = useState("");
  const [mileage, setMileage] = useState("");

  // useCreateModification invalidates modifications + summary on success.
  const createMod = useCreateModification(vehicleId);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    await createMod.mutateAsync({
      category,
      name,
      cost: Number(cost),
      installedAt,
      mileageKmAtInstall: Number(mileage),
    });
    // Clear inputs after a successful add.
    setName("");
    setCost("");
    setInstalledAt("");
    setMileage("");
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white border rounded p-3 space-y-2"
    >
      <p className="font-medium text-sm">Add a modification</p>
      {createMod.error && (
        <p className="text-red-600 text-sm">{createMod.error.message}</p>
      )}
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
      <button
        type="submit"
        disabled={createMod.isPending}
        className="bg-blue-600 text-white px-3 py-1.5 rounded text-sm disabled:opacity-50"
      >
        {createMod.isPending ? "Adding…" : "Add"}
      </button>
    </form>
  );
}

function AddDynoForm({ vehicleId }: { vehicleId: string }) {
  const [powerHp, setPowerHp] = useState("");
  const [torqueNm, setTorqueNm] = useState("");
  const [measuredAt, setMeasuredAt] = useState("");
  const [notes, setNotes] = useState("");

  // useCreateDynoResult invalidates dyno list + summary on success.
  const createDyno = useCreateDynoResult(vehicleId);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    await createDyno.mutateAsync({
      powerHp: Number(powerHp),
      torqueNm: Number(torqueNm),
      measuredAt,
      notes: notes || undefined,
    });
    setPowerHp("");
    setTorqueNm("");
    setMeasuredAt("");
    setNotes("");
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white border rounded p-3 space-y-2"
    >
      <p className="font-medium text-sm">Add a dyno result</p>
      {createDyno.error && (
        <p className="text-red-600 text-sm">{createDyno.error.message}</p>
      )}
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
      <button
        type="submit"
        disabled={createDyno.isPending}
        className="bg-blue-600 text-white px-3 py-1.5 rounded text-sm disabled:opacity-50"
      >
        {createDyno.isPending ? "Adding…" : "Add"}
      </button>
    </form>
  );
}
