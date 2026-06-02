"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  BarChart3Icon,
  BoxesIcon,
  CalendarRangeIcon,
  ClipboardListIcon,
  HomeIcon,
  Layers3Icon,
  LogOutIcon,
  ShieldIcon,
  TagsIcon,
  TrophyIcon,
  WarehouseIcon,
} from "lucide-react"

import { Button, buttonVariants } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { useAuth } from "@/features/auth/auth-context"
import { cn } from "@/lib/utils"

const statsItems = [
  { href: "/admin", label: "Doanh thu", icon: BarChart3Icon },
  // { href: "/admin/doanh-thu-theo-thang", label: "DT theo tháng", icon: CalendarRangeIcon },
  { href: "/admin/san-pham-ban-chay", label: "SP bán chạy", icon: TrophyIcon },
  { href: "/admin/don-hang-nhieu-kho", label: "Đơn nhiều kho", icon: WarehouseIcon },
]

const mgmtItems = [
  { href: "/admin/orders", label: "Đơn hàng", icon: ClipboardListIcon },
  { href: "/admin/products", label: "Sản phẩm", icon: BoxesIcon },
  { href: "/admin/categories", label: "Danh mục", icon: Layers3Icon },
  { href: "/admin/brands", label: "Thương hiệu", icon: TagsIcon },
]

export function AdminSidebar() {
  const pathname = usePathname()
  const { user, logout } = useAuth()

  return (
    <aside className="sticky top-0 hidden h-screen border-r bg-slate-950 text-slate-100 lg:flex lg:flex-col">
      <div className="p-5">
        <Link href="/admin" className="flex items-center gap-3">
          <span className="grid size-10 place-items-center rounded-xl bg-emerald-400 text-slate-950">
            <ShieldIcon className="size-5" />
          </span>
          <span>
            <span className="block text-sm font-semibold uppercase tracking-[0.2em] text-emerald-200">
              ADMIN
            </span>
            <span className="font-heading text-xl font-semibold">Admin</span>
          </span>
        </Link>
      </div>

      <nav className="grid gap-1 px-3">
        <p className="mb-1 px-3 text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500">
          Thống kê
        </p>
        {statsItems.map((item) => {
          const Icon = item.icon
          const active =
            item.href === "/admin"
              ? pathname === item.href
              : pathname.startsWith(item.href)

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/10 hover:text-white",
                active && "bg-emerald-400 text-slate-950 hover:bg-emerald-300 hover:text-slate-950"
              )}
            >
              <Icon className="size-4" />
              {item.label}
            </Link>
          )
        })}

        <Separator className="my-2 bg-white/10" />

        <p className="mb-1 px-3 text-[10px] font-semibold uppercase tracking-[0.2em] text-slate-500">
          Quản lý
        </p>
        {mgmtItems.map((item) => {
          const Icon = item.icon
          const active = pathname.startsWith(item.href)

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-slate-300 transition hover:bg-white/10 hover:text-white",
                active && "bg-emerald-400 text-slate-950 hover:bg-emerald-300 hover:text-slate-950"
              )}
            >
              <Icon className="size-4" />
              {item.label}
            </Link>
          )
        })}
      </nav>

      <div className="mt-auto p-4">
        <Separator className="mb-4 bg-white/10" />
        <div className="rounded-2xl bg-white/8 p-3">
          <p className="text-sm font-semibold">{user?.hoTen ?? "Admin"}</p>
          <p className="truncate text-xs text-slate-400">{user?.email}</p>
          <div className="mt-3 grid grid-cols-2 gap-2">
            <Link
              href="/"
              className={cn(
                buttonVariants({ variant: "outline", size: "sm" }),
                "border-white/10 bg-transparent text-slate-100 hover:bg-white/10"
              )}
            >
              <HomeIcon className="size-4" />
              Trang chủ
            </Link>
            <Button type="button" variant="destructive" size="sm" onClick={logout}>
              <LogOutIcon className="size-4" />
              Thoát
            </Button>
          </div>
        </div>
      </div>
    </aside>
  )
}
