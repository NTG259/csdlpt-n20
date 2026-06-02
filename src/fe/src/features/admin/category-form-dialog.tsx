"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2Icon, SaveIcon } from "lucide-react"
import { useForm } from "react-hook-form"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { useCategories } from "@/features/products/use-reference"
import type { CategoryUpsert } from "@/types/admin"
import type { Category } from "@/types/domain"

import { categorySchema, type CategoryFormInput } from "./category-schema"
import { applyServerErrors } from "./form-errors"
import { useCreateCategory, useUpdateCategory } from "./use-admin-categories"

interface CategoryFormDialogProps {
  open: boolean
  category?: Category | null
  onOpenChange: (open: boolean) => void
}

const emptyCategory: CategoryFormInput = {
  maDanhMuc: "",
  tenDanhMuc: "",
  maDanhMucCha: "",
  moTa: "",
  trangThai: true,
}

function emptyToUndefined(value?: string) {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}

export function CategoryFormDialog({
  open,
  category,
  onOpenChange,
}: CategoryFormDialogProps) {
  const isEdit = Boolean(category)
  const { data: categories = [] } = useCategories()
  const createCategory = useCreateCategory()
  const updateCategory = useUpdateCategory()
  const isPending = createCategory.isPending || updateCategory.isPending

  const form = useForm<CategoryFormInput>({
    resolver: zodResolver(categorySchema),
    defaultValues: emptyCategory,
  })

  useEffect(() => {
    if (!open) {
      return
    }

    form.reset(
      category
        ? {
            maDanhMuc: category.maDanhMuc,
            tenDanhMuc: category.tenDanhMuc,
            maDanhMucCha: category.maDanhMucCha ?? "",
            moTa: category.moTa ?? "",
            trangThai: category.trangThai,
          }
        : emptyCategory
    )
  }, [category, form, open])

  async function onSubmit(input: CategoryFormInput) {
    const payload: CategoryUpsert = {
      maDanhMuc: input.maDanhMuc.trim(),
      tenDanhMuc: input.tenDanhMuc.trim(),
      maDanhMucCha: emptyToUndefined(input.maDanhMucCha),
      moTa: emptyToUndefined(input.moTa),
      trangThai: input.trangThai,
    }

    try {
      if (category) {
        await updateCategory.mutateAsync({
          maDanhMuc: category.maDanhMuc,
          data: payload,
        })
      } else {
        await createCategory.mutateAsync(payload)
      }
      onOpenChange(false)
    } catch (error) {
      applyServerErrors(form, error)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Cập nhật danh mục" : "Thêm danh mục"}
          </DialogTitle>
          <DialogDescription>
            Danh mục cha là tùy chọn và không được trùng chính danh mục.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form
            id="category-form"
            onSubmit={form.handleSubmit(onSubmit)}
            className="grid gap-4"
          >
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="maDanhMuc"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Mã danh mục</FormLabel>
                    <FormControl>
                      <Input {...field} disabled={isEdit} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="tenDanhMuc"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tên danh mục</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name="maDanhMucCha"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Danh mục cha</FormLabel>
                  <Select
                    value={field.value || "none"}
                    onValueChange={(value) =>
                      field.onChange(value === "none" ? "" : String(value))
                    }
                  >
                    <FormControl>
                      <SelectTrigger className="h-10 w-full">
                        <SelectValue placeholder="Không có danh mục cha" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="none">Không có danh mục cha</SelectItem>
                      {categories
                        .filter((item) => item.maDanhMuc !== category?.maDanhMuc)
                        .map((item) => (
                          <SelectItem key={item.maDanhMuc} value={item.maDanhMuc}>
                            {item.tenDanhMuc}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="trangThai"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Trạng thái</FormLabel>
                  <Select
                    value={field.value ? "true" : "false"}
                    onValueChange={(value) => field.onChange(value === "true")}
                  >
                    <FormControl>
                      <SelectTrigger className="h-10 w-full">
                        <SelectValue placeholder="Trạng thái" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="true">Đang dùng</SelectItem>
                      <SelectItem value="false">Tạm ẩn</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="moTa"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Mô tả</FormLabel>
                  <FormControl>
                    <textarea
                      className="min-h-24 rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                      {...field}
                      value={field.value ?? ""}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </form>
        </Form>
        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={isPending}
            onClick={() => onOpenChange(false)}
          >
            Hủy
          </Button>
          <Button form="category-form" type="submit" disabled={isPending}>
            {isPending ? (
              <Loader2Icon className="size-4 animate-spin" />
            ) : (
              <SaveIcon className="size-4" />
            )}
            Lưu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
