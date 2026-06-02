import { useQuery } from "@tanstack/react-query"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { mainApiClient } from "@/lib/main-api-client"
import type { Region } from "@/types/domain"

export function useRegions() {
  return useQuery({
    queryKey: QK.regions(),
    queryFn: () => mainApiClient.get<Region[]>(MAIN_ENDPOINTS.REGIONS),
    staleTime: Infinity,
  })
}
