"use client"

import { RefreshCwIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useMultiWarehouseOrders } from "@/features/admin/use-admin-stats"

function isInterSite(danhSachKhoXuat: string): boolean {
  const codes = danhSachKhoXuat
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean)
  const prefixes = new Set(codes.map((c) => c.slice(0, 2)))
  return prefixes.size >= 2
}

export default function DonHangNhieuKhoPage() {
  const { data = [], isLoading, isError, refetch, isFetching } =
    useMultiWarehouseOrders()

  return (
    <div className="grid gap-6">
      <section className="rounded-3xl bg-slate-950 p-6 text-white shadow-xl">
        <p className="text-sm font-semibold uppercase tracking-[0.24em] text-emerald-300">
          Admin dashboard
        </p>
        <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <h1 className="font-heading text-3xl font-semibold sm:text-4xl">
              Đơn hàng nhiều kho
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              Các đơn hàng được xuất từ 2 kho trở lên, kể cả liên site
              SITE_BAC ↔ SITE_NAM.
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

      <div className="grid gap-4 md:grid-cols-3">
        <SummaryCard
          label="Tổng đơn nhiều kho"
          value={data.length}
          accent="bg-slate-900"
        />
        <SummaryCard
          label="Đơn xuất từ 3+ kho"
          value={data.filter((d) => d.soKhoXuat >= 3).length}
          accent="bg-amber-500"
        />
        <SummaryCard
          label="Đơn liên site"
          value={data.filter((d) => isInterSite(d.danhSachKhoXuat)).length}
          accent="bg-emerald-500"
        />
      </div>

      {isLoading ? (
        <Skeleton className="h-72 rounded-3xl" />
      ) : isError ? (
        <Card className="rounded-3xl border-red-200 bg-red-50">
          <CardHeader>
            <CardTitle>Không tải được dữ liệu</CardTitle>
            <CardDescription>
              Kiểm tra kết nối DB hoặc linked server SITE_BAC/SITE_NAM.
            </CardDescription>
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
            <CardTitle>Danh sách đơn hàng</CardTitle>
            <CardDescription>
              Chỉ tính phiếu xuất có trạng thái &quot;exported&quot;. Mỗi mã
              kho chỉ đếm một lần.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {data.length === 0 ? (
              <p className="py-8 text-center text-sm text-muted-foreground">
                Chưa có đơn hàng xuất từ nhiều kho.
              </p>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Mã đơn hàng</TableHead>
                    <TableHead className="text-center">Số kho</TableHead>
                    <TableHead>Danh sách kho</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.map((order) => (
                    <TableRow key={order.maDonHang}>
                      <TableCell className="font-mono font-medium">
                        {order.maDonHang}
                      </TableCell>
                      <TableCell className="text-center">
                        <span
                          className={`inline-flex size-7 items-center justify-center rounded-full text-xs font-bold ${
                            order.soKhoXuat >= 3
                              ? "bg-amber-100 text-amber-700"
                              : "bg-slate-100 text-slate-700"
                          }`}
                        >
                          {order.soKhoXuat}
                        </span>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {order.danhSachKhoXuat}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}

function SummaryCard({
  label,
  value,
  accent,
}: {
  label: string
  value: number
  accent: string
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
