import Link from "next/link"
import { MenuIcon } from "lucide-react"

import { RequireAdmin } from "@/features/admin/require-admin"
import { AdminSidebar } from "@/features/admin/admin-sidebar"
import { buttonVariants } from "@/components/ui/button"

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireAdmin>
      <div className="min-h-screen bg-[linear-gradient(135deg,#f8fafc_0%,#ecfdf5_45%,#fefce8_100%)] text-slate-950">
        <div className="grid min-h-screen lg:grid-cols-[280px_1fr]">
          <AdminSidebar />
          <div className="min-w-0">
            <header className="sticky top-0 z-30 flex items-center justify-between border-b bg-white/80 px-4 py-3 backdrop-blur lg:hidden">
              <Link href="/admin" className="font-heading text-lg font-semibold">
                Admin
              </Link>
              <Link
                href="/admin/products"
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
    </RequireAdmin>
  )
}
