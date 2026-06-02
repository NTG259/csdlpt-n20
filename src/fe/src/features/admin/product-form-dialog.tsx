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
import { Skeleton } from "@/components/ui/skeleton"
import { useProduct } from "@/features/products/use-products"
import { useBrands, useCategories } from "@/features/products/use-reference"
import type { ProductUpsert } from "@/types/admin"

import { applyServerErrors } from "./form-errors"
import { productSchema, type ProductFormInput } from "./product-schema"
import { useCreateProduct, useUpdateProduct } from "./use-admin-products"

interface ProductFormDialogProps {
  open: boolean
  maSP?: string | null
  onOpenChange: (open: boolean) => void
}

const emptyProduct: ProductFormInput = {
  maSP: "",
  tenSP: "",
  maDanhMuc: "",
  maThuongHieu: "",
  giaBan: 0,
  donViTinh: "Cái",
  hinhAnh: "",
  trangThai: true,
  moTa: "",
  thongSoKyThuat: "",
}

function emptyToUndefined(value?: string) {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}

export function ProductFormDialog({
  open,
  maSP,
  onOpenChange,
}: ProductFormDialogProps) {
  const isEdit = Boolean(maSP)
  const { data: product, isLoading: isLoadingProduct } = useProduct(maSP ?? "")
  const { data: categories = [], isLoading: isLoadingCategories } =
    useCategories()
  const { data: brands = [], isLoading: isLoadingBrands } = useBrands()
  const createProduct = useCreateProduct()
  const updateProduct = useUpdateProduct()
  const isPending = createProduct.isPending || updateProduct.isPending

  const form = useForm<ProductFormInput>({
    resolver: zodResolver(productSchema),
    defaultValues: emptyProduct,
  })

  useEffect(() => {
    if (!open) {
      return
    }

    if (!isEdit) {
      form.reset(emptyProduct)
      return
    }

    if (product) {
      form.reset({
        maSP: product.maSP,
        tenSP: product.tenSP,
        maDanhMuc: product.maDanhMuc,
        maThuongHieu: product.maThuongHieu,
        giaBan: product.giaBan,
        donViTinh: product.donViTinh,
        hinhAnh: product.hinhAnh ?? "",
        trangThai: product.trangThai,
        moTa: product.moTa ?? "",
        thongSoKyThuat: product.thongSoKyThuat ?? "",
      })
    }
  }, [form, isEdit, open, product])

  async function onSubmit(input: ProductFormInput) {
    const payload: ProductUpsert = {
      ...input,
      maSP: input.maSP.trim(),
      tenSP: input.tenSP.trim(),
      maDanhMuc: input.maDanhMuc.trim(),
      maThuongHieu: input.maThuongHieu.trim(),
      donViTinh: input.donViTinh.trim(),
      hinhAnh: emptyToUndefined(input.hinhAnh),
      moTa: emptyToUndefined(input.moTa),
      thongSoKyThuat: emptyToUndefined(input.thongSoKyThuat),
    }

    try {
      if (isEdit && maSP) {
        await updateProduct.mutateAsync({ maSP, data: payload })
      } else {
        await createProduct.mutateAsync(payload)
      }
      onOpenChange(false)
    } catch (error) {
      applyServerErrors(form, error)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Cập nhật sản phẩm" : "Thêm sản phẩm"}
          </DialogTitle>
          <DialogDescription>
            Mã sản phẩm là khóa chính và phải trùng với path khi cập nhật.
          </DialogDescription>
        </DialogHeader>

        {isEdit && isLoadingProduct ? (
          <div className="grid gap-3">
            <Skeleton className="h-10" />
            <Skeleton className="h-10" />
            <Skeleton className="h-24" />
          </div>
        ) : (
          <Form {...form}>
            <form
              id="product-form"
              onSubmit={form.handleSubmit(onSubmit)}
              className="grid max-h-[70vh] gap-4 overflow-y-auto pr-1"
            >
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField
                  control={form.control}
                  name="maSP"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Mã sản phẩm</FormLabel>
                      <FormControl>
                        <Input {...field} disabled={isEdit} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="tenSP"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tên sản phẩm</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="giaBan"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Giá bán</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min={0}
                          step={1000}
                          value={field.value}
                          onBlur={field.onBlur}
                          name={field.name}
                          ref={field.ref}
                          onChange={(event) =>
                            field.onChange(Number(event.target.value))
                          }
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="donViTinh"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Đơn vị tính</FormLabel>
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="maDanhMuc"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Danh mục</FormLabel>
                      <Select
                        value={field.value}
                        onValueChange={(value) => field.onChange(String(value))}
                        disabled={isLoadingCategories}
                      >
                        <FormControl>
                          <SelectTrigger className="h-10 w-full">
                            <SelectValue placeholder="Chọn danh mục" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {categories.map((category) => (
                            <SelectItem
                              key={category.maDanhMuc}
                              value={category.maDanhMuc}
                            >
                              {category.tenDanhMuc}
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
                  name="maThuongHieu"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Thương hiệu</FormLabel>
                      <Select
                        value={field.value}
                        onValueChange={(value) => field.onChange(String(value))}
                        disabled={isLoadingBrands}
                      >
                        <FormControl>
                          <SelectTrigger className="h-10 w-full">
                            <SelectValue placeholder="Chọn thương hiệu" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {brands.map((brand) => (
                            <SelectItem
                              key={brand.maThuongHieu}
                              value={brand.maThuongHieu}
                            >
                              {brand.tenThuongHieu}
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
                  name="hinhAnh"
                  render={({ field }) => (
                    <FormItem className="sm:col-span-2">
                      <FormLabel>URL hình ảnh</FormLabel>
                      <FormControl>
                        <Input {...field} value={field.value ?? ""} />
                      </FormControl>
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
                          <SelectItem value="true">Đang bán</SelectItem>
                          <SelectItem value="false">Ngừng bán</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

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
              <FormField
                control={form.control}
                name="thongSoKyThuat"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Thông số kỹ thuật</FormLabel>
                    <FormControl>
                      <textarea
                        className="min-h-24 rounded-lg border border-input bg-transparent px-3 py-2 font-mono text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                        placeholder='{"cpu":"i7"}'
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
        )}

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            disabled={isPending}
            onClick={() => onOpenChange(false)}
          >
            Hủy
          </Button>
          <Button form="product-form" type="submit" disabled={isPending}>
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
