"use client"

import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { toast } from "sonner"

import { EmptyState } from "@/components/shared/empty-state"
import { PageContainer } from "@/components/shared/page-container"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/features/auth/auth-context"
import { useAddToCart } from "@/features/cart/use-cart"
import type { ProductListItem } from "@/types/domain"

import {
  DEFAULT_PRODUCT_SIZE,
  DEFAULT_PRODUCT_SORT,
  ProductFilters,
  type ProductFilterState,
  type ProductStatusFilter,
} from "./product-filters"
import { ProductGrid } from "./product-grid"
import { Pagination } from "./pagination"
import { useProducts, type ProductQuery } from "./use-products"

const allowedSizes = new Set([12, 24, 48])
const allowedSorts = new Set([
  DEFAULT_PRODUCT_SORT,
  "tenSP,asc",
  "tenSP,desc",
  "giaBan,asc",
  "giaBan,desc",
])

function parsePositiveInt(value: string | null, fallback: number) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback
}

function parseSize(value: string | null) {
  const parsed = Number(value)
  return allowedSizes.has(parsed) ? parsed : DEFAULT_PRODUCT_SIZE
}

function parseStatus(value: string | null): ProductStatusFilter {
  if (value === "true" || value === "false") {
    return value
  }

  return "all"
}

function parseSort(value: string | null) {
  return value && allowedSorts.has(value) ? value : DEFAULT_PRODUCT_SORT
}

function normalizeSearch(value: string) {
  return value
    .toLocaleLowerCase("vi-VN")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .trim()
}

function productMatchesSearch(product: ProductListItem, term: string) {
  const haystack = [
    product.maSP,
    product.tenSP,
    product.tenDanhMuc,
    product.tenThuongHieu,
    product.donViTinh,
  ]
    .map(normalizeSearch)
    .join(" ")

  return haystack.includes(term)
}

function setParam(params: URLSearchParams, key: string, value?: string) {
  if (value) {
    params.set(key, value)
  } else {
    params.delete(key)
  }
}

export function ProductListPage() {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const { isAuthenticated, region } = useAuth()
  const addToCart = useAddToCart()

  const page = parsePositiveInt(searchParams.get("page"), 0)
  const filters: ProductFilterState = {
    q: searchParams.get("q")?.trim() ?? "",
    maDanhMuc: searchParams.get("maDanhMuc")?.trim() ?? "",
    maThuongHieu: searchParams.get("maThuongHieu")?.trim() ?? "",
    trangThai: parseStatus(searchParams.get("trangThai")),
    sort: parseSort(searchParams.get("sort")),
    size: parseSize(searchParams.get("size")),
  }

  const productQuery: ProductQuery = {
    page,
    size: filters.size,
    sort: filters.sort,
    ...(filters.maDanhMuc ? { maDanhMuc: filters.maDanhMuc } : {}),
    ...(filters.maThuongHieu ? { maThuongHieu: filters.maThuongHieu } : {}),
    ...(filters.trangThai !== "all"
      ? { trangThai: filters.trangThai === "true" }
      : {}),
  }

  const { data, isLoading, isError, error, isFetching } = useProducts(productQuery)
  const searchTerm = normalizeSearch(filters.q)
  const products = data?.items ?? []
  const visibleProducts = searchTerm
    ? products.filter((product) => productMatchesSearch(product, searchTerm))
    : products

  function pushFilters(next: ProductFilterState) {
    const params = new URLSearchParams()

    setParam(params, "q", next.q.trim())
    setParam(params, "maDanhMuc", next.maDanhMuc)
    setParam(params, "maThuongHieu", next.maThuongHieu)
    setParam(
      params,
      "trangThai",
      next.trangThai === "all" ? undefined : next.trangThai
    )
    setParam(
      params,
      "sort",
      next.sort === DEFAULT_PRODUCT_SORT ? undefined : next.sort
    )
    setParam(
      params,
      "size",
      next.size === DEFAULT_PRODUCT_SIZE ? undefined : String(next.size)
    )

    const query = params.toString()
    router.push(query ? `${pathname}?${query}` : pathname)
  }

  function pushPage(nextPage: number) {
    const params = new URLSearchParams(searchParams.toString())

    if (nextPage > 0) {
      params.set("page", String(nextPage))
    } else {
      params.delete("page")
    }

    const query = params.toString()
    router.push(query ? `${pathname}?${query}` : pathname)
  }

  function resetFilters() {
    router.push(pathname)
  }

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

  const resultLabel = data
    ? `Hiển thị ${visibleProducts.length}/${products.length} sản phẩm trên trang ${data.page + 1}. Tổng server: ${data.totalElements}.`
    : undefined

  return (
    <PageContainer className="grid gap-6">
      <section className="grid gap-3 rounded-3xl border bg-[radial-gradient(circle_at_top_left,_rgba(15,23,42,0.10),_transparent_35%),linear-gradient(135deg,_rgba(255,255,255,0.96),_rgba(241,245,249,0.96))] p-6 shadow-sm">
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">Catalog</Badge>
        </div>
        <div className="max-w-3xl space-y-2">
          <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
            Sản phẩm
          </h1>
          <p className="text-sm leading-6 text-muted-foreground sm:text-base">
            Tìm kiếm, lọc danh mục, thương hiệu, trạng thái kinh doanh.
          </p>
        </div>
      </section>

      <ProductFilters
        key={filters.q}
        filters={filters}
        resultLabel={resultLabel}
        onChange={pushFilters}
        onReset={resetFilters}
      />

      {isError ? (
        <EmptyState
          title="Không tải được danh sách sản phẩm"
          description={error instanceof Error ? error.message : "Vui lòng thử lại."}
          action={
            <Button type="button" variant="outline" onClick={() => router.refresh()}>
              Tải lại
            </Button>
          }
        />
      ) : (
        <>
          <ProductGrid
            products={visibleProducts}
            isLoading={isLoading}
            searchTerm={filters.q}
            onAddToCart={handleAddToCart}
          />

          {data ? (
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              last={data.last}
              onPageChange={pushPage}
            />
          ) : null}

          {isFetching && !isLoading ? (
            <p className="text-center text-xs text-muted-foreground">
              Đang cập nhật dữ liệu...
            </p>
          ) : null}
        </>
      )}
    </PageContainer>
  )
}
