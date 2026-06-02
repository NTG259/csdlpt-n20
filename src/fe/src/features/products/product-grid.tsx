import { Skeleton } from "@/components/ui/skeleton"
import { EmptyState } from "@/components/shared/empty-state"
import type { ProductListItem } from "@/types/domain"

import { ProductCard } from "./product-card"

interface ProductGridProps {
  products: ProductListItem[]
  isLoading?: boolean
  searchTerm?: string
  onAddToCart?: (product: ProductListItem) => void
}

export function ProductGrid({
  products,
  isLoading,
  searchTerm,
  onAddToCart,
}: ProductGridProps) {
  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <div key={index} className="space-y-3 rounded-lg border p-3">
            <Skeleton className="aspect-[4/3] w-full rounded-md" />
            <Skeleton className="h-5 w-4/5" />
            <Skeleton className="h-4 w-2/3" />
            <Skeleton className="h-9 w-full" />
          </div>
        ))}
      </div>
    )
  }

  if (products.length === 0) {
    return (
      <EmptyState
        title="Không có sản phẩm phù hợp"
        description={
          searchTerm
            ? "Không tìm thấy sản phẩm khớp từ khóa trên trang hiện tại."
            : "Thử đổi bộ lọc hoặc quay lại sau."
        }
      />
    )
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {products.map((product) => (
        <ProductCard
          key={product.maSP}
          product={product}
          onAddToCart={onAddToCart}
        />
      ))}
    </div>
  )
}
