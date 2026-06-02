import Link from "next/link"
import { MenuIcon } from "lucide-react"

import { buttonVariants } from "@/components/ui/button"
import { RequireWarehouse } from "@/features/warehouse/require-warehouse"
import { WarehouseSidebar } from "@/features/warehouse/warehouse-sidebar"

export default function WarehouseLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <RequireWarehouse>
      <div className="min-h-screen bg-slate-100 text-slate-950">
        <div className="grid min-h-screen lg:grid-cols-[280px_1fr]">
          <WarehouseSidebar />
          <div className="min-w-0">
            <header className="sticky top-0 z-30 flex items-center justify-between border-b bg-white/90 px-4 py-3 backdrop-blur lg:hidden">
              <Link href="/warehouse" className="font-heading text-lg font-semibold">
                Quản lý kho
              </Link>
              <Link
                href="/warehouse/phieu-xuat"
                className={buttonVariants({ variant: "outline", size: "sm" })}
              >
                <MenuIcon className="size-4" />
                Menu
              </Link>
            </header>
            <main className="mx-auto w-full max-w-7xl p-4 sm:p-6 lg:p-8">
              {children}
            </main>
          </div>
        </div>
      </div>
    </RequireWarehouse>
  )
}
