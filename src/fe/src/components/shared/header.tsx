"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  ClipboardListIcon,
  HomeIcon,
  LogInIcon,
  LogOutIcon,
  PackageSearchIcon,
  ShieldIcon,
  ShoppingCartIcon,
} from "lucide-react"

import { REGION_LABEL } from "@/constants/regions"
import { useAuth } from "@/features/auth/auth-context"
import { cn } from "@/lib/utils"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"

const navItems = [
  { href: "/", label: "Trang chủ", icon: HomeIcon },
  { href: "/products", label: "Sản phẩm", icon: PackageSearchIcon },
  { href: "/cart", label: "Giỏ hàng", icon: ShoppingCartIcon },
  { href: "/orders", label: "Đơn hàng", icon: ClipboardListIcon },
]

export function Header() {
  const pathname = usePathname()
  const { user, region, isAuthenticated, logout } = useAuth()
  const isAdminRoute = pathname.startsWith("/admin")
  const isAdmin = user?.vaiTro === "ADMIN"

  if (isAdminRoute) {
    return null
  }

  return (
    <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
      <div className="mx-auto flex min-h-16 w-full max-w-6xl flex-col gap-3 px-4 py-3 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:gap-6">
        <div className="flex items-center justify-between gap-3">
          <Link href="/" className="flex items-center gap-2 font-semibold">
            <span className="flex size-9 items-center justify-center rounded-md bg-primary text-sm font-bold text-primary-foreground">
              SM
            </span>
            <span>Tech Shop</span>
          </Link>
        </div>

        <nav className="flex flex-wrap items-center gap-1.5">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive =
              item.href === "/"
                ? pathname === item.href
                : pathname.startsWith(item.href)

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  buttonVariants({ variant: "ghost", size: "sm" }),
                  "gap-2",
                  isActive && "bg-muted text-foreground"
                )}
              >
                <Icon className="size-4" />
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div className="flex flex-wrap items-center gap-2">
          {isAuthenticated ? (
            <>
              {isAdmin && (
                <Link
                  href="/admin"
                  className={cn(
                    buttonVariants({ variant: "secondary", size: "sm" }),
                    "gap-2"
                  )}
                >
                  <ShieldIcon className="size-4" />
                  Admin
                </Link>
              )}
              <div className="flex min-w-0 items-center gap-2 rounded-md border bg-muted/40 px-3 py-1.5 text-sm">
                <span className="max-w-44 truncate font-medium">
                  {user?.hoTen}
                </span>
                {region ? (
                  <Badge variant="secondary">{REGION_LABEL[region]}</Badge>
                ) : (
                  <Badge variant="outline">Chưa rõ vùng</Badge>
                )}
              </div>
              <Button type="button" variant="outline" size="sm" onClick={logout}>
                <LogOutIcon className="size-4" />
                Đăng xuất
              </Button>
            </>
          ) : (
            <Link
              href="/login"
              className={cn(buttonVariants({ variant: "default", size: "sm" }), "gap-2")}
            >
              <LogInIcon className="size-4" />
              Đăng nhập
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
