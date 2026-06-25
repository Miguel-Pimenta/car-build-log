import type { Metadata } from "next";
import Link from "next/link";
import { Providers } from "./providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "Car Build Log",
  description: "Track your vehicles, modifications, and dyno results.",
};

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
          </div>
        </header>

        <main className="max-w-3xl mx-auto px-4 py-6">
          <Providers>{children}</Providers>
        </main>
      </body>
    </html>
  );
}
