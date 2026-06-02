"use client"

/* eslint-disable @next/next/no-img-element */
import { useEffect, useRef, useState } from "react"
import {
  AlertTriangleIcon,
  Loader2Icon,
  MinusIcon,
  PlusIcon,
  Trash2Icon,
} from "lucide-react"
import { toast } from "sonner"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { formatVnd, productImageUrl } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { ChiTietGioHang } from "@/types/cart"

import { getLineStatus } from "./cart-rules"
import { useRemoveCartItem, useUpdateCartItem } from "./use-cart"

interface CartItemRowProps {
  item: ChiTietGioHang
}

const QUANTITY_UPDATE_DELAY_MS = 650

function getStatusBadge(item: ChiTietGioHang) {
  const status = getLineStatus(item)

  if (status === "het_hang") {
    return (
      <Badge variant="destructive">
        <AlertTriangleIcon className="size-3" />
        Hết hàng
      </Badge>
    )
  }

  if (status === "vuot_ton") {
    return (
      <Badge variant="outline" className="border-amber-300 text-amber-700">
        Vượt tồn, chỉ còn {item.soLuongKhaDung}
      </Badge>
    )
  }

  if (item.soLuongKhaDung <= 5) {
    return <Badge variant="outline">Còn {item.soLuongKhaDung}</Badge>
  }

  return null
}

export function CartItemRow({ item }: CartItemRowProps) {
  const updateCartItem = useUpdateCartItem()
  const removeCartItem = useRemoveCartItem()
  const updateTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [quantity, setQuantity] = useState(item.soLuong)
  const [isDebouncingQuantity, setIsDebouncingQuantity] = useState(false)
  const status = getLineStatus(item)
  const isOutOfStock = status === "het_hang"
  const isOverStock = status === "vuot_ton"
  const isUpdating = updateCartItem.isPending
  const isRemoving = removeCartItem.isPending
  const isMutating = isUpdating || isRemoving
  const maxQuantity = item.soLuongKhaDung

  useEffect(() => {
    return () => {
      if (updateTimerRef.current) {
        clearTimeout(updateTimerRef.current)
      }
    }
  }, [])

  function clearQueuedUpdate() {
    if (updateTimerRef.current) {
      clearTimeout(updateTimerRef.current)
      updateTimerRef.current = null
    }

    setIsDebouncingQuantity(false)
  }

  function normalizeQuantity(nextQuantity: number) {
    if (!Number.isInteger(nextQuantity) || nextQuantity < 1) {
      return null
    }

    if (maxQuantity > 0 && nextQuantity > maxQuantity) {
      toast.warning(`Chỉ còn ${maxQuantity} ${item.donViTinh}`)
      return maxQuantity
    }

    return nextQuantity
  }

  function queueQuantityUpdate(nextQuantity: number) {
    const normalizedQuantity = normalizeQuantity(nextQuantity)

    if (!normalizedQuantity) {
      return
    }

    setQuantity(normalizedQuantity)

    if (normalizedQuantity === item.soLuong) {
      clearQueuedUpdate()
      return
    }

    if (updateTimerRef.current) {
      clearTimeout(updateTimerRef.current)
    }

    setIsDebouncingQuantity(true)
    updateTimerRef.current = setTimeout(() => {
      updateTimerRef.current = null
      setIsDebouncingQuantity(false)
      updateCartItem.mutate({
        maSP: item.maSP,
        soLuong: normalizedQuantity,
      })
    }, QUANTITY_UPDATE_DELAY_MS)
  }

  function updateQuantityImmediately(nextQuantity: number) {
    const normalizedQuantity = normalizeQuantity(nextQuantity)

    if (!normalizedQuantity) {
      return
    }

    clearQueuedUpdate()
    setQuantity(normalizedQuantity)
    updateCartItem.mutate({
      maSP: item.maSP,
      soLuong: normalizedQuantity,
    })
  }

  function removeItem() {
    clearQueuedUpdate()
    removeCartItem.mutate(item.maSP)
  }

  return (
    <div
      className={cn(
        "grid gap-4 rounded-xl border bg-background p-4 shadow-sm md:grid-cols-[88px_minmax(0,1fr)_auto] md:items-center",
        isOutOfStock && "opacity-65",
        isOverStock && "border-amber-300 bg-amber-50/60"
      )}
    >
      <div className="overflow-hidden rounded-lg border bg-muted">
        <img
          src={productImageUrl(item.hinhAnh)}
          alt={item.tenSP}
          className="aspect-square size-full object-cover"
          loading="lazy"
        />
      </div>

      <div className="min-w-0 space-y-3">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="min-w-0">
            <p className="font-semibold leading-6">{item.tenSP}</p>
            <p className="text-sm text-muted-foreground">
              {formatVnd(item.giaBan)} / {item.donViTinh}
            </p>
          </div>
          {getStatusBadge(item)}
        </div>

        {isOverStock ? (
          <div className="flex flex-wrap items-center gap-2 text-sm text-amber-700">
            <span>
              Số lượng trong giỏ đang vượt tồn kho khả dụng.
            </span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={isMutating}
              onClick={() => updateQuantityImmediately(maxQuantity)}
            >
              {isUpdating ? (
                <Loader2Icon className="size-4 animate-spin" />
              ) : null}
              Cập nhật về {maxQuantity}
            </Button>
          </div>
        ) : null}

        <div className="flex flex-wrap items-center gap-3">
          {!isOutOfStock ? (
            <div className="flex flex-wrap items-center gap-2">
              <div className="flex items-center gap-1 rounded-lg border bg-background p-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  disabled={isMutating || quantity <= 1}
                  onClick={() => queueQuantityUpdate(quantity - 1)}
                  aria-label="Giảm số lượng"
                >
                  <MinusIcon className="size-4" />
                </Button>
                <Input
                  type="number"
                  inputMode="numeric"
                  min={1}
                  max={maxQuantity}
                  value={quantity}
                  disabled={isMutating}
                  className="h-7 w-16 text-center"
                  onChange={(event) =>
                    queueQuantityUpdate(Number(event.target.value))
                  }
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  disabled={isMutating || quantity >= maxQuantity}
                  onClick={() => queueQuantityUpdate(quantity + 1)}
                  aria-label="Tăng số lượng"
                >
                  <PlusIcon className="size-4" />
                </Button>
              </div>
              {isDebouncingQuantity || isUpdating ? (
                <span className="inline-flex items-center gap-1.5 text-xs text-muted-foreground">
                  {isUpdating ? (
                    <Loader2Icon className="size-3.5 animate-spin" />
                  ) : null}
                  {isUpdating ? "Đang cập nhật..." : "Sẽ cập nhật sau giây lát"}
                </span>
              ) : null}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              Sản phẩm này không còn khả dụng, chỉ có thể xóa khỏi giỏ.
            </p>
          )}

          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={isMutating}
            onClick={removeItem}
          >
            {isRemoving ? (
              <Loader2Icon className="size-4 animate-spin" />
            ) : (
              <Trash2Icon className="size-4" />
            )}
            Xóa
          </Button>
        </div>
      </div>

      <div className="text-left md:text-right">
        <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">
          Thành tiền
        </p>
        <p className="text-lg font-semibold">
          {formatVnd(item.giaBan * quantity)}
        </p>
      </div>
    </div>
  )
}
