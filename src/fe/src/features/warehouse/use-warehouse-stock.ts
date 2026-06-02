"use client"

import { keepPreviousData, useQuery } from "@tanstack/react-query"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type { PageResponse } from "@/types/api"
import type { WarehouseStockItem, WarehouseStockQuery } from "@/types/warehouse"

import { assertWarehouseSession, hasWarehouseAccess } from "./warehouse-session"

export function useWarehouseStock(params: WarehouseStockQuery) {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseStock(region ?? "unknown", params),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).get<
        PageResponse<WarehouseStockItem>
      >(REGIONAL_ENDPOINTS.WAREHOUSE_STOCK, {
        query: { size: 20, sort: "tenSP,asc", ...params },
      })
    },
    placeholderData: keepPreviousData,
  })
}
