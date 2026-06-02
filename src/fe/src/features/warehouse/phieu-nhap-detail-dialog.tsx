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
import { useWarehouseImportDetail } from "@/features/warehouse/use-warehouse-imports"
import { warehouseErrorMessage } from "@/features/warehouse/warehouse-error"
import { WarehouseStatusBadge } from "@/features/warehouse/warehouse-status"
import { formatVnd } from "@/lib/format"

export function PhieuNhapDetailDialog({
  maPhieuNhap,
  open,
  onOpenChange,
}: {
  maPhieuNhap?: string | null
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const detailQuery = useWarehouseImportDetail(open ? maPhieuNhap : null)
  const detail = detailQuery.data

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>Chi tiết phiếu nhập</DialogTitle>
          <DialogDescription>{maPhieuNhap ?? "-"}</DialogDescription>
        </DialogHeader>

        {detailQuery.isLoading ? (
          <Skeleton className="h-72 rounded-lg" />
        ) : detailQuery.isError ? (
          <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            {warehouseErrorMessage(detailQuery.error, "Không tải được phiếu nhập")}
          </div>
        ) : detail ? (
          <div className="grid gap-4">
            <div className="grid gap-3 rounded-lg border bg-muted/30 p-4 text-sm md:grid-cols-3">
              <Info label="Mã đơn" value={detail.maDonHang} />
              <div className="grid gap-1">
                <span className="text-muted-foreground">Trạng thái nhập</span>
                <WarehouseStatusBadge status={detail.trangThaiNhap} />
              </div>
              <div className="grid gap-1">
                <span className="text-muted-foreground">Trạng thái xuất nguồn</span>
                <WarehouseStatusBadge status={detail.sourceExportStatus} />
              </div>
              <Info
                label="Kho xuất"
                value={`${detail.maKhoXuat} - ${detail.tenKhoXuat ?? "Remote"}`}
              />
              <Info label="Kho nhập" value={`${detail.maKhoNhap} - ${detail.tenKhoNhap}`} />
              <Info label="Ngày nhập" value={formatDateTime(detail.ngayNhap)} />
            </div>

            <div className="overflow-x-auto rounded-lg border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Sản phẩm</TableHead>
                    <TableHead>Số lượng</TableHead>
                    <TableHead>Đơn giá</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {detail.items.map((item) => (
                    <TableRow key={item.maCTPN}>
                      <TableCell>
                        <div className="font-medium">{item.tenSP}</div>
                        <div className="text-xs text-muted-foreground">{item.maSP}</div>
                      </TableCell>
                      <TableCell>{item.soLuong}</TableCell>
                      <TableCell>{formatVnd(item.donGiaNhap)}</TableCell>
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
