"use client"

import { useState } from "react"
import { useSearchParams } from "next/navigation"
import { RefreshCwIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Pagination } from "@/features/products/pagination"
import { useWarehouseStock } from "@/features/warehouse/use-warehouse-stock"
import { warehouseErrorMessage } from "@/features/warehouse/warehouse-error"
import type { WarehouseStockQuery } from "@/types/warehouse"

const PAGE_SIZE = 20

export default function WarehouseStockPage() {
  const searchParams = useSearchParams()
  const [query, setQuery] = useState<WarehouseStockQuery>({
    page: 0,
    size: PAGE_SIZE,
    onlyLowStock: searchParams.get("onlyLowStock") === "true" || undefined,
    onlyReserved: searchParams.get("onlyReserved") === "true" || undefined,
  })
  const [search, setSearch] = useState("")
  const stockQuery = useWarehouseStock(query)
  const data = stockQuery.data

  function updateQuery(next: Partial<WarehouseStockQuery>) {
    setQuery((current) => ({ ...current, ...next, page: next.page ?? 0 }))
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase text-cyan-700">
            Warehouse
          </p>
          <h1 className="font-heading text-3xl font-semibold">Tồn kho</h1>
        </div>
        <Button
          type="button"
          variant="outline"
          disabled={stockQuery.isFetching}
          onClick={() => stockQuery.refetch()}
        >
          <RefreshCwIcon className="size-4" />
          Làm mới
        </Button>
      </div>

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Bộ lọc</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-[1fr_auto_auto_auto] md:items-center">
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Tìm mã hoặc tên sản phẩm"
            className="h-10"
          />
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="size-4"
              checked={Boolean(query.onlyReserved)}
              onChange={(event) =>
                updateQuery({ onlyReserved: event.target.checked || undefined })
              }
            />
            Có hàng đang giữ
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="size-4"
              checked={Boolean(query.onlyLowStock)}
              onChange={(event) =>
                updateQuery({ onlyLowStock: event.target.checked || undefined })
              }
            />
            Sắp hết
          </label>
          <Button
            type="button"
            onClick={() => updateQuery({ q: search.trim() || undefined })}
          >
            Áp dụng
          </Button>
        </CardContent>
      </Card>

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Danh sách tồn kho</CardTitle>
        </CardHeader>
        <CardContent>
          {stockQuery.isLoading ? (
            <Skeleton className="h-80 rounded-lg" />
          ) : stockQuery.isError ? (
            <ErrorBlock
              message={warehouseErrorMessage(
                stockQuery.error,
                "Không tải được tồn kho"
              )}
              onRetry={() => stockQuery.refetch()}
            />
          ) : (
            <div className="grid gap-4">
              <div className="overflow-x-auto rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Sản phẩm</TableHead>
                      <TableHead>Số lượng tồn</TableHead>
                      <TableHead>Đang giữ</TableHead>
                      <TableHead>Khả dụng</TableHead>
                      <TableHead>Ngày cập nhật</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!data || data.items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={5} className="py-10 text-center">
                          Chưa có tồn kho phù hợp.
                        </TableCell>
                      </TableRow>
                    ) : (
                      data.items.map((item) => (
                        <TableRow key={`${item.maKho}-${item.maSP}`}>
                          <TableCell>
                            <div className="font-medium">{item.tenSP}</div>
                            <div className="text-xs text-muted-foreground">
                              {item.maSP}
                            </div>
                          </TableCell>
                          <TableCell>{item.soLuongTon}</TableCell>
                          <TableCell>{item.soLuongDatHang}</TableCell>
                          <TableCell>
                            <div className="flex items-center gap-2">
                              <span>{item.soLuongKhaDung}</span>
                              {item.soLuongKhaDung <= 5 && (
                                <Badge className="bg-amber-100 text-amber-800">
                                  Sắp hết
                                </Badge>
                              )}
                            </div>
                          </TableCell>
                          <TableCell>{formatDateTime(item.ngayCapNhat)}</TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
              {data && (
                <Pagination
                  page={data.page}
                  totalPages={data.totalPages}
                  last={data.last}
                  onPageChange={(page) => updateQuery({ page })}
                />
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

function ErrorBlock({
  message,
  onRetry,
}: {
  message: string
  onRetry: () => void
}) {
  return (
    <div className="grid gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
      <p>{message}</p>
      <Button type="button" className="w-fit" onClick={onRetry}>
        Thử lại
      </Button>
    </div>
  )
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-"
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value))
}
