"use client"

import { useState } from "react"
import { ChevronLeftIcon, ChevronRightIcon, RefreshCwIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { OrderDetailDialog } from "@/features/admin/order-detail-dialog"
import {
  TRANG_THAI_DH_OPTIONS,
  TRANG_THAI_TT_OPTIONS,
  TrangThaiDHBadge,
  TrangThaiTTBadge,
} from "@/features/admin/order-status-badge"
import { useAdminOrders } from "@/features/admin/use-admin-orders"
import { formatVnd } from "@/lib/format"
import type { AdminOrderFilter, AdminOrderItem } from "@/types/admin"

const PAGE_SIZE = 20

const defaultFilters: AdminOrderFilter = {
  page: 0,
  size: PAGE_SIZE,
}

function toSelectVal(v?: string) {
  return v ?? "all"
}

export default function AdminOrdersPage() {
  const [filters, setFilters] = useState<AdminOrderFilter>(defaultFilters)
  const [selected, setSelected] = useState<AdminOrderItem | null>(null)

  const { data, isLoading, isError, refetch, isFetching } =
    useAdminOrders(filters)

  function setPage(page: number) {
    setFilters((f) => ({ ...f, page }))
  }

  return (
    <div className="grid gap-6">
      <section className="rounded-3xl bg-slate-950 p-6 text-white shadow-xl">
        <p className="text-sm font-semibold uppercase tracking-[0.24em] text-emerald-300">
          Admin dashboard
        </p>
        <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <h1 className="font-heading text-3xl font-semibold sm:text-4xl">
              Quản lý đơn hàng
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              Danh sách đơn hàng từ SITE_BAC và SITE_NAM. Nhấn vào dòng để xem
              chi tiết và cập nhật trạng thái.
            </p>
          </div>
          <Button
            type="button"
            variant="secondary"
            disabled={isFetching}
            onClick={() => refetch()}
          >
            <RefreshCwIcon className="size-4" />
            Làm mới
          </Button>
        </div>
      </section>

      <Card className="rounded-3xl bg-white/90">
        <CardHeader>
          <CardTitle>Bộ lọc</CardTitle>
          <CardDescription>Bỏ trống để xem tất cả.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            <div className="grid gap-2">
              <Label>Site</Label>
              <Select
                value={toSelectVal(filters.siteNguon)}
                onValueChange={(v) =>
                  setFilters((f) => ({
                    ...f,
                    page: 0,
                    siteNguon: v === "all" ? undefined : (v ?? undefined),
                  }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả site</SelectItem>
                  <SelectItem value="SITE_BAC">Site Bắc</SelectItem>
                  <SelectItem value="SITE_NAM">Site Nam</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label>Trạng thái ĐH</Label>
              <Select
                value={toSelectVal(filters.trangThaiDH)}
                onValueChange={(v) =>
                  setFilters((f) => ({
                    ...f,
                    page: 0,
                    trangThaiDH: v === "all" ? undefined : (v ?? undefined),
                  }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  {TRANG_THAI_DH_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label>Trạng thái TT</Label>
              <Select
                value={toSelectVal(filters.trangThaiTT)}
                onValueChange={(v) =>
                  setFilters((f) => ({
                    ...f,
                    page: 0,
                    trangThaiTT: v === "all" ? undefined : (v ?? undefined),
                  }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  {TRANG_THAI_TT_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="tuNgay">Từ ngày</Label>
              <Input
                id="tuNgay"
                type="date"
                value={filters.tuNgay ?? ""}
                onChange={(e) =>
                  setFilters((f) => ({
                    ...f,
                    page: 0,
                    tuNgay: e.target.value || undefined,
                  }))
                }
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="denNgay">Đến ngày</Label>
              <Input
                id="denNgay"
                type="date"
                value={filters.denNgay ?? ""}
                onChange={(e) =>
                  setFilters((f) => ({
                    ...f,
                    page: 0,
                    denNgay: e.target.value || undefined,
                  }))
                }
              />
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => setFilters(defaultFilters)}
            >
              Xóa lọc
            </Button>
          </div>
        </CardContent>
      </Card>

      {isLoading ? (
        <Skeleton className="h-72 rounded-3xl" />
      ) : isError ? (
        <Card className="rounded-3xl border-red-200 bg-red-50">
          <CardHeader>
            <CardTitle>Không tải được đơn hàng</CardTitle>
          </CardHeader>
          <CardContent>
            <Button type="button" onClick={() => refetch()}>
              Thử lại
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Card className="rounded-3xl bg-white/90">
          <CardHeader>
            <CardTitle>
              Danh sách đơn hàng
              {data && (
                <span className="ml-2 text-base font-normal text-muted-foreground">
                  ({data.totalElements} đơn)
                </span>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Site</TableHead>
                  <TableHead>Mã đơn hàng</TableHead>
                  <TableHead>Người nhận</TableHead>
                  <TableHead>Ngày đặt</TableHead>
                  <TableHead>Trạng thái ĐH</TableHead>
                  <TableHead>Trạng thái TT</TableHead>
                  <TableHead className="text-right">Tổng tiền</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {!data || data.items.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={7}
                      className="py-12 text-center text-muted-foreground"
                    >
                      Không có đơn hàng nào.
                    </TableCell>
                  </TableRow>
                ) : (
                  data.items.map((order) => (
                    <TableRow
                      key={`${order.siteNguon}-${order.maDonHang}`}
                      className="cursor-pointer hover:bg-slate-50"
                      onClick={() => setSelected(order)}
                    >
                      <TableCell>
                        <span className="rounded-md bg-slate-100 px-1.5 py-0.5 text-xs font-medium">
                          {order.siteNguon}
                        </span>
                      </TableCell>
                      <TableCell className="max-w-[160px] truncate font-mono text-xs">
                        {order.maDonHang}
                      </TableCell>
                      <TableCell>
                        <div className="font-medium">{order.hoTenNguoiNhan}</div>
                        <div className="text-xs text-muted-foreground">
                          {order.soDienThoaiNhan}
                        </div>
                      </TableCell>
                      <TableCell className="text-sm">
                        {new Date(order.ngayDat).toLocaleDateString("vi-VN")}
                      </TableCell>
                      <TableCell>
                        <TrangThaiDHBadge value={order.trangThaiDH} />
                      </TableCell>
                      <TableCell>
                        <TrangThaiTTBadge value={order.trangThaiTT} />
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatVnd(order.tongTien)}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>

            {data && data.totalPages > 1 && (
              <div className="flex items-center justify-between border-t px-6 py-4">
                <p className="text-sm text-muted-foreground">
                  Trang {(filters.page ?? 0) + 1} / {data.totalPages}
                </p>
                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={(filters.page ?? 0) === 0}
                    onClick={() => setPage((filters.page ?? 0) - 1)}
                  >
                    <ChevronLeftIcon className="size-4" />
                    Trước
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={data.last}
                    onClick={() => setPage((filters.page ?? 0) + 1)}
                  >
                    Sau
                    <ChevronRightIcon className="size-4" />
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <OrderDetailDialog
        order={selected}
        onClose={() => setSelected(null)}
      />
    </div>
  )
}
