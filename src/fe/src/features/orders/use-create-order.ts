"use client"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import type { RegionCode } from "@/constants/regions"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { isApiError } from "@/lib/api-error"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type { DonHang, TaoDonHangRequest } from "@/types/order"

function assertRegionalSession(
  region: RegionCode | null,
  token: string | null
): asserts region is RegionCode {
  if (!token) {
    throw new Error("Ban can dang nhap de dat hang.")
  }

  if (!region) {
    throw new Error("Tai khoan chua gan khu vuc hop le.")
  }
}

function getOrderErrorMessage(error: unknown) {
  if (!isApiError(error)) {
    return "Khong tao duoc don hang"
  }

  if (error.errorCode === "OUT_OF_STOCK") {
    return "Mot so san pham khong con du hang. Vui long tai lai gio hang."
  }

  if (error.errorCode === "CART_EMPTY") {
    return "Gio hang trong. Vui long them san pham truoc khi dat."
  }

  if (error.errorCode === "PAYMENT_NOT_SUPPORTED") {
    return "Hien chi ho tro thanh toan COD."
  }

  return error.message
}

export function useCreateOrder() {
  const { token, region } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: TaoDonHangRequest) => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).post<DonHang>(
        REGIONAL_ENDPOINTS.ORDERS,
        input
      )
    },
    onSuccess: (order) => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.cart(region) })
        queryClient.invalidateQueries({ queryKey: QK.orders(region) })
      }

      toast.success(`Da tao don hang ${order.maDonHang}`)
    },
    onError: (error) => {
      toast.error(getOrderErrorMessage(error))
    },
  })
}
