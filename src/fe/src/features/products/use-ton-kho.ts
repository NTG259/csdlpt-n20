import { useQuery } from "@tanstack/react-query"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { mainApiClient } from "@/lib/main-api-client"
import type { TonKhoHeThong } from "@/types/ton-kho"

export function useTonKhoHeThong(maSP: string, enabled = true) {
  return useQuery({
    queryKey: QK.tonKho(maSP),
    enabled: Boolean(maSP) && enabled,
    staleTime: 30_000,
    queryFn: () =>
      mainApiClient.get<TonKhoHeThong>(MAIN_ENDPOINTS.PRODUCT_TON_KHO(maSP)),
  })
}
