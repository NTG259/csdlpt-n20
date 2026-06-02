"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2Icon, PackageCheckIcon } from "lucide-react"
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
import { useAuth } from "@/features/auth/auth-context"
import { formatVnd } from "@/lib/format"
import type { GioHang } from "@/types/cart"

import { createOrderSchema, type CreateOrderInput } from "./order-schema"
import { useCreateOrder } from "./use-create-order"

interface CheckoutDialogProps {
  cart: GioHang
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function CheckoutDialog({
  cart,
  open,
  onOpenChange,
}: CheckoutDialogProps) {
  const { user } = useAuth()
  const createOrder = useCreateOrder()
  const form = useForm<CreateOrderInput>({
    resolver: zodResolver(createOrderSchema),
    defaultValues: {
      hoTenNguoiNhan: user?.hoTen ?? "",
      soDienThoaiNhan: "",
      diaChiGiao: "",
      phuongThucTT: "COD",
      ghiChu: "",
    },
  })

  useEffect(() => {
    if (!open) {
      return
    }

    form.reset({
      hoTenNguoiNhan: user?.hoTen ?? "",
      soDienThoaiNhan: "",
      diaChiGiao: "",
      phuongThucTT: "COD",
      ghiChu: "",
    })
  }, [form, open, user?.hoTen])

  function onSubmit(input: CreateOrderInput) {
    createOrder.mutate(
      {
        ...input,
        ghiChu: input.ghiChu || undefined,
      },
      {
        onSuccess: () => {
          onOpenChange(false)
        },
      }
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Thong tin nhan hang</DialogTitle>
          <DialogDescription>
            Don hang se duoc tao tu cac san pham con hang trong gio. Thanh toan COD.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                control={form.control}
                name="hoTenNguoiNhan"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Ho ten nguoi nhan</FormLabel>
                    <FormControl>
                      <Input autoComplete="name" placeholder="Nguyen Van A" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="soDienThoaiNhan"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>So dien thoai</FormLabel>
                    <FormControl>
                      <Input
                        autoComplete="tel"
                        inputMode="tel"
                        placeholder="0901234567"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="diaChiGiao"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Dia chi giao hang</FormLabel>
                  <FormControl>
                    <Input
                      autoComplete="street-address"
                      placeholder="So nha, duong, phuong/xa, quan/huyen"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="ghiChu"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Ghi chu</FormLabel>
                  <FormControl>
                    <textarea
                      className="min-h-24 w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-sm outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                      placeholder="Thoi gian giao, ghi chu cho nhan vien giao hang"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="rounded-lg border bg-muted/40 px-3 py-2 text-sm">
              <div className="flex items-center justify-between gap-4">
                <span className="text-muted-foreground">So luong dat</span>
                <span className="font-medium">{cart.tongSoLuong}</span>
              </div>
              <div className="mt-1 flex items-center justify-between gap-4">
                <span className="text-muted-foreground">Tong tien</span>
                <span className="font-semibold">{formatVnd(cart.tongTien)}</span>
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                disabled={createOrder.isPending}
                onClick={() => onOpenChange(false)}
              >
                Huy
              </Button>
              <Button type="submit" disabled={createOrder.isPending}>
                {createOrder.isPending ? (
                  <Loader2Icon className="size-4 animate-spin" />
                ) : (
                  <PackageCheckIcon className="size-4" />
                )}
                Tao don hang
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
