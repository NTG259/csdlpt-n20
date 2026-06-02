"use client"

import { useQuery } from "@tanstack/react-query"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import type { RegionCode } from "@/constants/regions"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type { PageResponse } from "@/types/api"
import type { DonHang, DonHangSummary } from "@/types/order"

function assertRegionalSession(
  region: RegionCode | null,
  token: string | null
): asserts region is RegionCode {
  if (!token) {
    throw new Error("Ban can dang nhap de xem don hang.")
  }

  if (!region) {
    throw new Error("Tai khoan chua gan khu vuc hop le.")
  }
}

export function useOrders(page: number, size = 10) {
  const { token, region, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: [...QK.orders(region ?? "unknown"), page, size],
    enabled: isAuthenticated && Boolean(token) && Boolean(region),
    queryFn: () => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).get<PageResponse<DonHangSummary>>(
        REGIONAL_ENDPOINTS.ORDERS,
        { query: { page, size, sort: "ngayDat,desc" } }
      )
    },
  })
}

export function useOrderDetail(maDonHang?: string | null) {
  const { token, region, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: [...QK.orders(region ?? "unknown"), "detail", maDonHang],
    enabled: isAuthenticated && Boolean(token) && Boolean(region) && Boolean(maDonHang),
    queryFn: () => {
      assertRegionalSession(region, token)

      if (!maDonHang) {
        throw new Error("Thieu ma don hang.")
      }

      return getRegionalApiClient(region, token).get<DonHang>(
        REGIONAL_ENDPOINTS.ORDER_DETAIL(maDonHang)
      )
    },
  })
}
