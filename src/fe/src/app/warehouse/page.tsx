"use client"

import Link from "next/link"
import {
  BoxesIcon,
  ClipboardListIcon,
  PackageCheckIcon,
  RefreshCwIcon,
  TruckIcon,
} from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { REGION_LABEL } from "@/constants/regions"
import { useAuth } from "@/features/auth/auth-context"
import { useWarehouseContext } from "@/features/warehouse/use-warehouse-context"
import { useWarehouseDashboard } from "@/features/warehouse/use-warehouse-dashboard"
import { warehouseErrorMessage } from "@/features/warehouse/warehouse-error"
import { cn } from "@/lib/utils"

const metricLinks = [
  {
    label: "Chờ xuất nội bộ",
    href: "/warehouse/phieu-xuat?loai=noi_bo&trangThaiXuat=waiting_export",
    key: "waitingExportInternal",
    icon: ClipboardListIcon,
  },
  {
    label: "Chờ nhập nội bộ",
    href: "/warehouse/phieu-nhap?trangThaiNhap=waiting_import",
    key: "waitingImportInternal",
    icon: PackageCheckIcon,
  },
  {
    label: "Đơn sẵn sàng giao",
    href: "/warehouse/giao-khach",
    key: "readyToShipOrders",
    icon: TruckIcon,
  },
  {
    label: "Chờ xuất giao khách",
    href: "/warehouse/giao-khach",
    key: "waitingCustomerExport",
    icon: TruckIcon,
  },
  {
    label: "Sắp hết hàng",
    href: "/warehouse/ton-kho?onlyLowStock=true",
    key: "lowStockProducts",
    icon: BoxesIcon,
  },
] as const

export default function WarehouseDashboardPage() {
  const { region } = useAuth()
  const contextQuery = useWarehouseContext()
  const dashboardQuery = useWarehouseDashboard()
  const context = contextQuery.data
  const dashboard = dashboardQuery.data
  const isLoading = contextQuery.isLoading || dashboardQuery.isLoading
  const isError = contextQuery.isError || dashboardQuery.isError
  const error = contextQuery.error ?? dashboardQuery.error

  function refresh() {
    contextQuery.refetch()
    dashboardQuery.refetch()
  }

  return (
    <div className="grid gap-6">
      <section className="rounded-lg bg-slate-950 p-6 text-white shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase text-cyan-200">
              Warehouse dashboard
            </p>
            <h1 className="mt-2 font-heading text-3xl font-semibold">
              Quản lý kho
            </h1>
            <div className="mt-3 flex flex-wrap gap-2 text-sm">
              <Badge className="bg-cyan-300 text-slate-950">
                {context?.maKhoPhuTrach ?? "Chưa có kho"}
              </Badge>
              <Badge variant="outline" className="border-white/20 text-white">
                {context?.tenKho ?? "Kho phụ trách"}
              </Badge>
              {region && (
                <Badge variant="outline" className="border-white/20 text-white">
                  {REGION_LABEL[region]}
                </Badge>
              )}
            </div>
          </div>
          <Button
            type="button"
            variant="secondary"
            disabled={dashboardQuery.isFetching || contextQuery.isFetching}
            onClick={refresh}
          >
            <RefreshCwIcon className="size-4" />
            Làm mới
          </Button>
        </div>
      </section>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          {Array.from({ length: 5 }).map((_, index) => (
            <Skeleton key={index} className="h-28 rounded-lg" />
          ))}
        </div>
      ) : isError || !dashboard ? (
        <Card className="border-red-200 bg-red-50">
          <CardHeader>
            <CardTitle>Không tải được dashboard kho</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-3">
            <p className="text-sm text-red-700">
              {warehouseErrorMessage(error, "Không tải được dashboard kho")}
            </p>
            <Button type="button" className="w-fit" onClick={refresh}>
              Thử lại
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          {metricLinks.map((metric) => {
            const Icon = metric.icon
            return (
              <Link key={metric.key} href={metric.href}>
                <Card className="h-full bg-white/95 transition hover:border-cyan-300 hover:shadow-sm">
                  <CardContent className="flex items-center justify-between gap-4">
                    <div>
                      <p className="text-xs font-semibold uppercase text-muted-foreground">
                        {metric.label}
                      </p>
                      <p className="mt-2 text-3xl font-semibold">
                        {dashboard[metric.key]}
                      </p>
                    </div>
                    <span className="grid size-10 place-items-center rounded-lg bg-cyan-50 text-cyan-700">
                      <Icon className="size-5" />
                    </span>
                  </CardContent>
                </Card>
              </Link>
            )
          })}
        </div>
      )}

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Thao tác nhanh</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          <Link
            href="/warehouse/phieu-xuat"
            className={cn(buttonVariants({ variant: "outline" }))}
          >
            Phiếu xuất
          </Link>
          <Link
            href="/warehouse/phieu-nhap"
            className={cn(buttonVariants({ variant: "outline" }))}
          >
            Phiếu nhập
          </Link>
          <Link
            href="/warehouse/giao-khach"
            className={cn(buttonVariants({ variant: "outline" }))}
          >
            Giao khách
          </Link>
          <Link
            href="/warehouse/ton-kho"
            className={cn(buttonVariants({ variant: "outline" }))}
          >
            Tồn kho
          </Link>
        </CardContent>
      </Card>
    </div>
  )
}
