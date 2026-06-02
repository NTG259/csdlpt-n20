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
  ReadyToShipOrder,
  WarehouseActionResult,
  WarehouseReadyOrderQuery,
} from "@/types/warehouse"

import { warehouseErrorMessage } from "./warehouse-error"
import { assertWarehouseSession, hasWarehouseAccess } from "./warehouse-session"

export function useReadyToShipOrders(params: WarehouseReadyOrderQuery) {
  const { token, region, user, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.warehouseReadyOrders(region ?? "unknown", params),
    enabled:
      isAuthenticated &&
      Boolean(token) &&
      Boolean(region) &&
      hasWarehouseAccess(user?.vaiTro),
    queryFn: () => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).get<
        PageResponse<ReadyToShipOrder>
      >(REGIONAL_ENDPOINTS.WAREHOUSE_READY_TO_SHIP, {
        query: { size: 20, sort: "ngayDat,desc", ...params },
      })
    },
    placeholderData: keepPreviousData,
  })
}

export function useCreateCustomerExportSlip() {
  const { token, region, user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maDonHang: string) => {
      assertWarehouseSession(region, token, user?.vaiTro)

      return getRegionalApiClient(region, token).post<WarehouseActionResult>(
        REGIONAL_ENDPOINTS.WAREHOUSE_CREATE_CUSTOMER_EXPORT(maDonHang)
      )
    },
    onSuccess: () => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.warehouse(region) })
      }
      toast.success("Đã tạo phiếu xuất giao khách")
    },
    onError: (error) => {
      toast.error(warehouseErrorMessage(error, "Tạo phiếu giao khách thất bại"))
    },
  })
}
