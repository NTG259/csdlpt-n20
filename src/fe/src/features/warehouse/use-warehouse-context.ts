"use client"

import { useQuery } from "@tanstack/react-query"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type { WarehouseContext } from "@/types/warehouse"

import { assertWarehouseSession, hasWarehouseAccess } from "./warehouse-session"

export function useWarehouseContext() {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseContext(region ?? "unknown"),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).get<WarehouseContext>(
        REGIONAL_ENDPOINTS.WAREHOUSE_ME
      )
    },
    staleTime: 60_000,
  })
}
