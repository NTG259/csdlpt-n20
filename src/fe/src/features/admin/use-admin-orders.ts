"use client"

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { toast } from "sonner"

import { MAIN_ENDPOINTS } from "@/constants/endpoints"
import { QK } from "@/constants/query-keys"
import { useAuth } from "@/features/auth/auth-context"
import { mainApiClient } from "@/lib/main-api-client"
import type {
  AdminOrderDetail,
  AdminOrderFilter,
  AdminOrderPage,
  UpdateOrderStatusBody,
} from "@/types/admin"

export function useAdminOrders(filters: AdminOrderFilter) {
  const { token, user } = useAuth()
  const isAdmin = user?.vaiTro === "ADMIN"

  return useQuery({
    queryKey: QK.adminOrders(filters),
    queryFn: () =>
      mainApiClient.get<AdminOrderPage>(MAIN_ENDPOINTS.ADMIN_ORDERS, {
        token,
        query: { ...filters },
      }),
    enabled: Boolean(token) && isAdmin,
    staleTime: 30_000,
  })
}

export function useAdminOrderDetail(maDonHang: string, siteNguon: string) {
  const { token, user } = useAuth()
  const isAdmin = user?.vaiTro === "ADMIN"

  return useQuery({
    queryKey: QK.adminOrderDetail(maDonHang, siteNguon),
    queryFn: () =>
      mainApiClient.get<AdminOrderDetail>(
        MAIN_ENDPOINTS.ADMIN_ORDER_DETAIL(maDonHang),
        { token, query: { siteNguon } }
      ),
    enabled:
      Boolean(token) && isAdmin && Boolean(maDonHang) && Boolean(siteNguon),
    staleTime: 30_000,
  })
}

export function useUpdateOrderStatus() {
  const { token } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      maDonHang,
      body,
    }: {
      maDonHang: string
      body: UpdateOrderStatusBody
    }) =>
      mainApiClient.patch<AdminOrderDetail>(
        MAIN_ENDPOINTS.ADMIN_ORDER_STATUS(maDonHang),
        body,
        { token }
      ),
    onSuccess: (updated) => {
      toast.success("Cập nhật trạng thái thành công")
      queryClient.invalidateQueries({ queryKey: ["admin", "orders"] })
      queryClient.setQueryData(
        QK.adminOrderDetail(updated.maDonHang, updated.siteNguon),
        updated
      )
    },
    onError: () => {
      toast.error("Cập nhật trạng thái thất bại")
    },
  })
}
