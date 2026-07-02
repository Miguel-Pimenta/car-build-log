"use client";

import { logout } from "@/lib/api";

export default function LogoutButton() {
  function handleLogout() {
    logout(); // clears the token from localStorage
    window.location.href = "/login"; // hard redirect: also wipes any cached data
  }

  return (
    <button
      onClick={handleLogout}
      className="text-sm text-gray-600 hover:underline"
    >
      Log out
    </button>
  );
}
