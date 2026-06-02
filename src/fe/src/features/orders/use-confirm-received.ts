"use client"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import type { RegionCode } from "@/constants/regions"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { isApiError } from "@/lib/api-error"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type { DonHang } from "@/types/order"

function assertRegionalSession(
  region: RegionCode | null,
  token: string | null
): asserts region is RegionCode {
  if (!token) {
    throw new Error("Ban can dang nhap de xac nhan don hang.")
  }

  if (!region) {
    throw new Error("Tai khoan chua gan khu vuc hop le.")
  }
}

function getConfirmReceivedErrorMessage(error: unknown) {
  if (!isApiError(error)) {
    return "Khong xac nhan duoc nhan hang."
  }

  if (error.errorCode === "INVALID_ORDER_STATE") {
    return "Don khong o trang thai dang giao."
  }

  if (error.errorCode === "ACCESS_DENIED") {
    return "Don nay khong thuoc tai khoan cua ban."
  }

  if (error.errorCode === "ORDER_NOT_FOUND") {
    return "Khong tim thay don hang."
  }

  return error.message
}

export function useConfirmReceived() {
  const { token, region } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maDonHang: string) => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).post<DonHang>(
        REGIONAL_ENDPOINTS.ORDER_CONFIRM_RECEIVED(maDonHang)
      )
    },
    onSuccess: (order) => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.orders(region) })
      }

      toast.success(`Da xac nhan nhan hang don ${order.maDonHang}`)
    },
    onError: (error) => {
      toast.error(getConfirmReceivedErrorMessage(error))
    },
  })
}
