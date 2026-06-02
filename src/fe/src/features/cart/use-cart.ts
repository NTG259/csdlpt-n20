"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { REGIONAL_ENDPOINTS } from "@/constants/endpoints"
import type { RegionCode } from "@/constants/regions"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { isApiError } from "@/lib/api-error"
import { getRegionalApiClient } from "@/lib/regional-api-client"
import type {
  CapNhatSoLuongRequest,
  GioHang,
  ThemVaoGioRequest,
} from "@/types/cart"

function getCartErrorMessage(error: unknown, fallback: string) {
  return isApiError(error) ? error.message : fallback
}

function assertRegionalSession(
  region: RegionCode | null,
  token: string | null
): asserts region is RegionCode {
  if (!token) {
    throw new Error("Bạn cần đăng nhập để thao tác giỏ hàng.")
  }

  if (!region) {
    throw new Error("Tài khoản chưa gán khu vực hợp lệ.")
  }
}

export function useCart() {
  const { token, region, isAuthenticated } = useAuth()

  return useQuery({
    queryKey: QK.cart(region ?? "unknown"),
    enabled: isAuthenticated && Boolean(token) && Boolean(region),
    queryFn: () => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).get<GioHang>(
        REGIONAL_ENDPOINTS.CART
      )
    },
  })
}

export function useAddToCart() {
  const { token, region } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: ThemVaoGioRequest) => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).post<GioHang>(
        REGIONAL_ENDPOINTS.CART_ITEMS,
        input
      )
    },
    onSuccess: (cart) => {
      if (region) {
        queryClient.setQueryData(QK.cart(region), cart)
      }

      toast.success("Đã thêm vào giỏ hàng")
    },
    onError: (error) => {
      toast.error(getCartErrorMessage(error, "Không thêm được vào giỏ hàng"))
    },
  })
}

export function useUpdateCartItem() {
  const { token, region } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: { maSP: string } & CapNhatSoLuongRequest) => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).put<GioHang>(
        REGIONAL_ENDPOINTS.CART_ITEM(input.maSP),
        { soLuong: input.soLuong } satisfies CapNhatSoLuongRequest
      )
    },
    onSuccess: (cart) => {
      if (region) {
        queryClient.setQueryData(QK.cart(region), cart)
      }
    },
    onError: (error) => {
      toast.error(getCartErrorMessage(error, "Không cập nhật được giỏ hàng"))
    },
  })
}

export function useRemoveCartItem() {
  const { token, region } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maSP: string) => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).delete<void>(
        REGIONAL_ENDPOINTS.CART_ITEM(maSP)
      )
    },
    onSuccess: () => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.cart(region) })
      }
    },
    onError: (error) => {
      toast.error(getCartErrorMessage(error, "Không xóa được sản phẩm"))
    },
  })
}

export function useClearCart() {
  const { token, region } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => {
      assertRegionalSession(region, token)

      return getRegionalApiClient(region, token).delete<void>(
        REGIONAL_ENDPOINTS.CART
      )
    },
    onSuccess: () => {
      if (region) {
        queryClient.invalidateQueries({ queryKey: QK.cart(region) })
      }

      toast.success("Đã xóa toàn bộ giỏ hàng")
    },
    onError: (error) => {
      toast.error(getCartErrorMessage(error, "Không xóa được giỏ hàng"))
    },
  })
}
