"use client"

import { useQuery } from "@tanstack/react-query"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { mainApiClient } from "@/lib/main-api-client"
import type { RevenueStatsFilter, ThongKeDoanhThu } from "@/types/admin"

export function useRevenueStats(filters: RevenueStatsFilter) {
  const { token, user } = useAuth()
  const isAdmin = user?.vaiTro === "ADMIN"

  return useQuery({
    queryKey: QK.adminStatsRevenue(filters),
    queryFn: () =>
      mainApiClient.get<ThongKeDoanhThu>(MAIN_ENDPOINTS.ADMIN_STATS_REVENUE, {
        token,
        query: { ...filters },
      }),
    enabled: Boolean(token) && isAdmin,
    staleTime: 30_000,
  })
}
