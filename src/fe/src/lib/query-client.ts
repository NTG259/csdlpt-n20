import { QueryClient } from "@tanstack/react-query"

import { isApiError } from "@/lib/api-error"

export function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60_000,
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => {
          if (isApiError(error) && error.status < 500) {
            return false
          }

          return failureCount < 1
        },
      },
      mutations: {
        retry: false,
      },
    },
  })
}
