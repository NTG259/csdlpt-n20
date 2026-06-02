"use client"

import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { useWarehouseExportDetail } from "@/features/warehouse/use-warehouse-exports"
import { warehouseErrorMessage } from "@/features/warehouse/warehouse-error"
import {
  WarehouseStatusBadge,
  warehouseStatusLabel,
} from "@/features/warehouse/warehouse-status"

export function PhieuXuatDetailDialog({
  maPhieuXuat,
  open,
  onOpenChange,
}: {
  maPhieuXuat?: string | null
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const detailQuery = useWarehouseExportDetail(open ? maPhieuXuat : null)
  const detail = detailQuery.data

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>Chi tiết phiếu xuất</DialogTitle>
          <DialogDescription>{maPhieuXuat ?? "-"}</DialogDescription>
        </DialogHeader>

        {detailQuery.isLoading ? (
          <Skeleton className="h-72 rounded-lg" />
        ) : detailQuery.isError ? (
          <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {warehouseErrorMessage(detailQuery.error, "Không tải được phiếu xuất")}
          </div>
        ) : detail ? (
          <div className="grid gap-4">
            <div className="grid gap-3 rounded-lg border bg-muted/30 p-4 text-sm md:grid-cols-3">
              <Info label="Mã đơn" value={detail.maDonHang} />
              <Info label="Loại" value={warehouseStatusLabel(detail.loaiPhieu)} />
              <div className="grid gap-1">
                <span className="text-muted-foreground">Trạng thái</span>
                <div className="flex flex-wrap gap-2">
                  <WarehouseStatusBadge status={detail.trangThaiXuat} />
                  <WarehouseStatusBadge status={detail.trangThaiNhan} />
                </div>
              </div>
              <Info label="Kho xuất" value={`${detail.maKhoXuat} - ${detail.tenKhoXuat}`} />
              <Info
                label="Kho nhận"
                value={
                  detail.maKhoNhan
                    ? `${detail.maKhoNhan} - ${detail.tenKhoNhan ?? "-"}`
                    : "Giao khách"
                }
              />
              <Info label="Ngày tạo" value={formatDateTime(detail.ngayTao)} />
            </div>

            <div className="overflow-x-auto rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Sản phẩm</TableHead>
                    <TableHead>Số lượng xuất</TableHead>
                    <TableHead>Tồn kho</TableHead>
                    <TableHead>Đang giữ</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {detail.items.map((item) => (
                    <TableRow key={item.maCTXK}>
                      <TableCell>
                        <div className="font-medium">{item.tenSP}</div>
                        <div className="text-xs text-muted-foreground">{item.maSP}</div>
                      </TableCell>
                      <TableCell>{item.soLuongXuat}</TableCell>
                      <TableCell>{item.soLuongTon ?? "-"}</TableCell>
                      <TableCell>{item.soLuongDatHang ?? "-"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}

function Info({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="grid gap-1">
      <span className="text-muted-foreground">{label}</span>
      <span className="break-all font-medium">{value || "-"}</span>
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
