"use client"

import { useEffect } from "react"
import { usePathname, useRouter } from "next/navigation"

import { Loading } from "@/components/shared/loading"
import { useAuth } from "@/features/auth/auth-context"

export function RequireAdmin({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isAuthReady, user } = useAuth()
  const pathname = usePathname()
  const router = useRouter()
  const isAdmin = user?.vaiTro === "ADMIN"

  useEffect(() => {
    if (!isAuthReady) {
      return
    }

    if (!isAuthenticated) {
      router.replace(`/login?redirect=${encodeURIComponent(pathname)}`)
      return
    }

    if (!isAdmin && pathname !== "/admin/forbidden") {
      router.replace("/admin/forbidden")
    }
  }, [isAuthenticated, isAdmin, isAuthReady, pathname, router])

  if (!isAuthReady || !isAuthenticated) {
    return <Loading label="Đang kiểm tra phiên đăng nhập..." />
  }

  if (!isAdmin && pathname !== "/admin/forbidden") {
    return <Loading label="Đang kiểm tra quyền quản trị..." />
  }

  return <>{children}</>
}
