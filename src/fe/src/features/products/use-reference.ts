import { useQuery } from "@tanstack/react-query"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { mainApiClient } from "@/lib/main-api-client"
import type { Brand, Category } from "@/types/domain"

export function useCategories() {
  return useQuery({
    queryKey: QK.categories(),
    queryFn: () => mainApiClient.get<Category[]>(MAIN_ENDPOINTS.CATEGORIES),
    staleTime: Infinity,
  })
}

export function useBrands() {
  return useQuery({
    queryKey: QK.brands(),
    queryFn: () => mainApiClient.get<Brand[]>(MAIN_ENDPOINTS.BRANDS),
    staleTime: Infinity,
  })
}
