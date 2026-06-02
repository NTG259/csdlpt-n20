"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { ArrowRightIcon } from "lucide-react"
import { toast } from "sonner"

import { EmptyState } from "@/components/shared/empty-state"
import { Button, buttonVariants } from "@/components/ui/button"
import { useAuth } from "@/features/auth/auth-context"
import { useAddToCart } from "@/features/cart/use-cart"
import { cn } from "@/lib/utils"
import type { ProductListItem } from "@/types/domain"

import { ProductGrid } from "./product-grid"
import { useProducts } from "./use-products"

export function HomeProducts() {
  const router = useRouter()
  const pathname = usePathname()
  const { isAuthenticated, region } = useAuth()
  const addToCart = useAddToCart()
  const { data, isLoading, isError, error } = useProducts({
    page: 0,
    size: 8,
    sort: "ngayTao,desc",
    trangThai: true,
  })

  function handleAddToCart(product: ProductListItem) {
    if (!isAuthenticated) {
      router.push(`/login?next=${encodeURIComponent(pathname)}`)
      return
    }

    if (!region) {
      toast.error("Tài khoản chưa gán khu vực hợp lệ.")
      return
    }

    addToCart.mutate({ maSP: product.maSP, soLuong: 1 })
  }

  return (
    <section className="grid gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-muted-foreground">
            Sản phẩm mới
          </p>
        </div>
        <Link
          href="/products"
          className={cn(buttonVariants({ variant: "outline" }), "gap-2")}
        >
          Xem tất cả
          <ArrowRightIcon className="size-4" />
        </Link>
      </div>

      {isError ? (
        <EmptyState
          title="Không tải được sản phẩm mới"
          description={error instanceof Error ? error.message : "Vui lòng thử lại."}
          action={
            <Button type="button" variant="outline" onClick={() => router.refresh()}>
              Tải lại
            </Button>
          }
        />
      ) : (
        <ProductGrid
          products={data?.items ?? []}
          isLoading={isLoading}
          onAddToCart={handleAddToCart}
        />
      )}
    </section>
  )
}
