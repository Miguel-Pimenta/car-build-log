"use client";

// This file sets up TanStack Query for the whole app.
//
// Why "use client"?
//   QueryClientProvider uses React context internally, which only works in
//   client components. But every page can still be a Server Component — they
//   just need to be *children* of this provider.
//
// Why useState for QueryClient?
//   We must NOT write `const queryClient = new QueryClient()` at module scope.
//   In Next.js App Router, server-side renders can create multiple instances
//   per request if the module is evaluated more than once. Wrapping it in
//   useState guarantees one stable client per browser session.
import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

interface ProvidersProps {
  children: React.ReactNode;
}

export function Providers({ children }: ProvidersProps) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Data is considered fresh for 30 seconds — short enough to feel
            // live, long enough not to hammer the backend on every navigation.
            staleTime: 30 * 1000,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
