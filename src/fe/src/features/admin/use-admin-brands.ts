"use client"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { mainApiClient } from "@/lib/main-api-client"
import type { BrandUpsert } from "@/types/admin"
import type { Brand } from "@/types/domain"

import { adminErrorMessage } from "./form-errors"

export function useCreateBrand() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: BrandUpsert) =>
      mainApiClient.post<Brand>(MAIN_ENDPOINTS.BRANDS, input, { token }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.brands() })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã tạo thương hiệu")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Tạo thương hiệu thất bại")),
  })
}

export function useUpdateBrand() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      maThuongHieu,
      data,
    }: {
      maThuongHieu: string
      data: BrandUpsert
    }) =>
      mainApiClient.put<Brand>(MAIN_ENDPOINTS.BRAND_ITEM(maThuongHieu), data, {
        token,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.brands() })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã cập nhật thương hiệu")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Cập nhật thương hiệu thất bại")),
  })
}

export function useDeleteBrand() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maThuongHieu: string) =>
      mainApiClient.delete<void>(MAIN_ENDPOINTS.BRAND_ITEM(maThuongHieu), {
        token,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.brands() })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã xoá thương hiệu")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Xoá thương hiệu thất bại")),
  })
}
