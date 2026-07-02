"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { register } from "@/lib/api";

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await register({ username, email, password, name: name || undefined });
      router.push("/"); // register() already stored the token
      router.refresh();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
      setSubmitting(false);
    }
  }

  return (
    <div className="max-w-sm mx-auto">
      <h1 className="text-2xl font-bold mb-4">Create an account</h1>
      <form
        onSubmit={handleSubmit}
        className="space-y-3 bg-white border rounded p-4"
      >
        {error && <p className="text-red-600 text-sm">{error}</p>}
        <label className="block">
          <span className="block text-sm font-medium mb-1">Username</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-full border rounded px-3 py-2"
          />
        </label>
        <label className="block">
          <span className="block text-sm font-medium mb-1">Email</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full border rounded px-3 py-2"
          />
        </label>
        <label className="block">
          <span className="block text-sm font-medium mb-1">Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="At least 8 characters"
            className="w-full border rounded px-3 py-2"
          />
        </label>
        <label className="block">
          <span className="block text-sm font-medium mb-1">
            Name (optional)
          </span>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full border rounded px-3 py-2"
          />
        </label>
        <button
          type="submit"
          disabled={submitting}
          className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50"
        >
          {submitting ? "Creating…" : "Create account"}
        </button>
      </form>
      <p className="text-sm text-gray-600 mt-3">
        Already have an account?{" "}
        <Link href="/login" className="text-blue-600 hover:underline">
          Log in
        </Link>
      </p>
    </div>
  );
}
