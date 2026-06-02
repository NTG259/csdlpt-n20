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
  PhieuXuatDetail,
  PhieuXuatSummary,
  WarehouseActionResult,
  WarehouseExportQuery,
} from "@/types/warehouse"

import { warehouseErrorMessage } from "./warehouse-error"
import { assertWarehouseSession, hasWarehouseAccess } from "./warehouse-session"

export function useWarehouseExports(params: WarehouseExportQuery) {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseExports(region ?? "unknown", params),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).get<
        PageResponse<PhieuXuatSummary>
      >(REGIONAL_ENDPOINTS.WAREHOUSE_EXPORTS, {
        query: { size: 20, sort: "ngayTao,desc", ...params },
      })
    },
    placeholderData: keepPreviousData,
  })
}

export function useWarehouseExportDetail(maPhieuXuat?: string | null) {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseExportDetail(region ?? "unknown", maPhieuXuat),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro) &&
      Boolean(maPhieuXuat),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      if (!maPhieuXuat) {
        throw new Error("Thiếu mã phiếu xuất.")
      }

      return getRegionalApiClient(region, token).get<PhieuXuatDetail>(
        REGIONAL_ENDPOINTS.WAREHOUSE_EXPORT_DETAIL(maPhieuXuat)
      )
    },
  })
}

export function useConfirmInternalExport() {
  const { token, region, user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maPhieuXuat: string) => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).post<WarehouseActionResult>(
        REGIONAL_ENDPOINTS.WAREHOUSE_CONFIRM_INTERNAL_EXPORT(maPhieuXuat)
      )
    },
    onSuccess: () => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.warehouse(region) })
      }
      toast.success("Đã xác nhận xuất nội bộ")
    },
    onError: (error) => {
      toast.error(warehouseErrorMessage(error, "Xác nhận xuất thất bại"))
    },
  })
}

export function useConfirmCustomerExport() {
  const { token, region, user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maPhieuXuat: string) => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).post<WarehouseActionResult>(
        REGIONAL_ENDPOINTS.WAREHOUSE_CONFIRM_CUSTOMER_EXPORT(maPhieuXuat)
      )
    },
    onSuccess: () => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.warehouse(region) })
      }
      toast.success("Đã xác nhận xuất giao khách")
    },
    onError: (error) => {
      toast.error(warehouseErrorMessage(error, "Xác nhận giao khách thất bại"))
    },
  })
}
