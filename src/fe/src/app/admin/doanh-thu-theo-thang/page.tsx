"use client"

import { useState, useMemo } from "react"
import { RefreshCwIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
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
import { useRevenueByMonth } from "@/features/admin/use-admin-stats"
import { formatVnd } from "@/lib/format"
import type { DoanhThuTheoThang } from "@/types/admin"

const SITE_OPTIONS = [
  { value: "all", label: "Tất cả" },
  { value: "SITE_BAC", label: "Site Bắc" },
  { value: "SITE_NAM", label: "Site Nam" },
  { value: "TOAN_HE_THONG", label: "Toàn hệ thống" },
]

export default function DoanhThuTheoThangPage() {
  const [siteFilter, setSiteFilter] = useState("all")
  const { data = [], isLoading, isError, refetch, isFetching } =
    useRevenueByMonth()

  const filtered = useMemo<DoanhThuTheoThang[]>(() => {
    if (siteFilter === "all") return data
    return data.filter((row) => row.siteNguon === siteFilter)
  }, [data, siteFilter])

  const years = useMemo(
    () => [...new Set(filtered.map((r) => r.nam))].sort((a, b) => b - a),
    [filtered]
  )

  return (
    <div className="grid gap-6">
      <section className="rounded-3xl bg-slate-950 p-6 text-white shadow-xl">
        <p className="text-sm font-semibold uppercase tracking-[0.24em] text-emerald-300">
          Admin dashboard
        </p>
        <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <h1 className="font-heading text-3xl font-semibold sm:text-4xl">
              Doanh thu theo tháng
            </h1>
            <p className="mt-2 max-w-2xl text-sm text-slate-300">
              Lịch sử doanh thu nhóm theo năm, tháng và kho — gồm cả dòng tổng
              toàn hệ thống.
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
          <CardTitle>Lọc theo site</CardTitle>
        </CardHeader>
        <CardContent>
          <Select
            value={siteFilter}
            onValueChange={(v) => setSiteFilter(v ?? "all")}
          >
            <SelectTrigger className="w-56">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {SITE_OPTIONS.map((opt) => (
                <SelectItem key={opt.value} value={opt.value}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {isLoading ? (
        <div className="grid gap-4">
          {[0, 1].map((i) => (
            <Skeleton key={i} className="h-48 rounded-3xl" />
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
      ) : filtered.length === 0 ? (
        <Card className="rounded-3xl bg-white/90">
          <CardContent className="py-12 text-center text-sm text-muted-foreground">
            Chưa có dữ liệu doanh thu.
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6">
          {years.map((year) => {
            const rows = filtered.filter((r) => r.nam === year)
            const months = [...new Set(rows.map((r) => r.thang))].sort(
              (a, b) => b - a
            )
            return (
              <Card key={year} className="rounded-3xl bg-white/90">
                <CardHeader>
                  <CardTitle>Năm {year}</CardTitle>
                  <CardDescription>
                    {months.length} tháng có dữ liệu
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Tháng</TableHead>
                        <TableHead>Site</TableHead>
                        <TableHead>Mã kho</TableHead>
                        <TableHead>Tên kho</TableHead>
                        <TableHead className="text-right">Doanh thu</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {rows.map((row, idx) => (
                        <TableRow
                          key={idx}
                          className={
                            row.siteNguon === "TOAN_HE_THONG"
                              ? "bg-emerald-50 font-semibold"
                              : undefined
                          }
                        >
                          <TableCell>
                            {row.siteNguon === "TOAN_HE_THONG"
                              ? `T${row.thang} tổng`
                              : `T${row.thang}`}
                          </TableCell>
                          <TableCell>
                            <span
                              className={
                                row.siteNguon === "TOAN_HE_THONG"
                                  ? "text-emerald-700"
                                  : undefined
                              }
                            >
                              {row.siteNguon}
                            </span>
                          </TableCell>
                          <TableCell>{row.maKho ?? "—"}</TableCell>
                          <TableCell>{row.tenKho}</TableCell>
                          <TableCell className="text-right">
                            {formatVnd(row.doanhThu)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </CardContent>
              </Card>
            )
          })}
        </div>
      )}
    </div>
  )
}
