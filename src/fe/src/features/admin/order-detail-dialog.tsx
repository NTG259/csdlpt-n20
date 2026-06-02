"use client"

import { useState } from "react"
import { Loader2Icon } from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  TRANG_THAI_DH_OPTIONS,
  TRANG_THAI_TT_OPTIONS,
  TrangThaiDHBadge,
  TrangThaiTTBadge,
} from "@/features/admin/order-status-badge"
import {
  useAdminOrderDetail,
  useUpdateOrderStatus,
} from "@/features/admin/use-admin-orders"
import { formatVnd } from "@/lib/format"
import type { AdminOrderItem } from "@/types/admin"

interface Props {
  order: AdminOrderItem | null
  onClose: () => void
}

export function OrderDetailDialog({ order, onClose }: Props) {
  return (
    <Dialog open={Boolean(order)} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] w-full sm:max-w-3xl overflow-y-auto">
        {order && <DialogBody order={order} onClose={onClose} />}
      </DialogContent>
    </Dialog>
  )
}

function DialogBody({
  order,
  onClose,
}: {
  order: AdminOrderItem
  onClose: () => void
}) {
  const { data: detail, isLoading } = useAdminOrderDetail(
    order.maDonHang,
    order.siteNguon
  )

  const { mutate: updateStatus, isPending } = useUpdateOrderStatus()

  const [newTrangThaiDH, setNewTrangThaiDH] = useState<string>("")
  const [newTrangThaiTT, setNewTrangThaiTT] = useState<string>("")

  function handleUpdate() {
    if (!newTrangThaiDH && !newTrangThaiTT) return
    updateStatus(
      {
        maDonHang: order.maDonHang,
        body: {
          siteNguon: order.siteNguon,
          trangThaiDH: newTrangThaiDH || null,
          trangThaiTT: newTrangThaiTT || null,
        },
      },
      { onSuccess: onClose }
    )
  }

  const current = detail ?? order

  return (
    <>
      <DialogHeader>
        <DialogTitle className="font-mono text-sm">
          {order.maDonHang}
        </DialogTitle>
        <DialogDescription>
          {order.siteNguon} · {order.maKhuVucXuLi}
        </DialogDescription>
      </DialogHeader>

      <div className="grid gap-6 py-2">
        <div className="grid gap-3 text-sm md:grid-cols-2">
          <InfoRow label="Người nhận" value={current.hoTenNguoiNhan} />
          <InfoRow label="Điện thoại" value={current.soDienThoaiNhan} />
          {detail && (
            <InfoRow
              label="Địa chỉ"
              value={detail.diaChiGiao}
              className="md:col-span-2"
            />
          )}
          <InfoRow label="Ngày đặt" value={new Date(current.ngayDat).toLocaleString("vi-VN")} />
          <InfoRow label="Thanh toán" value={current.phuongThucTT} />
          <div className="flex items-center gap-2">
            <span className="w-28 shrink-0 text-muted-foreground">
              Trạng thái ĐH
            </span>
            <TrangThaiDHBadge value={current.trangThaiDH} />
          </div>
          <div className="flex items-center gap-2">
            <span className="w-28 shrink-0 text-muted-foreground">
              Trạng thái TT
            </span>
            <TrangThaiTTBadge value={current.trangThaiTT} />
          </div>
          {detail?.ghiChu && (
            <InfoRow
              label="Ghi chú"
              value={detail.ghiChu}
              className="md:col-span-2"
            />
          )}
        </div>

        <Separator />

        <div>
          <p className="mb-3 text-sm font-semibold">Chi tiết sản phẩm</p>
          {isLoading ? (
            <Skeleton className="h-24 rounded-xl" />
          ) : detail ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã SP</TableHead>
                  <TableHead>Tên sản phẩm</TableHead>
                  <TableHead className="text-right">SL</TableHead>
                  <TableHead className="text-right">Đơn giá</TableHead>
                  <TableHead className="text-right">Thành tiền</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {detail.chiTiet.map((item) => (
                  <TableRow key={item.maCTDH}>
                    <TableCell className="font-mono text-xs">
                      {item.maSP}
                    </TableCell>
                    <TableCell>{item.tenSP}</TableCell>
                    <TableCell className="text-right">{item.soLuong}</TableCell>
                    <TableCell className="text-right">
                      {formatVnd(item.donGia)}
                    </TableCell>
                    <TableCell className="text-right font-medium">
                      {formatVnd(item.thanhTien)}
                    </TableCell>
                  </TableRow>
                ))}
                <TableRow className="bg-slate-50 font-semibold">
                  <TableCell colSpan={4} className="text-right">
                    Tổng cộng
                  </TableCell>
                  <TableCell className="text-right">
                    {formatVnd(current.tongTien)}
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          ) : null}
        </div>

        <Separator />

        <div className="grid gap-4">
          <p className="text-sm font-semibold">Cập nhật trạng thái</p>
          <div className="grid gap-4 md:grid-cols-2">
            <div className="grid gap-2">
              <Label>Trạng thái đơn hàng</Label>
              <Select
                value={newTrangThaiDH}
                onValueChange={(v) => setNewTrangThaiDH(v ?? "")}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Giữ nguyên" />
                </SelectTrigger>
                <SelectContent>
                  {TRANG_THAI_DH_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label>Trạng thái thanh toán</Label>
              <Select
                value={newTrangThaiTT}
                onValueChange={(v) => setNewTrangThaiTT(v ?? "")}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Giữ nguyên" />
                </SelectTrigger>
                <SelectContent>
                  {TRANG_THAI_TT_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              type="button"
              disabled={isPending || (!newTrangThaiDH && !newTrangThaiTT)}
              onClick={handleUpdate}
            >
              {isPending && <Loader2Icon className="size-4 animate-spin" />}
              Lưu thay đổi
            </Button>
            <Button type="button" variant="outline" onClick={onClose}>
              Đóng
            </Button>
          </div>
        </div>
      </div>
    </>
  )
}

function InfoRow({
  label,
  value,
  className,
}: {
  label: string
  value: string
  className?: string
}) {
  return (
    <div className={`flex gap-2 ${className ?? ""}`}>
      <span className="w-28 shrink-0 text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  )
}
