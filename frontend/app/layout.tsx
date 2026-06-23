import type { Metadata } from 'next';
import Link from 'next/link';
import './globals.css';

// `metadata` sets the browser tab title. Only layouts/pages can export this.
export const metadata: Metadata = {
  title: 'Car Build Log',
  description: 'Track your vehicles, modifications, and dyno results.',
};

// The layout wraps every page. `children` is whichever page is currently shown.
export default function RootLayout({ children }: { children: React.ReactNode }) {
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

        <main className="max-w-3xl mx-auto px-4 py-6">{children}</main>
      </body>
    </html>
  );
}
