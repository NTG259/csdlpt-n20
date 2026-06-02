"use client"

import { ShoppingBagIcon, Trash2Icon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { formatVnd } from "@/lib/format"
import type { GioHang } from "@/types/cart"

import { validateCart } from "./cart-rules"

interface CartSummaryProps {
  cart: GioHang
  isClearing?: boolean
  isCheckingOut?: boolean
  onClearCart: () => void
  onCheckout: () => void
}

export function CartSummary({
  cart,
  isClearing,
  isCheckingOut,
  onClearCart,
  onCheckout,
}: CartSummaryProps) {
  const validation = validateCart(cart)

  return (
    <Card className="h-fit lg:sticky lg:top-24">
      <CardHeader>
        <CardTitle>Tom tat gio hang</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4">
        <div className="space-y-3 text-sm">
          <div className="flex items-center justify-between gap-4">
            <span className="text-muted-foreground">So luong con hang</span>
            <span className="font-medium">{cart.tongSoLuong}</span>
          </div>
          <div className="flex items-center justify-between gap-4 text-lg font-semibold">
            <span>Tong tien</span>
            <span>{formatVnd(cart.tongTien)}</span>
          </div>
        </div>

        {validation.hasHetHang ? (
          <p className="rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
            San pham het hang khong duoc tinh vao tong tien va se khong duoc dat.
          </p>
        ) : null}

        {!validation.canCheckout && validation.blockReason ? (
          <p className="rounded-lg border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {validation.blockReason}
          </p>
        ) : null}

        <Button
          type="button"
          size="lg"
          disabled={!validation.canCheckout || isCheckingOut}
          title={validation.canCheckout ? undefined : validation.blockReason}
          onClick={onCheckout}
        >
          <ShoppingBagIcon className="size-4" />
          Dat hang
        </Button>

        <Button
          type="button"
          variant="outline"
          disabled={isClearing}
          onClick={onClearCart}
        >
          <Trash2Icon className="size-4" />
          Xoa toan bo gio
        </Button>
      </CardContent>
    </Card>
  )
}
