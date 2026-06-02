/* eslint-disable @next/next/no-img-element */
import Link from "next/link"
import { EyeIcon, ShoppingCartIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardFooter } from "@/components/ui/card"
import { formatVnd, productImageUrl } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { ProductListItem } from "@/types/domain"

interface ProductCardProps {
  product: ProductListItem
  onAddToCart?: (product: ProductListItem) => void
}

export function ProductCard({ product, onAddToCart }: ProductCardProps) {
  return (
    <Card className="h-full rounded-lg py-0">
      <div className="relative aspect-[4/3] overflow-hidden bg-muted">
        <img
          src={productImageUrl(product.hinhAnh)}
          alt={product.tenSP}
          className="size-full object-cover"
          loading="lazy"
        />
        <Badge
          variant={product.trangThai ? "secondary" : "outline"}
          className="absolute left-3 top-3"
        >
          {product.trangThai ? "Đang bán" : "Ngừng bán"}
        </Badge>
      </div>

      <CardContent className="grid gap-3 p-4">
        <div className="grid gap-1">
          <Link
            href={`/products/${product.maSP}`}
            className="line-clamp-2 min-h-11 text-base font-semibold leading-snug hover:underline"
          >
            {product.tenSP}
          </Link>
          <p className="text-xs text-muted-foreground">
            {product.tenDanhMuc} · {product.tenThuongHieu}
          </p>
        </div>
        <div className="flex items-end justify-between gap-2">
          <p className="text-lg font-semibold">{formatVnd(product.giaBan)}</p>
          <span className="text-xs text-muted-foreground">{product.donViTinh}</span>
        </div>
      </CardContent>

      <CardFooter className="gap-2 p-4">
        <Link
          href={`/products/${product.maSP}`}
          className={cn(
            buttonVariants({ variant: "outline", size: "sm" }),
            "flex-1 gap-2"
          )}
        >
          <EyeIcon className="size-4" />
          Chi tiết
        </Link>
        <Button
          type="button"
          size="sm"
          className="flex-1"
          disabled={!product.trangThai}
          onClick={() => onAddToCart?.(product)}
        >
          <ShoppingCartIcon className="size-4" />
          Thêm
        </Button>
      </CardFooter>
    </Card>
  )
}
