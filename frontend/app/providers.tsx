"use client"; // needs useState + provides React context, so it must run on the client

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

interface ProvidersProps {
  children: React.ReactNode;
}

export function Providers({ children }: ProvidersProps) {
  // useState with a lazy initializer creates the client exactly ONCE and keeps the
  // same instance across re-renders (a bare `new QueryClient()` would rebuild it and
  // throw away the cache every render).
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Data is "fresh" for 30s; within that window Query serves cache without refetching.
            staleTime: 30 * 1000,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
