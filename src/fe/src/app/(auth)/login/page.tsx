import Link from "next/link"

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { LoginForm } from "@/features/auth/login-form"

export default function LoginPage() {
  return (
    <main className="flex flex-1 items-center justify-center px-4 py-10">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Đăng nhập</CardTitle>
          <CardDescription>
            Dùng tài khoản khách hàng để tiếp tục mua hàng.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <LoginForm />
          <div className="mt-4 text-center text-xs text-muted-foreground">
            <Link href="/" className="underline">
              Quay về trang chủ
            </Link>
          </div>
        </CardContent>
      </Card>
    </main>
  )
}
