"use client"

/* eslint-disable @next/next/no-img-element */
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import {
  ArrowLeftIcon,
  LayersIcon,
  PackageIcon,
  ShoppingCartIcon,
  TagsIcon,
} from "lucide-react"
import { toast } from "sonner"

import { EmptyState } from "@/components/shared/empty-state"
import { Loading } from "@/components/shared/loading"
import { PageContainer } from "@/components/shared/page-container"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useAuth } from "@/features/auth/auth-context"
import { useAddToCart } from "@/features/cart/use-cart"
import { isApiError } from "@/lib/api-error"
import { formatVnd, productImageUrl } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { ProductDetail } from "@/types/domain"

import { TonKhoHeThong } from "./ton-kho-he-thong"
import { useProduct } from "./use-products"

interface ProductDetailViewProps {
  maSP: string
}

interface ProductTextBlockProps {
  title: string
  content?: string
}

function ProductTextBlock({ title, content }: ProductTextBlockProps) {
  if (!content?.trim()) {
    return null
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="whitespace-pre-line text-sm leading-7 text-muted-foreground">
          {content}
        </p>
      </CardContent>
    </Card>
  )
}

function ProductFacts({ product }: { product: ProductDetail }) {
  const facts = [
    {
      icon: PackageIcon,
      label: "Mã sản phẩm",
      value: product.maSP,
    },
    {
      icon: LayersIcon,
      label: "Danh mục",
      value: product.tenDanhMuc,
    },
    {
      icon: TagsIcon,
      label: "Thương hiệu",
      value: product.tenThuongHieu,
    },
  ]

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {facts.map((fact) => {
        const Icon = fact.icon

        return (
          <div key={fact.label} className="rounded-xl border bg-muted/30 p-4">
            <Icon className="mb-3 size-5 text-muted-foreground" />
            <p className="text-xs uppercase tracking-[0.16em] text-muted-foreground">
              {fact.label}
            </p>
            <p className="mt-1 font-medium">{fact.value}</p>
          </div>
        )
      })}
    </div>
  )
}

export function ProductDetailView({ maSP }: ProductDetailViewProps) {
  const router = useRouter()
  const pathname = usePathname()
  const { isAuthenticated, region } = useAuth()
  const addToCart = useAddToCart()
  const { data: product, isLoading, isError, error } = useProduct(maSP)

  function handleAddToCart() {
    if (!product?.trangThai) {
      return
    }

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

  if (isLoading) {
    return (
      <PageContainer>
        <Loading label="Đang tải chi tiết sản phẩm" />
      </PageContainer>
    )
  }

  if (isError) {
    const isNotFound = isApiError(error) && error.errorCode === "RESOURCE_NOT_FOUND"

    return (
      <PageContainer>
        <EmptyState
          title={
            isNotFound
              ? "Không tìm thấy sản phẩm"
              : "Không tải được chi tiết sản phẩm"
          }
          description={
            isNotFound
              ? `Mã ${maSP} không tồn tại hoặc đã bị xóa.`
              : error instanceof Error
                ? error.message
                : "Vui lòng thử lại."
          }
          action={
            <Link
              href="/products"
              className={cn(buttonVariants({ variant: "outline" }), "gap-2")}
            >
              <ArrowLeftIcon className="size-4" />
              Quay lại danh sách
            </Link>
          }
        />
      </PageContainer>
    )
  }

  if (!product) {
    return (
      <PageContainer>
        <EmptyState
          title="Không có dữ liệu sản phẩm"
          action={
            <Link href="/products" className={buttonVariants({ variant: "outline" })}>
              Quay lại danh sách
            </Link>
          }
        />
      </PageContainer>
    )
  }

  return (
    <PageContainer className="grid gap-6">
      <Link
        href="/products"
        className={cn(
          buttonVariants({ variant: "ghost", size: "sm" }),
          "w-fit gap-2"
        )}
      >
        <ArrowLeftIcon className="size-4" />
        Quay lại sản phẩm
      </Link>

      <section className="grid gap-6 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <div className="self-start overflow-hidden rounded-3xl border bg-muted">
          <img
            src={productImageUrl(product.hinhAnh)}
            alt={product.tenSP}
            className="aspect-[4/3] w-full object-cover"
          />
        </div>

        <div className="grid content-start gap-5 rounded-3xl border bg-background p-6 shadow-sm">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={product.trangThai ? "secondary" : "outline"}>
              {product.trangThai ? "Đang bán" : "Ngừng bán"}
            </Badge>
            <Badge variant="outline">{product.donViTinh}</Badge>
          </div>

          <div className="space-y-3">
            <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
              {product.tenSP}
            </h1>
            <p className="text-3xl font-semibold text-primary">
              {formatVnd(product.giaBan)}
            </p>
          </div>

          <ProductFacts product={product} />

          <TonKhoHeThong maSP={product.maSP} />

          <Button
            type="button"
            size="lg"
            className="w-full sm:w-fit"
            disabled={!product.trangThai || addToCart.isPending}
            onClick={handleAddToCart}
          >
            <ShoppingCartIcon className="size-4" />
            Thêm vào giỏ
          </Button>
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <ProductTextBlock title="Mô tả" content={product.moTa} />
        <ProductTextBlock
          title="Thông số kỹ thuật"
          content={product.thongSoKyThuat}
        />
      </section>
    </PageContainer>
  )
}
