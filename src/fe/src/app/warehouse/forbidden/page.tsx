import Link from "next/link"

import { buttonVariants } from "@/components/ui/button"

export default function WarehouseForbiddenPage() {
  return (
    <div className="grid min-h-[60vh] place-items-center">
      <div className="max-w-md rounded-lg border bg-white p-6 text-center shadow-sm">
        <p className="text-sm font-semibold uppercase text-cyan-700">Warehouse</p>
        <h1 className="mt-2 font-heading text-2xl font-semibold">
          Không có quyền truy cập kho
        </h1>
        <p className="mt-3 text-sm text-muted-foreground">
          Tài khoản hiện tại không phải nhân viên kho hoặc quản trị viên.
        </p>
        <Link href="/" className={buttonVariants({ className: "mt-5" })}>
          Về trang chủ
        </Link>
      </div>
    </div>
  )
}
