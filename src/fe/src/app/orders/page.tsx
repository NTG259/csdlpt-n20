"use client"

import { useState } from "react"
import Link from "next/link"
import {
  ClipboardListIcon,
  EyeIcon,
  PackageCheckIcon,
  PackageIcon,
  RotateCcwIcon,
  ShoppingCartIcon,
} from "lucide-react"

import { EmptyState } from "@/components/shared/empty-state"
import { PageContainer } from "@/components/shared/page-container"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { useAuth } from "@/features/auth/auth-context"
import { useConfirmReceived } from "@/features/orders/use-confirm-received"
import { useOrderDetail, useOrders } from "@/features/orders/use-orders"
import { formatVnd } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { DonHang, DonHangSummary } from "@/types/order"

const PAGE_SIZE = 10

const ORDER_STATUS_LABEL: Record<string, string> = {
  pending: "Cho xu ly",
  processing: "Dang xu ly",
  shipping: "Dang giao",
  completed: "Hoan thanh",
  cancelled: "Da huy",
}

const PAYMENT_STATUS_LABEL: Record<string, string> = {
  waiting_cod: "Cho COD",
  paid: "Da thanh toan",
  failed: "Thanh toan loi",
  cancelled: "Da huy",
}

function formatDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date)
}

function orderStatusLabel(status: string) {
  return ORDER_STATUS_LABEL[status] ?? status
}

function paymentStatusLabel(status: string) {
  return PAYMENT_STATUS_LABEL[status] ?? status
}

function OrdersSkeleton() {
  return (
    <PageContainer className="grid gap-6">
      <Skeleton className="h-28 rounded-3xl" />
      <div className="grid gap-3">
        {Array.from({ length: 4 }).map((_, index) => (
          <Skeleton key={index} className="h-36 rounded-xl" />
        ))}
      </div>
    </PageContainer>
  )
}

function OrderCard({
  order,
  onView,
  onConfirmReceived,
  confirmPending,
}: {
  order: DonHangSummary
  onView: (maDonHang: string) => void
  onConfirmReceived: (order: DonHangSummary) => void
  confirmPending: boolean
}) {
  return (
    <Card>
      <CardContent className="grid gap-4 p-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
        <div className="grid gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="secondary">{orderStatusLabel(order.trangThaiDH)}</Badge>
            <Badge variant="outline">{paymentStatusLabel(order.trangThaiTT)}</Badge>
            <span className="text-xs text-muted-foreground">
              {formatDateTime(order.ngayDat)}
            </span>
          </div>

          <div className="grid gap-1">
            <p className="font-medium">Don #{order.maDonHang}</p>
            <p className="text-sm text-muted-foreground">
              {order.hoTenNguoiNhan} - {order.soDienThoaiNhan}
            </p>
            <p className="line-clamp-2 text-sm text-muted-foreground">
              {order.diaChiGiao}
            </p>
          </div>
        </div>

        <div className="grid gap-3 sm:min-w-44 sm:justify-items-end">
          <p className="text-lg font-semibold">{formatVnd(order.tongTien)}</p>
          <div className="flex flex-wrap gap-2 sm:justify-end">
            {order.trangThaiDH === "shipping" ? (
              <Button
                type="button"
                disabled={confirmPending}
                onClick={() => onConfirmReceived(order)}
              >
                <PackageCheckIcon className="size-4" />
                {confirmPending ? "Dang xac nhan" : "Da nhan hang"}
              </Button>
            ) : null}
            <Button type="button" variant="outline" onClick={() => onView(order.maDonHang)}>
              <EyeIcon className="size-4" />
              Xem chi tiet
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}

function OrderDetailDialog({
  maDonHang,
  open,
  onOpenChange,
  onConfirmReceived,
  confirmPending,
}: {
  maDonHang: string | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirmReceived: (order: DonHang) => void
  confirmPending: boolean
}) {
  const detailQuery = useOrderDetail(maDonHang)
  const order = detailQuery.data

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Chi tiet don hang</DialogTitle>
          <DialogDescription>
            {maDonHang ? `Ma don: ${maDonHang}` : "Chua chon don hang"}
          </DialogDescription>
        </DialogHeader>

        {detailQuery.isLoading ? (
          <div className="grid gap-3">
            <Skeleton className="h-20 rounded-lg" />
            <Skeleton className="h-28 rounded-lg" />
          </div>
        ) : detailQuery.isError ? (
          <p className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {detailQuery.error instanceof Error
              ? detailQuery.error.message
              : "Khong tai duoc chi tiet don hang."}
          </p>
        ) : order ? (
          <OrderDetailContent
            order={order}
            onConfirmReceived={onConfirmReceived}
            confirmPending={confirmPending}
          />
        ) : null}
      </DialogContent>
    </Dialog>
  )
}

function OrderDetailContent({
  order,
  onConfirmReceived,
  confirmPending,
}: {
  order: DonHang
  onConfirmReceived: (order: DonHang) => void
  confirmPending: boolean
}) {
  return (
    <div className="grid gap-4">
      <div className="grid gap-2 rounded-lg border bg-muted/30 p-3 text-sm">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">{orderStatusLabel(order.trangThaiDH)}</Badge>
          <Badge variant="outline">{paymentStatusLabel(order.trangThaiTT)}</Badge>
          <span className="text-muted-foreground">{formatDateTime(order.ngayDat)}</span>
        </div>
        <p>
          <span className="text-muted-foreground">Nguoi nhan: </span>
          {order.hoTenNguoiNhan} - {order.soDienThoaiNhan}
        </p>
        <p>
          <span className="text-muted-foreground">Dia chi: </span>
          {order.diaChiGiao}
        </p>
        {order.ghiChu ? (
          <p>
            <span className="text-muted-foreground">Ghi chu: </span>
            {order.ghiChu}
          </p>
        ) : null}
      </div>

      <div className="grid gap-2">
        {order.items.map((item) => (
          <div
            key={item.maSP}
            className="grid gap-2 rounded-lg border p-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center"
          >
            <div>
              <p className="font-medium">{item.tenSP}</p>
              <p className="text-sm text-muted-foreground">
                {item.maSP} - SL {item.soLuong} x {formatVnd(item.donGia)}
              </p>
            </div>
            <p className="font-semibold">{formatVnd(item.thanhTien)}</p>
          </div>
        ))}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-3">
        <div className="text-base font-semibold">
          <span>Tong tien</span>{" "}
          <span>{formatVnd(order.tongTien)}</span>
        </div>
        {order.trangThaiDH === "shipping" ? (
          <Button
            type="button"
            disabled={confirmPending}
            onClick={() => onConfirmReceived(order)}
          >
            <PackageCheckIcon className="size-4" />
            {confirmPending ? "Dang xac nhan" : "Da nhan hang"}
          </Button>
        ) : null}
      </div>
    </div>
  )
}

function ConfirmReceivedDialog({
  order,
  open,
  pending,
  onOpenChange,
  onConfirm,
}: {
  order: DonHang | DonHangSummary | null
  open: boolean
  pending: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: () => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Xac nhan da nhan hang</DialogTitle>
          <DialogDescription>
            {order
              ? `Xac nhan ban da nhan don ${order.maDonHang}. Thao tac nay se chuyen don sang hoan thanh.`
              : "Chua chon don hang."}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={pending}
            onClick={() => onOpenChange(false)}
          >
            Huy
          </Button>
          <Button type="button" disabled={!order || pending} onClick={onConfirm}>
            <PackageCheckIcon className="size-4" />
            {pending ? "Dang xac nhan" : "Xac nhan"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export default function OrdersPage() {
  const [page, setPage] = useState(0)
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null)
  const [confirmOrder, setConfirmOrder] = useState<DonHang | DonHangSummary | null>(null)
  const { isAuthenticated, region } = useAuth()
  const ordersQuery = useOrders(page, PAGE_SIZE)
  const confirmReceived = useConfirmReceived()
  const orders = ordersQuery.data

  const handleConfirmReceived = () => {
    if (!confirmOrder) {
      return
    }

    confirmReceived.mutate(confirmOrder.maDonHang, {
      onSuccess: () => setConfirmOrder(null),
    })
  }

  if (!isAuthenticated) {
    return (
      <PageContainer>
        <EmptyState
          title="Ban can dang nhap de xem don hang"
          description="Don hang duoc luu theo tai khoan va khu vuc cua ban."
          action={
            <Link
              href="/login?next=/orders"
              className={cn(buttonVariants({ variant: "default" }), "gap-2")}
            >
              Dang nhap
            </Link>
          }
        />
      </PageContainer>
    )
  }

  if (!region) {
    return (
      <PageContainer>
        <EmptyState
          title="Tai khoan chua co khu vuc hop le"
          description="Khong the chon Site Bac/Nam de tai danh sach don hang."
        />
      </PageContainer>
    )
  }

  if (ordersQuery.isLoading) {
    return <OrdersSkeleton />
  }

  if (ordersQuery.isError) {
    return (
      <PageContainer>
        <EmptyState
          title="Khong tai duoc danh sach don hang"
          description={
            ordersQuery.error instanceof Error
              ? ordersQuery.error.message
              : "Vui long thu lai."
          }
          action={
            <Button type="button" variant="outline" onClick={() => ordersQuery.refetch()}>
              <RotateCcwIcon className="size-4" />
              Thu lai
            </Button>
          }
        />
      </PageContainer>
    )
  }

  return (
    <PageContainer className="grid gap-6">
      <section className="grid gap-3 rounded-3xl border bg-[linear-gradient(135deg,_rgba(255,255,255,0.98),_rgba(248,250,252,0.94))] p-6 shadow-sm">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">
            <ClipboardListIcon className="size-3" />
            Don hang
          </Badge>
        </div>
        <div className="max-w-3xl space-y-2">
          <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Don hang cua toi
          </h1>
          <p className="text-sm leading-6 text-muted-foreground sm:text-base">
            Theo doi cac don da dat tren site khu vuc cua tai khoan hien tai.
          </p>
        </div>
      </section>

      {!orders || orders.items.length === 0 ? (
        <EmptyState
          title="Chua co don hang"
          description="Khi ban dat hang thanh cong, don se xuat hien tai day."
          action={
            <Link
              href="/cart"
              className={cn(buttonVariants({ variant: "default" }), "gap-2")}
            >
              <ShoppingCartIcon className="size-4" />
              Den gio hang
            </Link>
          }
        />
      ) : (
        <>
          <div className="grid gap-3">
            {orders.items.map((order) => (
              <OrderCard
                key={order.maDonHang}
                order={order}
                onView={setSelectedOrderId}
                onConfirmReceived={setConfirmOrder}
                confirmPending={
                  confirmReceived.isPending &&
                  confirmReceived.variables === order.maDonHang
                }
              />
            ))}
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t pt-4 text-sm">
            <div className="flex items-center gap-2 text-muted-foreground">
              <PackageIcon className="size-4" />
              {orders.totalElements} don hang
            </div>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                disabled={orders.page <= 0}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
              >
                Trang truoc
              </Button>
              <span className="min-w-24 text-center text-muted-foreground">
                {orders.page + 1}/{Math.max(orders.totalPages, 1)}
              </span>
              <Button
                type="button"
                variant="outline"
                disabled={orders.last}
                onClick={() => setPage((current) => current + 1)}
              >
                Trang sau
              </Button>
            </div>
          </div>
        </>
      )}

      <OrderDetailDialog
        maDonHang={selectedOrderId}
        open={Boolean(selectedOrderId)}
        onConfirmReceived={setConfirmOrder}
        confirmPending={
          confirmReceived.isPending &&
          confirmReceived.variables === selectedOrderId
        }
        onOpenChange={(open) => {
          if (!open) {
            setSelectedOrderId(null)
          }
        }}
      />
      <ConfirmReceivedDialog
        order={confirmOrder}
        open={Boolean(confirmOrder)}
        pending={confirmReceived.isPending}
        onOpenChange={(open) => {
          if (!open && !confirmReceived.isPending) {
            setConfirmOrder(null)
          }
        }}
        onConfirm={handleConfirmReceived}
      />
    </PageContainer>
  )
}
