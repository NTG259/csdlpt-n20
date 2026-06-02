"use client"

import { RefreshCwIcon, TrophyIcon } from "lucide-react"

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
import { useTopProducts } from "@/features/admin/use-admin-stats"
import { formatVnd } from "@/lib/format"

const MEDAL: Record<number, string> = { 1: "🥇", 2: "🥈", 3: "🥉" }

export default function SanPhamBanChayPage() {
  const { data = [], isLoading, isError, refetch, isFetching } =
    useTopProducts()

  const maxQty = Math.max(1, ...data.map((p) => p.tongSoLuongBan))

  return (
    <div className="grid gap-6">
      <section className="rounded-3xl bg-slate-950 p-6 text-white shadow-xl">
        <p className="text-sm font-semibold uppercase tracking-[0.24em] text-emerald-300">
          Admin dashboard
        </p>
        <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <h1 className="font-heading text-3xl font-semibold sm:text-4xl">
              Top sản phẩm bán chạy
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              10 sản phẩm có số lượng bán ra cao nhất trên toàn hệ thống
              SITE_BAC + SITE_NAM.
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

      {isLoading ? (
        <div className="grid gap-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-20 rounded-3xl" />
          ))}
        </div>
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
      ) : data.length === 0 ? (
        <Card className="rounded-3xl bg-white/90">
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            Chưa có dữ liệu sản phẩm.
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6 xl:grid-cols-[1fr_1.6fr]">
          <Card className="rounded-3xl bg-white/90">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrophyIcon className="size-5 text-amber-500" />
                Biểu đồ số lượng bán
              </CardTitle>
              <CardDescription>So sánh tương đối giữa các sản phẩm.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4">
              {data.map((product, idx) => (
                <div key={product.maSP} className="grid gap-1.5">
                  <div className="flex items-center justify-between text-sm">
                    <span className="font-medium">
                      {MEDAL[idx + 1] ?? `#${idx + 1}`}{" "}
                      <span className="ml-1">{product.tenSP}</span>
                    </span>
                    <span className="tabular-nums">
                      {product.tongSoLuongBan.toLocaleString("vi-VN")}
                    </span>
                  </div>
                  <div className="h-2.5 rounded-full bg-slate-100">
                    <div
                      className="h-full rounded-full bg-emerald-500 transition-all"
                      style={{
                        width: `${Math.max(
                          6,
                          (product.tongSoLuongBan / maxQty) * 100
                        )}%`,
                      }}
                    />
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="rounded-3xl bg-white/90">
            <CardHeader>
              <CardTitle>Bảng chi tiết</CardTitle>
              <CardDescription>
                Chỉ tính đơn đã thanh toán và đã hoàn thành.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">#</TableHead>
                    <TableHead>Mã SP</TableHead>
                    <TableHead>Tên sản phẩm</TableHead>
                    <TableHead className="text-right">SL bán</TableHead>
                    <TableHead className="text-right">Doanh thu</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.map((product, idx) => (
                    <TableRow key={product.maSP}>
                      <TableCell className="font-semibold text-muted-foreground">
                        {MEDAL[idx + 1] ?? idx + 1}
                      </TableCell>
                      <TableCell className="font-mono text-xs">
                        {product.maSP}
                      </TableCell>
                      <TableCell className="font-medium">
                        {product.tenSP}
                      </TableCell>
                      <TableCell className="text-right tabular-nums">
                        {product.tongSoLuongBan.toLocaleString("vi-VN")}
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatVnd(product.tongDoanhThu)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
