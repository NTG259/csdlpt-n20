"use client"

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
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { BrandFormDialog } from "@/features/admin/brand-form-dialog"
import { ConfirmDelete } from "@/features/admin/confirm-delete"
import { useDeleteBrand } from "@/features/admin/use-admin-brands"
import { Pagination } from "@/features/products/pagination"
import { useBrands } from "@/features/products/use-reference"
import type { Brand } from "@/types/domain"

const PAGE_SIZE = 10

export default function AdminBrandsPage() {
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingBrand, setEditingBrand] = useState<Brand | null>(null)
  const [deletingBrand, setDeletingBrand] = useState<Brand | null>(null)
  const { data: brands = [], isLoading, isError, refetch } = useBrands()
  const deleteBrand = useDeleteBrand()

  const visibleBrands = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) {
      return brands
    }

    return brands.filter((brand) =>
      [brand.maThuongHieu, brand.tenThuongHieu]
        .join(" ")
        .toLowerCase()
        .includes(term)
    )
  }, [brands, search])

  const totalPages = Math.max(1, Math.ceil(visibleBrands.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages - 1)
  const pagedBrands = useMemo(() => {
    const start = safePage * PAGE_SIZE
    return visibleBrands.slice(start, start + PAGE_SIZE)
  }, [safePage, visibleBrands])

  function openCreate() {
    setEditingBrand(null)
    setFormOpen(true)
  }

  function openEdit(brand: Brand) {
    setEditingBrand(brand)
    setFormOpen(true)
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-700">
            Catalog
          </p>
          <h1 className="font-heading text-3xl font-semibold">Thương hiệu</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Quản lý thương hiệu, tên thương hiệu phải duy nhất.
          </p>
        </div>
        <Button type="button" onClick={openCreate}>
          <PlusIcon className="size-4" />
          Thêm thương hiệu
        </Button>
      </div>

      <Card className="rounded-3xl bg-white/90">
        <CardHeader>
          <CardTitle>Danh sách thương hiệu</CardTitle>
          <CardDescription>
            Delete sẽ soft-delete bằng cách đặt `trangThai=false`.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4">
          <div className="relative max-w-md">
            <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setPage(0)
              }}
              placeholder="Tìm mã hoặc tên..."
              className="h-10 pl-9"
            />
          </div>

          {isLoading ? (
            <Skeleton className="h-80 rounded-2xl" />
          ) : isError ? (
            <div className="grid gap-3 py-10 text-center">
              <p>Không tải được thương hiệu.</p>
              <Button type="button" onClick={() => refetch()}>
                Thử lại
              </Button>
            </div>
          ) : (
            <div className="grid gap-4">
              <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã</TableHead>
                  <TableHead>Tên thương hiệu</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibleBrands.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} className="py-10 text-center">
                      Chưa có thương hiệu phù hợp.
                    </TableCell>
                  </TableRow>
                ) : (
                  pagedBrands.map((brand) => (
                    <TableRow key={brand.maThuongHieu}>
                      <TableCell className="font-medium">
                        {brand.maThuongHieu}
                      </TableCell>
                      <TableCell>{brand.tenThuongHieu}</TableCell>
                      <TableCell>
                        <StatusBadge active={brand.trangThai} />
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => openEdit(brand)}
                          >
                            <EditIcon className="size-4" />
                            Sửa
                          </Button>
                          <Button
                            type="button"
                            variant="destructive"
                            size="sm"
                            onClick={() => setDeletingBrand(brand)}
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
              <Pagination
                page={safePage}
                totalPages={totalPages}
                last={safePage + 1 >= totalPages}
                onPageChange={setPage}
              />
            </div>
          )}
        </CardContent>
      </Card>

      <BrandFormDialog
        open={formOpen}
        brand={editingBrand}
        onOpenChange={setFormOpen}
      />
      <ConfirmDelete
        open={Boolean(deletingBrand)}
        title="Xoá thương hiệu"
        description={
          deletingBrand
            ? `Backend sẽ soft-delete ${deletingBrand.maThuongHieu}.`
            : ""
        }
        isPending={deleteBrand.isPending}
        onOpenChange={(open) => !open && setDeletingBrand(null)}
        onConfirm={() => {
          if (!deletingBrand) {
            return
          }
          deleteBrand.mutate(deletingBrand.maThuongHieu, {
            onSuccess: () => setDeletingBrand(null),
          })
        }}
      />
    </div>
  )
}

function StatusBadge({ active }: { active: boolean }) {
  return active ? (
    <Badge className="bg-emerald-100 text-emerald-800">Đang dùng</Badge>
  ) : (
    <Badge variant="outline">Tạm ẩn</Badge>
  )
}
