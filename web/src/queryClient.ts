import { QueryClient } from '@tanstack/react-query'

// Tuned for onSnapshot-driven caches:
//  - staleTime: Infinity       — onSnapshot listener keeps data fresh, so no refetch ever needed
//  - refetchOnWindowFocus: false — subscription is always active
//  - refetchOnMount: false       — cache is populated by the listener, not the queryFn
//  - retry: false                — the listener surfaces errors directly; no retry semantics needed
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: Infinity,
      refetchOnWindowFocus: false,
      refetchOnMount: false,
      refetchOnReconnect: false,
      retry: false,
    },
    mutations: {
      retry: false,
    },
  },
})
