"use client"

import { useState } from "react"
import Link from "next/link"
import { AlertTriangleIcon, RotateCcwIcon, ShoppingCartIcon } from "lucide-react"

import { EmptyState } from "@/components/shared/empty-state"
import { PageContainer } from "@/components/shared/page-container"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useAuth } from "@/features/auth/auth-context"
import { CartItemRow } from "@/features/cart/cart-item-row"
import { CartSummary } from "@/features/cart/cart-summary"
import { useCart, useClearCart, useRemoveCartItem } from "@/features/cart/use-cart"
import { CheckoutDialog } from "@/features/orders/checkout-dialog"
import { cn } from "@/lib/utils"

function CartSkeleton() {
  return (
    <PageContainer className="grid gap-6">
      <Skeleton className="h-32 rounded-3xl" />
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="grid gap-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <Skeleton key={index} className="h-32 rounded-xl" />
          ))}
        </div>
        <Skeleton className="h-72 rounded-xl" />
      </div>
    </PageContainer>
  )
}

export default function CartPage() {
  const [checkoutOpen, setCheckoutOpen] = useState(false)
  const { isAuthenticated, region } = useAuth()
  const { data: cart, isLoading, isError, error, refetch } = useCart()
  const clearCart = useClearCart()
  const removeCartItem = useRemoveCartItem()

  if (!isAuthenticated) {
    return (
      <PageContainer>
        <EmptyState
          title="Bạn cần đăng nhập để xem giỏ hàng"
          description="Giỏ hàng được lưu theo tài khoản và khu vực Bắc/Nam."
          action={
            <Link
              href="/login?next=/cart"
              className={cn(buttonVariants({ variant: "default" }), "gap-2")}
            >
              Đăng nhập
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
          title="Tài khoản chưa có khu vực hợp lệ"
          description="Không thể chọn Site Bắc/Nam để tải giỏ hàng."
        />
      </PageContainer>
    )
  }

  if (isLoading) {
    return <CartSkeleton />
  }

  if (isError) {
    return (
      <PageContainer>
        <EmptyState
          title="Không tải được giỏ hàng"
          description={error instanceof Error ? error.message : "Vui lòng thử lại."}
          action={
            <Button type="button" variant="outline" onClick={() => refetch()}>
              <RotateCcwIcon className="size-4" />
              Thử lại
            </Button>
          }
        />
      </PageContainer>
    )
  }

  if (
    !cart ||
    (cart.sanPhamHopLe.length === 0 && cart.sanPhamHetHang.length === 0)
  ) {
    return (
      <PageContainer>
        <EmptyState
          title="Giỏ hàng trống"
          description="Thêm sản phẩm vào giỏ để bắt đầu đặt hàng."
          action={
            <Link
              href="/products"
              className={cn(buttonVariants({ variant: "default" }), "gap-2")}
            >
              <ShoppingCartIcon className="size-4" />
              Xem sản phẩm
            </Link>
          }
        />
      </PageContainer>
    )
  }

  const activeCart = cart

  function removeOutOfStockItems() {
    for (const item of activeCart.sanPhamHetHang) {
      removeCartItem.mutate(item.maSP)
    }
  }

  return (
    <PageContainer className="grid gap-6">
      <section className="grid gap-3 rounded-3xl border bg-[radial-gradient(circle_at_top_left,_rgba(14,165,233,0.16),_transparent_34%),linear-gradient(135deg,_rgba(255,255,255,0.97),_rgba(248,250,252,0.94))] p-6 shadow-sm">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">Giỏ hàng</Badge>
        </div>
        <div className="max-w-3xl space-y-2">
          <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Giỏ hàng theo khu vực
          </h1>
          <p className="text-sm leading-6 text-muted-foreground sm:text-base">
            Dữ liệu giỏ hàng được gọi từ Site Bắc hoặc Site Nam theo khu vực của
            user, không gọi Site Main.
          </p>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="grid gap-4">
          <Card>
            <CardHeader>
              <CardTitle>Sản phẩm còn hàng</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3">
              {activeCart.sanPhamHopLe.map((item) => (
                <CartItemRow
                  key={`${item.maSP}-${item.soLuong}-${item.soLuongKhaDung}`}
                  item={item}
                />
              ))}
            </CardContent>
          </Card>

          {activeCart.sanPhamHetHang.length > 0 ? (
            <Card className="border-destructive/30">
              <CardHeader className="gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1">
                  <CardTitle className="flex items-center gap-2 text-destructive">
                    <AlertTriangleIcon className="size-5" />
                    Sản phẩm hết hàng
                  </CardTitle>
                  <p className="text-sm text-muted-foreground">
                    Các dòng này không được tính vào tổng tiền.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={removeCartItem.isPending}
                  onClick={removeOutOfStockItems}
                >
                  Xóa tất cả hết hàng
                </Button>
              </CardHeader>
              <CardContent className="grid gap-3">
                {activeCart.sanPhamHetHang.map((item) => (
                  <CartItemRow
                    key={`${item.maSP}-${item.soLuong}-${item.soLuongKhaDung}`}
                    item={item}
                  />
                ))}
              </CardContent>
            </Card>
          ) : null}
        </div>

        <CartSummary
          cart={activeCart}
          isClearing={clearCart.isPending}
          isCheckingOut={checkoutOpen}
          onClearCart={() => clearCart.mutate()}
          onCheckout={() => setCheckoutOpen(true)}
        />
      </div>

      <CheckoutDialog
        cart={activeCart}
        open={checkoutOpen}
        onOpenChange={setCheckoutOpen}
      />
    </PageContainer>
  )
}
