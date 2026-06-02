"use client"

/* eslint-disable @next/next/no-img-element */

import { useMemo, useState } from "react"
import { EditIcon, PlusIcon, SearchIcon, Trash2Icon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { ConfirmDelete } from "@/features/admin/confirm-delete"
import { ProductFormDialog } from "@/features/admin/product-form-dialog"
import {
  useAdminProducts,
  useDeleteProduct,
  type AdminProductQuery,
} from "@/features/admin/use-admin-products"
import { Pagination } from "@/features/products/pagination"
import { useBrands, useCategories } from "@/features/products/use-reference"
import { formatVnd, productImageUrl } from "@/lib/format"
import type { ProductListItem } from "@/types/domain"

const defaultQuery: AdminProductQuery = {
  page: 0,
  size: 20,
  sort: "ngayTao,desc",
}

function selectValue(value?: string) {
  return value && value.trim() ? value : "all"
}

export default function AdminProductsPage() {
  const [query, setQuery] = useState<AdminProductQuery>(defaultQuery)
  const [search, setSearch] = useState("")
  const [formOpen, setFormOpen] = useState(false)
  const [editingMaSP, setEditingMaSP] = useState<string | null>(null)
  const [deletingProduct, setDeletingProduct] = useState<ProductListItem | null>(
    null
  )
  const { data, isLoading, isError, refetch } = useAdminProducts(query)
  const { data: categories = [] } = useCategories()
  const { data: brands = [] } = useBrands()
  const deleteProduct = useDeleteProduct()

  const visibleProducts = useMemo(() => {
    const term = search.trim().toLowerCase()
    const items = data?.items ?? []
    if (!term) {
      return items
    }

    return items.filter((product) =>
      [
        product.maSP,
        product.tenSP,
        product.tenDanhMuc,
        product.tenThuongHieu,
      ]
        .join(" ")
        .toLowerCase()
        .includes(term)
    )
  }, [data?.items, search])

  function updateQuery(next: Partial<AdminProductQuery>) {
    setQuery((current) => ({ ...current, ...next, page: next.page ?? 0 }))
  }

  function openCreate() {
    setEditingMaSP(null)
    setFormOpen(true)
  }

  function openEdit(maSP: string) {
    setEditingMaSP(maSP)
    setFormOpen(true)
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-700">
            Catalog
          </p>
          <h1 className="font-heading text-3xl font-semibold">Sản phẩm</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Quản lý sản phẩm Site Main, bao gồm hàng đang bán và ngừng bán.
          </p>
        </div>
        <Button type="button" onClick={openCreate}>
          <PlusIcon className="size-4" />
          Thêm sản phẩm
        </Button>
      </div>

      <Card className="rounded-3xl bg-white/90">
        <CardHeader>
          <CardTitle>Bộ lọc</CardTitle>
          <CardDescription>
            Backend hỗ trợ lọc danh mục, thương hiệu, trạng thái, phân trang và
            sắp xếp. Ô tìm kiếm lọc nhanh trên trang hiện tại.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
            <div className="relative">
              <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Tìm trong trang..."
                className="h-10 pl-9"
              />
            </div>
            <Select
              value={selectValue(query.maDanhMuc)}
              onValueChange={(value) =>
                updateQuery({
                  maDanhMuc: value === "all" ? undefined : String(value),
                })
              }
            >
              <SelectTrigger className="h-10 w-full">
                <SelectValue placeholder="Danh mục" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả danh mục</SelectItem>
                {categories.map((category) => (
                  <SelectItem key={category.maDanhMuc} value={category.maDanhMuc}>
                    {category.tenDanhMuc}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={selectValue(query.maThuongHieu)}
              onValueChange={(value) =>
                updateQuery({
                  maThuongHieu: value === "all" ? undefined : String(value),
                })
              }
            >
              <SelectTrigger className="h-10 w-full">
                <SelectValue placeholder="Thương hiệu" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả thương hiệu</SelectItem>
                {brands.map((brand) => (
                  <SelectItem key={brand.maThuongHieu} value={brand.maThuongHieu}>
                    {brand.tenThuongHieu}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={
                query.trangThai === undefined ? "all" : String(query.trangThai)
              }
              onValueChange={(value) =>
                updateQuery({
                  trangThai:
                    value === "all" ? undefined : String(value) === "true",
                })
              }
            >
              <SelectTrigger className="h-10 w-full">
                <SelectValue placeholder="Trạng thái" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Tất cả trạng thái</SelectItem>
                <SelectItem value="true">Đang bán</SelectItem>
                <SelectItem value="false">Ngừng bán</SelectItem>
              </SelectContent>
            </Select>
            <Select
              value={query.sort}
              onValueChange={(value) => updateQuery({ sort: String(value) })}
            >
              <SelectTrigger className="h-10 w-full">
                <SelectValue placeholder="Sắp xếp" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ngayTao,desc">Mới nhất</SelectItem>
                <SelectItem value="tenSP,asc">Tên A-Z</SelectItem>
                <SelectItem value="giaBan,asc">Giá thấp đến cao</SelectItem>
                <SelectItem value="giaBan,desc">Giá cao đến thấp</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card className="rounded-3xl bg-white/90">
        <CardHeader>
          <CardTitle>Danh sách sản phẩm</CardTitle>
          <CardDescription>
            Tổng {data?.totalElements ?? 0} sản phẩm theo bộ lọc hiện tại.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-80 rounded-2xl" />
          ) : isError ? (
            <div className="grid gap-3 py-10 text-center">
              <p>Không tải được sản phẩm.</p>
              <Button type="button" onClick={() => refetch()}>
                Thử lại
              </Button>
            </div>
          ) : (
            <div className="grid gap-4">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Sản phẩm</TableHead>
                    <TableHead>Danh mục</TableHead>
                    <TableHead>Thương hiệu</TableHead>
                    <TableHead>Giá</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {visibleProducts.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="py-10 text-center">
                        Chưa có sản phẩm phù hợp.
                      </TableCell>
                    </TableRow>
                  ) : (
                    visibleProducts.map((product) => (
                      <TableRow key={product.maSP}>
                        <TableCell>
                          <div className="flex items-center gap-3">
                            <img
                              src={productImageUrl(product.hinhAnh)}
                              alt={product.tenSP}
                              className="size-12 rounded-xl object-cover"
                            />
                            <div>
                              <div className="font-medium">{product.tenSP}</div>
                              <div className="text-xs text-muted-foreground">
                                {product.maSP}
                              </div>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>{product.tenDanhMuc}</TableCell>
                        <TableCell>{product.tenThuongHieu}</TableCell>
                        <TableCell>{formatVnd(product.giaBan)}</TableCell>
                        <TableCell>
                          <StatusBadge active={product.trangThai} />
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-2">
                            <Button
                              type="button"
                              variant="outline"
                              size="sm"
                              onClick={() => openEdit(product.maSP)}
                            >
                              <EditIcon className="size-4" />
                              Sửa
                            </Button>
                            <Button
                              type="button"
                              variant="destructive"
                              size="sm"
                              onClick={() => setDeletingProduct(product)}
                            >
                              <Trash2Icon className="size-4" />
                              Xoá
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
              {data && (
                <Pagination
                  page={data.page}
                  totalPages={data.totalPages}
                  last={data.last}
                  onPageChange={(page) =>
                    setQuery((current) => ({ ...current, page }))
                  }
                />
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <ProductFormDialog
        open={formOpen}
        maSP={editingMaSP}
        onOpenChange={setFormOpen}
      />
      <ConfirmDelete
        open={Boolean(deletingProduct)}
        title="Xoá sản phẩm"
        description={
          deletingProduct
            ? `Backend sẽ soft-delete ${deletingProduct.maSP} bằng cách đặt trạng thái ngừng bán.`
            : ""
        }
        isPending={deleteProduct.isPending}
        onOpenChange={(open) => !open && setDeletingProduct(null)}
        onConfirm={() => {
          if (!deletingProduct) {
            return
          }
          deleteProduct.mutate(deletingProduct.maSP, {
            onSuccess: () => setDeletingProduct(null),
          })
        }}
      />
    </div>
  )
}

function StatusBadge({ active }: { active: boolean }) {
  return active ? (
    <Badge className="bg-emerald-100 text-emerald-800">Đang bán</Badge>
  ) : (
    <Badge variant="outline">Ngừng bán</Badge>
  )
}
