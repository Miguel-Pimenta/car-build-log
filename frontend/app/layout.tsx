import type { Metadata } from "next";
import Link from "next/link";
import { Providers } from "./providers";
import "./globals.css";
import LogoutButton from "@/components/LogoutButton";

export const metadata: Metadata = {
  title: "Car Build Log",
  description: "Track your vehicles, modifications, and dyno results.",
};

// No "use client" here: the root layout is a Server Component. It can still render
// client-component "islands" (LogoutButton, Providers) — only those ship JS to the browser.
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <header className="bg-white border-b">
          <div className="max-w-3xl mx-auto px-4 py-4">
            <Link href="/" className="text-xl font-bold">
              🚗 Car Build Log
            </Link>
            <LogoutButton /> {/* client island: needs onClick, so it's a Client Component */}
          </div>
        </header>

        <main className="max-w-3xl mx-auto px-4 py-6">
          {/* Providers must wrap children so every page shares one TanStack Query cache */}
          <Providers>{children}</Providers>
        </main>
      </body>
    </html>
  );
}
