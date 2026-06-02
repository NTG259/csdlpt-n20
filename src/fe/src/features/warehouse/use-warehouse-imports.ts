"use client"

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { toast } from "sonner"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type { PageResponse } from "@/types/api"
import type {
  PhieuNhapDetail,
  PhieuNhapSummary,
  WarehouseActionResult,
  WarehouseImportQuery,
} from "@/types/warehouse"

import { warehouseErrorMessage } from "./warehouse-error"
import { assertWarehouseSession, hasWarehouseAccess } from "./warehouse-session"

export function useWarehouseImports(params: WarehouseImportQuery) {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseImports(region ?? "unknown", params),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).get<
        PageResponse<PhieuNhapSummary>
      >(REGIONAL_ENDPOINTS.WAREHOUSE_IMPORTS, {
        query: { size: 20, sort: "ngayNhap,desc", ...params },
      })
    },
    placeholderData: keepPreviousData,
  })
}

export function useWarehouseImportDetail(maPhieuNhap?: string | null) {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseImportDetail(region ?? "unknown", maPhieuNhap),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro) &&
      Boolean(maPhieuNhap),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      if (!maPhieuNhap) {
        throw new Error("Thiếu mã phiếu nhập.")
      }

      return getRegionalApiClient(region, token).get<PhieuNhapDetail>(
        REGIONAL_ENDPOINTS.WAREHOUSE_IMPORT_DETAIL(maPhieuNhap)
      )
    },
  })
}

export function useConfirmInternalImport() {
  const { token, region, user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maPhieuNhap: string) => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).post<WarehouseActionResult>(
        REGIONAL_ENDPOINTS.WAREHOUSE_CONFIRM_IMPORT(maPhieuNhap)
      )
    },
    onSuccess: () => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.warehouse(region) })
      }
      toast.success("Đã xác nhận nhập nội bộ")
    },
    onError: (error) => {
      toast.error(warehouseErrorMessage(error, "Xác nhận nhập thất bại"))
    },
  })
}
