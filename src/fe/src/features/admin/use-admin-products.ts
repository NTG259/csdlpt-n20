"use client"

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"
import { toast } from "sonner"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { mainApiClient } from "@/lib/main-api-client"
import type { PageResponse } from "@/types/api"
import type { ProductDetail, ProductListItem } from "@/types/domain"
import type { ProductUpsert } from "@/types/admin"

import { adminErrorMessage } from "./form-errors"

export interface AdminProductQuery {
  page?: number
  size?: number
  sort?: string
  maDanhMuc?: string
  maThuongHieu?: string
  trangThai?: boolean
}

export function useAdminProducts(params: AdminProductQuery) {
  const { token, user } = useAuth()
  const isAdmin = user?.vaiTro === "ADMIN"

  return useQuery({
    queryKey: QK.adminProducts(params),
    queryFn: () =>
      mainApiClient.get<PageResponse<ProductListItem>>(MAIN_ENDPOINTS.PRODUCTS, {
        token,
        query: {
          size: 20,
          ...params,
        },
      }),
    enabled: Boolean(token) && isAdmin,
    placeholderData: keepPreviousData,
  })
}

export function useCreateProduct() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: ProductUpsert) =>
      mainApiClient.post<ProductDetail>(MAIN_ENDPOINTS.PRODUCT_CREATE, input, {
        token,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã tạo sản phẩm")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Tạo sản phẩm thất bại")),
  })
}

export function useUpdateProduct() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ maSP, data }: { maSP: string; data: ProductUpsert }) =>
      mainApiClient.put<ProductDetail>(MAIN_ENDPOINTS.PRODUCT_UPDATE(maSP), data, {
        token,
      }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      queryClient.invalidateQueries({ queryKey: QK.product(variables.maSP) })
      toast.success("Đã cập nhật sản phẩm")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Cập nhật sản phẩm thất bại")),
  })
}

export function useDeleteProduct() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maSP: string) =>
      mainApiClient.delete<void>(MAIN_ENDPOINTS.PRODUCT_DELETE(maSP), {
        token,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã xoá sản phẩm")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Xoá sản phẩm thất bại")),
  })
}
