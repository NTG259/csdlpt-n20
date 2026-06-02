import Link from "next/link"

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { RegisterForm } from "@/features/auth/register-form"

export default function RegisterPage() {
  return (
    <main className="flex flex-1 items-center justify-center px-4 py-10">
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle>Đăng ký tài khoản</CardTitle>
          <CardDescription>
            Chọn đúng khu vực để giỏ hàng và đơn hàng đi về site tương ứng.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <RegisterForm />
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
