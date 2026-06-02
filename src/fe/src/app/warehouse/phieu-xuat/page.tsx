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
import { PhieuXuatDetailDialog } from "@/features/warehouse/phieu-xuat-detail-dialog"
import {
  useConfirmCustomerExport,
  useConfirmInternalExport,
  useWarehouseExports,
} from "@/features/warehouse/use-warehouse-exports"
import { warehouseErrorMessage } from "@/features/warehouse/warehouse-error"
import {
  WarehouseStatusBadge,
} from "@/features/warehouse/warehouse-status"
import type {
  WarehouseExportQuery,
  WarehouseExportStatus,
  WarehouseExportType,
  WarehouseReceiveStatus,
} from "@/types/warehouse"

const PAGE_SIZE = 20

interface ConfirmTarget {
  id: string
  type: "internal" | "customer"
}

export default function WarehouseExportsPage() {
  const searchParams = useSearchParams()
  const [query, setQuery] = useState<WarehouseExportQuery>({
    page: 0,
    size: PAGE_SIZE,
    loai: readExportType(searchParams.get("loai")),
    trangThaiXuat: readExportStatus(searchParams.get("trangThaiXuat")),
    trangThaiNhan: readReceiveStatus(searchParams.get("trangThaiNhan")),
  })
  const [maDonHangDraft, setMaDonHangDraft] = useState("")
  const [detailId, setDetailId] = useState<string | null>(null)
  const [confirmTarget, setConfirmTarget] = useState<ConfirmTarget | null>(null)
  const exportsQuery = useWarehouseExports(query)
  const confirmInternal = useConfirmInternalExport()
  const confirmCustomer = useConfirmCustomerExport()
  const isConfirming = confirmInternal.isPending || confirmCustomer.isPending
  const data = exportsQuery.data

  function updateQuery(next: Partial<WarehouseExportQuery>) {
    setQuery((current) => ({ ...current, ...next, page: next.page ?? 0 }))
  }

  function applyOrderFilter() {
    updateQuery({ maDonHang: maDonHangDraft.trim() || undefined })
  }

  function confirmAction() {
    if (!confirmTarget) {
      return
    }

    const mutation =
      confirmTarget.type === "internal" ? confirmInternal : confirmCustomer

    mutation.mutate(confirmTarget.id, {
      onSuccess: () => setConfirmTarget(null),
    })
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase text-cyan-700">
            Warehouse
          </p>
          <h1 className="font-heading text-3xl font-semibold">Phiếu xuất</h1>
        </div>
        <Button
          type="button"
          variant="outline"
          disabled={exportsQuery.isFetching}
          onClick={() => exportsQuery.refetch()}
        >
          <RefreshCwIcon className="size-4" />
          Làm mới
        </Button>
      </div>

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Bộ lọc</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
          <Select
            value={query.loai ?? "all"}
            onValueChange={(value) =>
              updateQuery({
                loai: value === "all" ? undefined : (value as WarehouseExportType),
              })
            }
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Loại phiếu" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả loại</SelectItem>
              <SelectItem value="noi_bo">Nội bộ</SelectItem>
              <SelectItem value="giao_khach">Giao khách</SelectItem>
            </SelectContent>
          </Select>
          <Select
            value={query.trangThaiXuat ?? "all"}
            onValueChange={(value) =>
              updateQuery({
                trangThaiXuat:
                  value === "all" ? undefined : (value as WarehouseExportStatus),
              })
            }
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Trạng thái xuất" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả xuất</SelectItem>
              <SelectItem value="waiting_export">Chờ xuất</SelectItem>
              <SelectItem value="exported">Đã xuất</SelectItem>
              <SelectItem value="cancelled">Đã hủy</SelectItem>
            </SelectContent>
          </Select>
          <Select
            value={query.trangThaiNhan ?? "all"}
            onValueChange={(value) =>
              updateQuery({
                trangThaiNhan:
                  value === "all" ? undefined : (value as WarehouseReceiveStatus),
              })
            }
          >
            <SelectTrigger className="h-10 w-full">
              <SelectValue placeholder="Trạng thái nhận" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả nhận</SelectItem>
              <SelectItem value="waiting_receive">Chờ nhận</SelectItem>
              <SelectItem value="received">Đã nhận</SelectItem>
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
          <CardTitle>Danh sách phiếu xuất</CardTitle>
        </CardHeader>
        <CardContent>
          {exportsQuery.isLoading ? (
            <Skeleton className="h-80 rounded-lg" />
          ) : exportsQuery.isError ? (
            <ErrorBlock
              message={warehouseErrorMessage(
                exportsQuery.error,
                "Không tải được phiếu xuất"
              )}
              onRetry={() => exportsQuery.refetch()}
            />
          ) : (
            <div className="grid gap-4">
              <div className="overflow-x-auto rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Phiếu</TableHead>
                      <TableHead>Loại</TableHead>
                      <TableHead>Kho nhận</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Tổng SL</TableHead>
                      <TableHead className="text-right">Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!data || data.items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} className="py-10 text-center">
                          Chưa có phiếu xuất phù hợp.
                        </TableCell>
                      </TableRow>
                    ) : (
                      data.items.map((item) => {
                        const canInternal =
                          item.loaiPhieu === "noi_bo" &&
                          item.trangThaiXuat === "waiting_export"
                        const canCustomer =
                          item.loaiPhieu === "giao_khach" &&
                          item.trangThaiXuat === "waiting_export"

                        return (
                          <TableRow key={item.maPhieuXuat}>
                            <TableCell>
                              <div className="font-medium">{item.maPhieuXuat}</div>
                              <div className="text-xs text-muted-foreground">
                                Đơn {item.maDonHang}
                              </div>
                            </TableCell>
                            <TableCell>
                              <WarehouseStatusBadge status={item.loaiPhieu} />
                            </TableCell>
                            <TableCell>
                              {item.maKhoNhan
                                ? `${item.maKhoNhan} - ${item.tenKhoNhan ?? "-"}`
                                : "Giao khách"}
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-wrap gap-2">
                                <WarehouseStatusBadge status={item.trangThaiXuat} />
                                <WarehouseStatusBadge status={item.trangThaiNhan} />
                              </div>
                            </TableCell>
                            <TableCell>{item.tongSoLuong}</TableCell>
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
                                <Button
                                  type="button"
                                  variant="outline"
                                  size="sm"
                                  onClick={() => setDetailId(item.maPhieuXuat)}
                                >
                                  <EyeIcon className="size-4" />
                                  Xem
                                </Button>
                                {canInternal && (
                                  <Button
                                    type="button"
                                    size="sm"
                                    onClick={() =>
                                      setConfirmTarget({
                                        id: item.maPhieuXuat,
                                        type: "internal",
                                      })
                                    }
                                  >
                                    Xác nhận xuất
                                  </Button>
                                )}
                                {canCustomer && (
                                  <Button
                                    type="button"
                                    size="sm"
                                    onClick={() =>
                                      setConfirmTarget({
                                        id: item.maPhieuXuat,
                                        type: "customer",
                                      })
                                    }
                                  >
                                    Xác nhận giao
                                  </Button>
                                )}
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

      <PhieuXuatDetailDialog
        maPhieuXuat={detailId}
        open={Boolean(detailId)}
        onOpenChange={(open) => !open && setDetailId(null)}
      />
      <ConfirmWarehouseAction
        open={Boolean(confirmTarget)}
        title={
          confirmTarget?.type === "customer"
            ? "Xác nhận xuất giao khách"
            : "Xác nhận xuất nội bộ"
        }
        description={
          confirmTarget
            ? `Phiếu ${confirmTarget.id} sẽ được chuyển trạng thái sau khi backend trừ tồn kho.`
            : ""
        }
        confirmLabel={
          confirmTarget?.type === "customer"
            ? "Xác nhận giao"
            : "Xác nhận xuất"
        }
        isPending={isConfirming}
        onOpenChange={(open) => !open && setConfirmTarget(null)}
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

function readExportType(value: string | null): WarehouseExportType | undefined {
  return value === "noi_bo" || value === "giao_khach" ? value : undefined
}

function readExportStatus(value: string | null): WarehouseExportStatus | undefined {
  return value === "waiting_export" ||
    value === "exported" ||
    value === "cancelled"
    ? value
    : undefined
}

function readReceiveStatus(
  value: string | null
): WarehouseReceiveStatus | undefined {
  return value === "waiting_receive" || value === "received" ? value : undefined
}
