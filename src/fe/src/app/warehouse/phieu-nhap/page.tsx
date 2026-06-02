"use client"

import { useState } from "react"
import { useSearchParams } from "next/navigation"
import { EyeIcon, RefreshCwIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
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
import { Pagination } from "@/features/products/pagination"
import { ConfirmWarehouseAction } from "@/features/warehouse/confirm-warehouse-action"
import { PhieuNhapDetailDialog } from "@/features/warehouse/phieu-nhap-detail-dialog"
import {
  useConfirmInternalImport,
  useWarehouseImports,
} from "@/features/warehouse/use-warehouse-imports"
import { warehouseErrorMessage } from "@/features/warehouse/warehouse-error"
import { WarehouseStatusBadge } from "@/features/warehouse/warehouse-status"
import type {
  WarehouseImportQuery,
  WarehouseImportStatus,
} from "@/types/warehouse"

const PAGE_SIZE = 20

export default function WarehouseImportsPage() {
  const searchParams = useSearchParams()
  const [query, setQuery] = useState<WarehouseImportQuery>({
    page: 0,
    size: PAGE_SIZE,
    trangThaiNhap: readImportStatus(searchParams.get("trangThaiNhap")),
  })
  const [maDonHangDraft, setMaDonHangDraft] = useState("")
  const [detailId, setDetailId] = useState<string | null>(null)
  const [confirmId, setConfirmId] = useState<string | null>(null)
  const importsQuery = useWarehouseImports(query)
  const confirmImport = useConfirmInternalImport()
  const data = importsQuery.data

  function updateQuery(next: Partial<WarehouseImportQuery>) {
    setQuery((current) => ({ ...current, ...next, page: next.page ?? 0 }))
  }

  function applyOrderFilter() {
    updateQuery({ maDonHang: maDonHangDraft.trim() || undefined })
  }

  function confirmAction() {
    if (!confirmId) {
      return
    }

    confirmImport.mutate(confirmId, {
      onSuccess: () => setConfirmId(null),
    })
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase text-cyan-700">
            Warehouse
          </p>
          <h1 className="font-heading text-3xl font-semibold">Phiếu nhập</h1>
        </div>
        <Button
          type="button"
          variant="outline"
          disabled={importsQuery.isFetching}
          onClick={() => importsQuery.refetch()}
        >
          <RefreshCwIcon className="size-4" />
          Làm mới
        </Button>
      </div>

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Bộ lọc</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <Select
            value={query.trangThaiNhap ?? "all"}
            onValueChange={(value) =>
              updateQuery({
                trangThaiNhap:
                  value === "all" ? undefined : (value as WarehouseImportStatus),
              })
            }
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Trạng thái nhập" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả trạng thái</SelectItem>
              <SelectItem value="waiting_import">Chờ nhập</SelectItem>
              <SelectItem value="imported">Đã nhập</SelectItem>
              <SelectItem value="cancelled">Đã hủy</SelectItem>
            </SelectContent>
          </Select>
          <Input
            value={maDonHangDraft}
            onChange={(event) => setMaDonHangDraft(event.target.value)}
            placeholder="Mã đơn hàng"
            className="h-10"
          />
          <Button type="button" onClick={applyOrderFilter}>
            Áp dụng
          </Button>
        </CardContent>
      </Card>

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Danh sách phiếu nhập</CardTitle>
        </CardHeader>
        <CardContent>
          {importsQuery.isLoading ? (
            <Skeleton className="h-80 rounded-lg" />
          ) : importsQuery.isError ? (
            <ErrorBlock
              message={warehouseErrorMessage(
                importsQuery.error,
                "Không tải được phiếu nhập"
              )}
              onRetry={() => importsQuery.refetch()}
            />
          ) : (
            <div className="grid gap-4">
              <div className="overflow-x-auto rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Phiếu</TableHead>
                      <TableHead>Kho xuất</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Nguồn</TableHead>
                      <TableHead>Tổng SL</TableHead>
                      <TableHead className="text-right">Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!data || data.items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} className="py-10 text-center">
                          Chưa có phiếu nhập phù hợp.
                        </TableCell>
                      </TableRow>
                    ) : (
                      data.items.map((item) => {
                        const sourceReady =
                          item.sourceExportStatus === "exported" ||
                          item.sourceExportStatus === "remote" ||
                          !item.sourceExportStatus
                        const canConfirm =
                          item.trangThaiNhap === "waiting_import" && sourceReady

                        return (
                          <TableRow key={item.maPhieuNhap}>
                            <TableCell>
                              <div className="font-medium">{item.maPhieuNhap}</div>
                              <div className="text-xs text-muted-foreground">
                                Đơn {item.maDonHang}
                              </div>
                            </TableCell>
                            <TableCell>
                              {item.maKhoXuat} - {item.tenKhoXuat ?? "Remote"}
                            </TableCell>
                            <TableCell>
                              <WarehouseStatusBadge status={item.trangThaiNhap} />
                            </TableCell>
                            <TableCell>
                              <WarehouseStatusBadge status={item.sourceExportStatus} />
                            </TableCell>
                            <TableCell>{item.tongSoLuong}</TableCell>
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  onClick={() => setDetailId(item.maPhieuNhap)}
                                >
                                  <EyeIcon className="size-4" />
                                  Xem
                                </Button>
                                <Button
                                  type="button"
                                  size="sm"
                                  disabled={!canConfirm}
                                  title={
                                    sourceReady
                                      ? undefined
                                      : "Kho xuất chưa exported"
                                  }
                                  onClick={() => setConfirmId(item.maPhieuNhap)}
                                >
                                  Xác nhận nhập
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        )
                      })
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

      <PhieuNhapDetailDialog
        maPhieuNhap={detailId}
        open={Boolean(detailId)}
        onOpenChange={(open) => !open && setDetailId(null)}
      />
      <ConfirmWarehouseAction
        open={Boolean(confirmId)}
        title="Xác nhận nhập nội bộ"
        description={
          confirmId
            ? `Phiếu ${confirmId} sẽ được chuyển trạng thái sau khi backend cộng tồn kho.`
            : ""
        }
        confirmLabel="Xác nhận nhập"
        isPending={confirmImport.isPending}
        onOpenChange={(open) => !open && setConfirmId(null)}
        onConfirm={confirmAction}
      />
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

function readImportStatus(value: string | null): WarehouseImportStatus | undefined {
  return value === "waiting_import" || value === "imported" || value === "cancelled"
    ? value
    : undefined
}
