"use client"

import { useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"

import { Loading } from "@/components/shared/loading"
import { useAuth } from "@/features/auth/auth-context"

export function RequireWarehouse({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isAuthReady, user } = useAuth()
  const pathname = usePathname()
  const router = useRouter()
  const hasWarehouseAccess =
    user?.vaiTro === "WAREHOUSE_STAFF" || user?.vaiTro === "ADMIN"

  useEffect(() => {
    if (!isAuthReady) {
      return
    }

    if (!isAuthenticated) {
      router.replace(`/login?redirect=${encodeURIComponent(pathname)}`)
      return
    }

    if (!hasWarehouseAccess && pathname !== "/warehouse/forbidden") {
      router.replace("/warehouse/forbidden")
    }
  }, [hasWarehouseAccess, isAuthenticated, isAuthReady, pathname, router])

  if (!isAuthReady || !isAuthenticated) {
    return <Loading label="Đang kiểm tra phiên đăng nhập..." />
  }

  if (!hasWarehouseAccess && pathname !== "/warehouse/forbidden") {
    return <Loading label="Đang kiểm tra quyền kho..." />
  }

  return <>{children}</>
}
