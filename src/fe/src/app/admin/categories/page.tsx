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
import { CategoryFormDialog } from "@/features/admin/category-form-dialog"
import { ConfirmDelete } from "@/features/admin/confirm-delete"
import { useDeleteCategory } from "@/features/admin/use-admin-categories"
import { Pagination } from "@/features/products/pagination"
import { useCategories } from "@/features/products/use-reference"
import type { Category } from "@/types/domain"

const PAGE_SIZE = 10

export default function AdminCategoriesPage() {
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(0)
  const [formOpen, setFormOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(null)
  const { data: categories = [], isLoading, isError, refetch } = useCategories()
  const deleteCategory = useDeleteCategory()

  const categoryNameById = useMemo(() => {
    return new Map(categories.map((category) => [category.maDanhMuc, category.tenDanhMuc]))
  }, [categories])

  const visibleCategories = useMemo(() => {
    const term = search.trim().toLowerCase()
    if (!term) {
      return categories
    }

    return categories.filter((category) =>
      [category.maDanhMuc, category.tenDanhMuc, category.moTa ?? ""]
        .join(" ")
        .toLowerCase()
        .includes(term)
    )
  }, [categories, search])

  const totalPages = Math.max(1, Math.ceil(visibleCategories.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages - 1)
  const pagedCategories = useMemo(() => {
    const start = safePage * PAGE_SIZE
    return visibleCategories.slice(start, start + PAGE_SIZE)
  }, [safePage, visibleCategories])

  function openCreate() {
    setEditingCategory(null)
    setFormOpen(true)
  }

  function openEdit(category: Category) {
    setEditingCategory(category)
    setFormOpen(true)
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-emerald-700">
            Catalog
          </p>
          <h1 className="font-heading text-3xl font-semibold">Danh mục</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Quản lý danh mục và quan hệ danh mục cha.
          </p>
        </div>
        <Button type="button" onClick={openCreate}>
          <PlusIcon className="size-4" />
          Thêm danh mục
        </Button>
      </div>

      <Card className="rounded-3xl bg-white/90">
        <CardHeader>
          <CardTitle>Danh sách danh mục</CardTitle>
          <CardDescription>
            GET public hiện trả danh mục đang active. Delete sẽ soft-delete.
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
              placeholder="Tìm mã, tên, mô tả..."
              className="h-10 pl-9"
            />
          </div>

          {isLoading ? (
            <Skeleton className="h-80 rounded-2xl" />
          ) : isError ? (
            <div className="grid gap-3 py-10 text-center">
              <p>Không tải được danh mục.</p>
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
                  <TableHead>Tên danh mục</TableHead>
                  <TableHead>Danh mục cha</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibleCategories.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="py-10 text-center">
                      Chưa có danh mục phù hợp.
                    </TableCell>
                  </TableRow>
                ) : (
                  pagedCategories.map((category) => (
                    <TableRow key={category.maDanhMuc}>
                      <TableCell className="font-medium">
                        {category.maDanhMuc}
                      </TableCell>
                      <TableCell>
                        <div>{category.tenDanhMuc}</div>
                        {category.moTa && (
                          <div className="max-w-md truncate text-xs text-muted-foreground">
                            {category.moTa}
                          </div>
                        )}
                      </TableCell>
                      <TableCell>
                        {category.maDanhMucCha
                          ? categoryNameById.get(category.maDanhMucCha) ??
                            category.maDanhMucCha
                          : "Gốc"}
                      </TableCell>
                      <TableCell>
                        <StatusBadge active={category.trangThai} />
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => openEdit(category)}
                          >
                            <EditIcon className="size-4" />
                            Sửa
                          </Button>
                          <Button
                            type="button"
                            variant="destructive"
                            size="sm"
                            onClick={() => setDeletingCategory(category)}
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

      <CategoryFormDialog
        open={formOpen}
        category={editingCategory}
        onOpenChange={setFormOpen}
      />
      <ConfirmDelete
        open={Boolean(deletingCategory)}
        title="Xoá danh mục"
        description={
          deletingCategory
            ? `Backend sẽ soft-delete ${deletingCategory.maDanhMuc}.`
            : ""
        }
        isPending={deleteCategory.isPending}
        onOpenChange={(open) => !open && setDeletingCategory(null)}
        onConfirm={() => {
          if (!deletingCategory) {
            return
          }
          deleteCategory.mutate(deletingCategory.maDanhMuc, {
            onSuccess: () => setDeletingCategory(null),
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
