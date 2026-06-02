"use client"

import { useQuery } from "@tanstack/react-query"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { mainApiClient } from "@/lib/main-api-client"
import type {
  DoanhThuTheoThang,
  DonHangNhieuKho,
  RevenueStatsFilter,
  SanPhamBanChay,
  ThongKeDoanhThu,
} from "@/types/admin"

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

export function useRevenueByMonth() {
  const { token, user } = useAuth()
  const isAdmin = user?.vaiTro === "ADMIN"

  return useQuery({
    queryKey: QK.adminStatsRevenueByMonth(),
    queryFn: () =>
      mainApiClient.get<DoanhThuTheoThang[]>(
        MAIN_ENDPOINTS.ADMIN_STATS_REVENUE_BY_MONTH,
        { token }
      ),
    enabled: Boolean(token) && isAdmin,
    staleTime: 60_000,
  })
}

export function useTopProducts() {
  const { token, user } = useAuth()
  const isAdmin = user?.vaiTro === "ADMIN"

  return useQuery({
    queryKey: QK.adminStatsTopProducts(),
    queryFn: () =>
      mainApiClient.get<SanPhamBanChay[]>(MAIN_ENDPOINTS.ADMIN_STATS_TOP_PRODUCTS, {
        token,
      }),
    enabled: Boolean(token) && isAdmin,
    staleTime: 60_000,
  })
}

export function useMultiWarehouseOrders() {
  const { token, user } = useAuth()
  const isAdmin = user?.vaiTro === "ADMIN"

  return useQuery({
    queryKey: QK.adminStatsMultiWarehouse(),
    queryFn: () =>
      mainApiClient.get<DonHangNhieuKho[]>(
        MAIN_ENDPOINTS.ADMIN_STATS_MULTI_WAREHOUSE,
        { token }
      ),
    enabled: Boolean(token) && isAdmin,
    staleTime: 60_000,
  })
}
