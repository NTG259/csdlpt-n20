"use client"

import { useMutation, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { mainApiClient } from "@/lib/main-api-client"
import type { CategoryUpsert } from "@/types/admin"
import type { Category } from "@/types/domain"

import { adminErrorMessage } from "./form-errors"

export function useCreateCategory() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CategoryUpsert) =>
      mainApiClient.post<Category>(MAIN_ENDPOINTS.CATEGORIES, input, { token }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.categories() })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã tạo danh mục")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Tạo danh mục thất bại")),
  })
}

export function useUpdateCategory() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      maDanhMuc,
      data,
    }: {
      maDanhMuc: string
      data: CategoryUpsert
    }) =>
      mainApiClient.put<Category>(MAIN_ENDPOINTS.CATEGORY_ITEM(maDanhMuc), data, {
        token,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.categories() })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã cập nhật danh mục")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Cập nhật danh mục thất bại")),
  })
}

export function useDeleteCategory() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (maDanhMuc: string) =>
      mainApiClient.delete<void>(MAIN_ENDPOINTS.CATEGORY_ITEM(maDanhMuc), {
        token,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QK.categories() })
      queryClient.invalidateQueries({ queryKey: ["products"] })
      toast.success("Đã xoá danh mục")
    },
    onError: (error) =>
      toast.error(adminErrorMessage(error, "Xoá danh mục thất bại")),
  })
}
