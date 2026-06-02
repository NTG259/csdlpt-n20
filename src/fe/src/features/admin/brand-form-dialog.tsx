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
import type { BrandUpsert } from "@/types/admin"
import type { Brand } from "@/types/domain"

import { brandSchema, type BrandFormInput } from "./brand-schema"
import { applyServerErrors } from "./form-errors"
import { useCreateBrand, useUpdateBrand } from "./use-admin-brands"

interface BrandFormDialogProps {
  open: boolean
  brand?: Brand | null
  onOpenChange: (open: boolean) => void
}

const emptyBrand: BrandFormInput = {
  maThuongHieu: "",
  tenThuongHieu: "",
  trangThai: true,
}

export function BrandFormDialog({
  open,
  brand,
  onOpenChange,
}: BrandFormDialogProps) {
  const isEdit = Boolean(brand)
  const createBrand = useCreateBrand()
  const updateBrand = useUpdateBrand()
  const isPending = createBrand.isPending || updateBrand.isPending

  const form = useForm<BrandFormInput>({
    resolver: zodResolver(brandSchema),
    defaultValues: emptyBrand,
  })

  useEffect(() => {
    if (!open) {
      return
    }

    form.reset(
      brand
        ? {
            maThuongHieu: brand.maThuongHieu,
            tenThuongHieu: brand.tenThuongHieu,
            trangThai: brand.trangThai,
          }
        : emptyBrand
    )
  }, [brand, form, open])

  async function onSubmit(input: BrandFormInput) {
    const payload: BrandUpsert = {
      maThuongHieu: input.maThuongHieu.trim(),
      tenThuongHieu: input.tenThuongHieu.trim(),
      trangThai: input.trangThai,
    }

    try {
      if (brand) {
        await updateBrand.mutateAsync({
          maThuongHieu: brand.maThuongHieu,
          data: payload,
        })
      } else {
        await createBrand.mutateAsync(payload)
      }
      onOpenChange(false)
    } catch (error) {
      applyServerErrors(form, error)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? "Cập nhật thương hiệu" : "Thêm thương hiệu"}
          </DialogTitle>
          <DialogDescription>
            Tên thương hiệu phải duy nhất trong hệ thống.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form
            id="brand-form"
            onSubmit={form.handleSubmit(onSubmit)}
            className="grid gap-4"
          >
            <FormField
              control={form.control}
              name="maThuongHieu"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Mã thương hiệu</FormLabel>
                  <FormControl>
                    <Input {...field} disabled={isEdit} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="tenThuongHieu"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tên thương hiệu</FormLabel>
                  <FormControl>
                    <Input {...field} />
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
                      <SelectItem value="true">Đang dùng</SelectItem>
                      <SelectItem value="false">Tạm ẩn</SelectItem>
                    </SelectContent>
                  </Select>
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
          <Button form="brand-form" type="submit" disabled={isPending}>
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
