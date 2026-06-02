"use client"

import Link from "next/link"
import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2Icon, LogInIcon } from "lucide-react"
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

import { loginSchema, type LoginInput } from "./schemas"
import { useLogin } from "./use-auth-mutations"

export function LoginForm() {
  const loginMutation = useLogin()
  const form = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      matKhau: "",
    },
  })

  function onSubmit(input: LoginInput) {
    loginMutation.mutate(input)
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4">
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

        <FormField
          control={form.control}
          name="matKhau"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Mật khẩu</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  autoComplete="current-password"
                  placeholder="Nhập mật khẩu"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" size="lg" disabled={loginMutation.isPending}>
          {loginMutation.isPending ? (
            <Loader2Icon className="size-4 animate-spin" />
          ) : (
            <LogInIcon className="size-4" />
          )}
          Đăng nhập
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          Chưa có tài khoản?{" "}
          <Link className="font-medium text-foreground underline" href="/register">
            Đăng ký
          </Link>
        </p>
      </form>
    </Form>
  )
}
