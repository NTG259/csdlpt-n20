import Link from "next/link"

import { buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export default function AdminForbiddenPage() {
  return (
    <div className="grid min-h-[70vh] place-items-center">
      <Card className="max-w-md rounded-3xl border-red-200 bg-white/90 text-center shadow-xl">
        <CardHeader>
          <CardTitle className="text-2xl">Không đủ quyền quản trị</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Tài khoản hiện tại không có vai trò ADMIN để truy cập khu vực này.
          </p>
          <Link href="/" className={buttonVariants()}>
            Quay về cửa hàng
          </Link>
        </CardContent>
      </Card>
    </div>
  )
}
