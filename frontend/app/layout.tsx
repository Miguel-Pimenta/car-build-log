import type { Metadata } from "next";
import Link from "next/link";
import { Providers } from "./providers";
import "./globals.css";

// `metadata` sets the browser tab title. Only layouts/pages can export this.
export const metadata: Metadata = {
  title: "Car Build Log",
  description: "Track your vehicles, modifications, and dyno results.",
};

// The layout wraps every page. `children` is whichever page is currently shown.
// RootLayout itself stays a Server Component — only the <Providers> wrapper
// inside it needs to be a client component (because QueryClientProvider uses
// React context). This keeps the HTML shell server-rendered.
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        {/* A simple header shown on every page */}
        <header className="bg-white border-b">
          <div className="max-w-3xl mx-auto px-4 py-4">
            <Link href="/" className="text-xl font-bold">
              🚗 Car Build Log
            </Link>
          </div>
        </header>

        {/* Providers wraps children so TanStack Query is available everywhere. */}
        <main className="max-w-3xl mx-auto px-4 py-6">
          <Providers>{children}</Providers>
        </main>
      </body>
    </html>
  );
}
