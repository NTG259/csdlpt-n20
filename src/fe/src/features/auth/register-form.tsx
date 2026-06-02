"use client"

import Link from "next/link"
import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2Icon, UserPlusIcon } from "lucide-react"
import { useForm } from "react-hook-form"

import { Button } from "@/components/ui/button"
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

import { registerSchema, type RegisterInput } from "./schemas"
import { useRegister } from "./use-auth-mutations"
import { useRegions } from "./use-regions"

export function RegisterForm() {
  const registerMutation = useRegister()
  const regionsQuery = useRegions()
  const form = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      hoTen: "",
      email: "",
      soDienThoai: "",
      matKhau: "",
      maKhuVuc: "",
      diaChi: "",
      ngaySinh: "",
      gioiTinh: "",
      cccd: "",
    },
  })

  function onSubmit(input: RegisterInput) {
    registerMutation.mutate(input)
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="hoTen"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Họ tên</FormLabel>
                <FormControl>
                  <Input autoComplete="name" placeholder="Nguyen Van A" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Email</FormLabel>
                <FormControl>
                  <Input
                    type="email"
                    autoComplete="email"
                    placeholder="email@example.com"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <FormField
            control={form.control}
            name="soDienThoai"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Số điện thoại</FormLabel>
                <FormControl>
                  <Input
                    autoComplete="tel"
                    placeholder="0123456789"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="matKhau"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Mật khẩu</FormLabel>
                <FormControl>
                  <Input
                    type="password"
                    autoComplete="new-password"
                    placeholder="Từ 6 đến 72 ký tự"
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
          name="maKhuVuc"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Khu vực</FormLabel>
              <Select
                value={field.value || null}
                onValueChange={(value) => field.onChange(value ?? "")}
                disabled={regionsQuery.isLoading || registerMutation.isPending}
              >
                <FormControl>
                  <SelectTrigger className="w-full">
                    <SelectValue>
                      {field.value
                        ? regionsQuery.data?.find(
                            (region) => region.maKhuVuc === field.value
                          )?.tenKhuVuc
                        : "Chọn khu vực"}
                    </SelectValue>
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {regionsQuery.data?.map((region) => (
                    <SelectItem key={region.maKhuVuc} value={region.maKhuVuc}>
                      {region.tenKhuVuc}
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
          name="diaChi"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Địa chỉ</FormLabel>
              <FormControl>
                <Input autoComplete="street-address" placeholder="Địa chỉ nhận hàng" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <div className="grid gap-4 sm:grid-cols-3">
          <FormField
            control={form.control}
            name="ngaySinh"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Ngày sinh</FormLabel>
                <FormControl>
                  <Input type="date" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="gioiTinh"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Giới tính</FormLabel>
                <FormControl>
                  <Input placeholder="Nam/Nữ" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="cccd"
            render={({ field }) => (
              <FormItem>
                <FormLabel>CCCD</FormLabel>
                <FormControl>
                  <Input inputMode="numeric" placeholder="12 chữ số" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {regionsQuery.isError ? (
          <p className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            Không tải được danh sách khu vực. Vui lòng thử lại sau.
          </p>
        ) : null}

        <Button
          type="submit"
          size="lg"
          disabled={registerMutation.isPending || regionsQuery.isLoading}
        >
          {registerMutation.isPending ? (
            <Loader2Icon className="size-4 animate-spin" />
          ) : (
            <UserPlusIcon className="size-4" />
          )}
          Đăng ký
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          Đã có tài khoản?{" "}
          <Link className="font-medium text-foreground underline" href="/login">
            Đăng nhập
          </Link>
        </p>
      </form>
    </Form>
  )
}
