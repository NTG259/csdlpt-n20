"use client"

import { useState } from "react"
import { RefreshCwIcon } from "lucide-react"

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
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useRevenueStats } from "@/features/admin/use-admin-stats"
import { useProducts } from "@/features/products/use-products"
import { useCategories } from "@/features/products/use-reference"
import { formatVnd } from "@/lib/format"
import type { RevenueStatsFilter, ThongKeDoanhThu } from "@/types/admin"

const defaultFilters: RevenueStatsFilter = {
  chiTinhDaXuat: true,
}

function toSelectValue(value?: string) {
  return value && value.trim() ? value : "all"
}

export default function AdminDashboardPage() {
  const [filters, setFilters] = useState<RevenueStatsFilter>(defaultFilters)
  const { data, isLoading, isError, refetch, isFetching } =
    useRevenueStats(filters)
  const { data: categories = [] } = useCategories()
  const { data: productPage } = useProducts({ size: 100, trangThai: true })

  return (
    <div className="grid gap-6">
      <section className="rounded-3xl bg-slate-950 p-6 text-white shadow-xl">
        <p className="text-sm font-semibold uppercase tracking-[0.24em] text-emerald-300">
          Admin dashboard
        </p>
        <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <h1 className="font-heading text-3xl font-semibold sm:text-4xl">
              Doanh thu toàn hệ thống
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              Dữ liệu được tổng hợp từ Site Main qua stored procedure cross-site
              SITE_BAC/SITE_NAM.
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
          <CardTitle>Bộ lọc thống kê</CardTitle>
          <CardDescription>
            Ngày dùng định dạng inclusive theo backend. Bỏ trống để xem toàn bộ.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
            <div className="grid gap-2">
              <Label htmlFor="tuNgay">Từ ngày</Label>
              <Input
                id="tuNgay"
                type="date"
                value={filters.tuNgay ?? ""}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    tuNgay: event.target.value || undefined,
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
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    denNgay: event.target.value || undefined,
                  }))
                }
              />
            </div>
            <div className="grid gap-2">
              <Label>Khu vực</Label>
              <Input
                placeholder="KV01..."
                value={filters.maKhuVuc ?? ""}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    maKhuVuc: event.target.value || undefined,
                  }))
                }
              />
            </div>
            <div className="grid gap-2">
              <Label>Mã kho</Label>
              <Input
                placeholder="KB01..."
                value={filters.maKho ?? ""}
                onChange={(event) =>
                  setFilters((current) => ({
                    ...current,
                    maKho: event.target.value || undefined,
                  }))
                }
              />
            </div>
            <div className="grid gap-2">
              <Label>Sản phẩm</Label>
              <Select
                value={toSelectValue(filters.maSP)}
                onValueChange={(value) =>
                  setFilters((current) => ({
                    ...current,
                    maSP: value === "all" ? undefined : String(value),
                  }))
                }
              >
                <SelectTrigger className="h-10 w-full">
                  <SelectValue placeholder="Tất cả sản phẩm" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả sản phẩm</SelectItem>
                  {productPage?.items.map((product) => (
                    <SelectItem key={product.maSP} value={product.maSP}>
                      {product.maSP} - {product.tenSP}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label>Loại doanh thu</Label>
              <Select
                value={filters.chiTinhDaXuat === false ? "false" : "true"}
                onValueChange={(value) =>
                  setFilters((current) => ({
                    ...current,
                    chiTinhDaXuat: value !== "false",
                  }))
                }
              >
                <SelectTrigger className="h-10 w-full">
                  <SelectValue placeholder="Loại doanh thu" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="true">Chỉ đã xuất</SelectItem>
                  <SelectItem value="false">Gồm dự kiến</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Button type="button" onClick={() => refetch()} disabled={isFetching}>
              Áp dụng
            </Button>
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
        <DashboardSkeleton />
      ) : isError || !data ? (
        <Card className="rounded-3xl border-red-200 bg-red-50">
          <CardHeader>
            <CardTitle>Không tải được thống kê doanh thu</CardTitle>
            <CardDescription>
              Nếu lỗi là INTERNAL_ERROR, kiểm tra DB hoặc linked server
              SITE_BAC/SITE_NAM.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button type="button" onClick={() => refetch()}>
              Thử lại
            </Button>
          </CardContent>
        </Card>
      ) : (
        <RevenueDashboard data={data} categoryCount={categories.length} />
      )}
    </div>
  )
}

function DashboardSkeleton() {
  return (
    <div className="grid gap-4">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        {Array.from({ length: 5 }).map((_, index) => (
          <Skeleton key={index} className="h-28 rounded-3xl" />
        ))}
      </div>
      <Skeleton className="h-72 rounded-3xl" />
    </div>
  )
}

function RevenueDashboard({
  data,
  categoryCount,
}: {
  data: ThongKeDoanhThu
  categoryCount: number
}) {
  const stats = data.toanHeThong
  const maxRegionRevenue = Math.max(
    1,
    ...data.theoVung.map((item) => item.doanhThu)
  )

  return (
    <div className="grid gap-6">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <MetricCard
          label="Tổng doanh thu"
          value={formatVnd(stats.tongDoanhThu)}
          accent="bg-emerald-500"
        />
        <MetricCard label="Đơn hàng" value={stats.tongSoDonHang} />
        <MetricCard label="Phiếu xuất" value={stats.tongSoPhieuXuat} />
        <MetricCard label="SL xuất" value={stats.tongSoLuongXuat} />
        <MetricCard label="Danh mục active" value={categoryCount} />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1fr_1.4fr]">
        <Card className="rounded-3xl bg-white/90">
          <CardHeader>
            <CardTitle>Doanh thu theo vùng</CardTitle>
            <CardDescription>
              So sánh doanh thu xuất kho theo từng mã khu vực.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4">
            {data.theoVung.length === 0 ? (
              <p className="text-sm text-muted-foreground">Chưa có dữ liệu.</p>
            ) : (
              data.theoVung.map((region) => (
                <div key={region.maKhuVuc} className="grid gap-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium">{region.maKhuVuc}</span>
                    <span>{formatVnd(region.doanhThu)}</span>
                  </div>
                  <div className="h-3 rounded-full bg-slate-100">
                    <div
                      className="h-full rounded-full bg-emerald-500"
                      style={{
                        width: `${Math.max(
                          8,
                          (region.doanhThu / maxRegionRevenue) * 100
                        )}%`,
                      }}
                    />
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        <Card className="rounded-3xl bg-white/90">
          <CardHeader>
            <CardTitle>Chi tiết theo kho</CardTitle>
            <CardDescription>
              Nguồn từ result set `theoKho` của API doanh thu.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Site</TableHead>
                  <TableHead>Kho</TableHead>
                  <TableHead>Vùng</TableHead>
                  <TableHead>Đơn</TableHead>
                  <TableHead>SL xuất</TableHead>
                  <TableHead className="text-right">Doanh thu</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.theoKho.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="py-8 text-center">
                      Chưa có dữ liệu theo kho.
                    </TableCell>
                  </TableRow>
                ) : (
                  data.theoKho.map((warehouse) => (
                    <TableRow key={`${warehouse.siteXuat}-${warehouse.maKhoXuat}`}>
                      <TableCell>{warehouse.siteXuat}</TableCell>
                      <TableCell>
                        <div className="font-medium">{warehouse.maKhoXuat}</div>
                        <div className="text-xs text-muted-foreground">
                          {warehouse.tenKho}
                        </div>
                      </TableCell>
                      <TableCell>{warehouse.maKhuVuc}</TableCell>
                      <TableCell>{warehouse.soDonHang}</TableCell>
                      <TableCell>{warehouse.tongSoLuongXuat}</TableCell>
                      <TableCell className="text-right font-medium">
                        {formatVnd(warehouse.doanhThu)}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function MetricCard({
  label,
  value,
  accent = "bg-slate-900",
}: {
  label: string
  value: string | number
  accent?: string
}) {
  return (
    <Card className="rounded-3xl bg-white/90">
      <CardContent className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
            {label}
          </p>
          <p className="mt-2 text-2xl font-semibold">{value}</p>
        </div>
        <span className={`size-3 rounded-full ${accent}`} />
      </CardContent>
    </Card>
  )
}
