"use client"

import { useQuery } from "@tanstack/react-query"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type { WarehouseDashboard } from "@/types/warehouse"

import { assertWarehouseSession, hasWarehouseAccess } from "./warehouse-session"

export interface WarehouseDashboardQuery {
  fromDate?: string
  toDate?: string
}

export function useWarehouseDashboard(params: WarehouseDashboardQuery = {}) {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseDashboard(region ?? "unknown", params),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).get<WarehouseDashboard>(
        REGIONAL_ENDPOINTS.WAREHOUSE_DASHBOARD,
        { query: { ...params } }
      )
    },
    staleTime: 20_000,
  })
}
