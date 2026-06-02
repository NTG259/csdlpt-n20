"use client"

import { useState } from "react"
import { EyeIcon, RefreshCwIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
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
import { Pagination } from "@/features/products/pagination"
import { ConfirmWarehouseAction } from "@/features/warehouse/confirm-warehouse-action"
import { PhieuXuatDetailDialog } from "@/features/warehouse/phieu-xuat-detail-dialog"
import {
  useConfirmCustomerExport,
  useWarehouseExports,
} from "@/features/warehouse/use-warehouse-exports"
import {
  useCreateCustomerExportSlip,
  useReadyToShipOrders,
} from "@/features/warehouse/use-warehouse-ready-orders"
import { warehouseErrorMessage } from "@/features/warehouse/warehouse-error"
import { WarehouseStatusBadge } from "@/features/warehouse/warehouse-status"

const PAGE_SIZE = 10

type ConfirmTarget =
  | { type: "create"; id: string }
  | { type: "confirm"; id: string }

export default function WarehouseDeliveryPage() {
  const [readyPage, setReadyPage] = useState(0)
  const [exportPage, setExportPage] = useState(0)
  const [detailId, setDetailId] = useState<string | null>(null)
  const [confirmTarget, setConfirmTarget] = useState<ConfirmTarget | null>(null)
  const readyOrdersQuery = useReadyToShipOrders({
    page: readyPage,
    size: PAGE_SIZE,
    sort: "ngayDat,desc",
  })
  const exportsQuery = useWarehouseExports({
    page: exportPage,
    size: PAGE_SIZE,
    sort: "ngayTao,desc",
    loai: "giao_khach",
    trangThaiXuat: "waiting_export",
  })
  const createSlip = useCreateCustomerExportSlip()
  const confirmCustomer = useConfirmCustomerExport()
  const isPending = createSlip.isPending || confirmCustomer.isPending

  function refresh() {
    readyOrdersQuery.refetch()
    exportsQuery.refetch()
  }

  function confirmAction() {
    if (!confirmTarget) {
      return
    }

    if (confirmTarget.type === "create") {
      createSlip.mutate(confirmTarget.id, {
        onSuccess: () => setConfirmTarget(null),
      })
      return
    }

    confirmCustomer.mutate(confirmTarget.id, {
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
          <h1 className="font-heading text-3xl font-semibold">Giao khách</h1>
        </div>
        <Button
          type="button"
          variant="outline"
          disabled={readyOrdersQuery.isFetching || exportsQuery.isFetching}
          onClick={refresh}
        >
          <RefreshCwIcon className="size-4" />
          Làm mới
        </Button>
      </div>

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Đơn sẵn sàng tạo phiếu giao khách</CardTitle>
        </CardHeader>
        <CardContent>
          {readyOrdersQuery.isLoading ? (
            <Skeleton className="h-72 rounded-lg" />
          ) : readyOrdersQuery.isError ? (
            <ErrorBlock
              message={warehouseErrorMessage(
                readyOrdersQuery.error,
                "Không tải được đơn sẵn sàng giao"
              )}
              onRetry={() => readyOrdersQuery.refetch()}
            />
          ) : (
            <div className="grid gap-4">
              <div className="overflow-x-auto rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Đơn hàng</TableHead>
                      <TableHead>Kho xuất</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Tổng SL</TableHead>
                      <TableHead>Phiếu giao</TableHead>
                      <TableHead className="text-right">Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!readyOrdersQuery.data ||
                    readyOrdersQuery.data.items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={6} className="py-10 text-center">
                          Chưa có đơn sẵn sàng giao.
                        </TableCell>
                      </TableRow>
                    ) : (
                      readyOrdersQuery.data.items.map((order) => (
                        <TableRow key={order.maDonHang}>
                          <TableCell>
                            <div className="font-medium">{order.maDonHang}</div>
                            <div className="text-xs text-muted-foreground">
                              {formatDateTime(order.ngayTao)}
                            </div>
                          </TableCell>
                          <TableCell>
                            {order.maKhoXuat} - {order.tenKhoXuat}
                          </TableCell>
                          <TableCell>
                            <WarehouseStatusBadge status={order.trangThaiDH} />
                          </TableCell>
                          <TableCell>{order.tongSoLuong}</TableCell>
                          <TableCell>
                            {order.daCoPhieuXuatGiaoKhach
                              ? order.maPhieuXuatGiaoKhach
                              : "Chưa tạo"}
                          </TableCell>
                          <TableCell className="text-right">
                            <Button
                              type="button"
                              size="sm"
                              disabled={order.daCoPhieuXuatGiaoKhach}
                              onClick={() =>
                                setConfirmTarget({
                                  type: "create",
                                  id: order.maDonHang,
                                })
                              }
                            >
                              Tạo phiếu
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
              {readyOrdersQuery.data && (
                <Pagination
                  page={readyOrdersQuery.data.page}
                  totalPages={readyOrdersQuery.data.totalPages}
                  last={readyOrdersQuery.data.last}
                  onPageChange={setReadyPage}
                />
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="bg-white/95">
        <CardHeader>
          <CardTitle>Phiếu xuất giao khách đang chờ xác nhận</CardTitle>
        </CardHeader>
        <CardContent>
          {exportsQuery.isLoading ? (
            <Skeleton className="h-72 rounded-lg" />
          ) : exportsQuery.isError ? (
            <ErrorBlock
              message={warehouseErrorMessage(
                exportsQuery.error,
                "Không tải được phiếu giao khách"
              )}
              onRetry={() => exportsQuery.refetch()}
            />
          ) : (
            <div className="grid gap-4">
              <div className="overflow-x-auto rounded-lg border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Phiếu xuất</TableHead>
                      <TableHead>Đơn hàng</TableHead>
                      <TableHead>Trạng thái</TableHead>
                      <TableHead>Tổng SL</TableHead>
                      <TableHead className="text-right">Thao tác</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {!exportsQuery.data || exportsQuery.data.items.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={5} className="py-10 text-center">
                          Chưa có phiếu giao khách đang chờ xác nhận.
                        </TableCell>
                      </TableRow>
                    ) : (
                      exportsQuery.data.items.map((item) => (
                        <TableRow key={item.maPhieuXuat}>
                          <TableCell>
                            <div className="font-medium">{item.maPhieuXuat}</div>
                            <div className="text-xs text-muted-foreground">
                              {formatDateTime(item.ngayTao)}
                            </div>
                          </TableCell>
                          <TableCell>{item.maDonHang}</TableCell>
                          <TableCell>
                            <WarehouseStatusBadge status={item.trangThaiXuat} />
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
                              <Button
                                type="button"
                                size="sm"
                                onClick={() =>
                                  setConfirmTarget({
                                    type: "confirm",
                                    id: item.maPhieuXuat,
                                  })
                                }
                              >
                                Xác nhận giao
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
              {exportsQuery.data && (
                <Pagination
                  page={exportsQuery.data.page}
                  totalPages={exportsQuery.data.totalPages}
                  last={exportsQuery.data.last}
                  onPageChange={setExportPage}
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
          confirmTarget?.type === "create"
            ? "Tạo phiếu xuất giao khách"
            : "Xác nhận xuất giao khách"
        }
        description={
          confirmTarget?.type === "create"
            ? `Đơn ${confirmTarget.id} sẽ được tạo phiếu xuất giao khách.`
            : confirmTarget
              ? `Phiếu ${confirmTarget.id} sẽ được xác nhận và trừ tồn kho.`
              : ""
        }
        confirmLabel={
          confirmTarget?.type === "create" ? "Tạo phiếu" : "Xác nhận giao"
        }
        isPending={isPending}
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

function formatDateTime(value?: string | null) {
  if (!value) {
    return "-"
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value))
}
