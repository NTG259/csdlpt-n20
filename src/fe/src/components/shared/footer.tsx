"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { MailIcon, MapPinIcon, PhoneIcon } from "lucide-react"

const quickLinks = [
  { href: "/", label: "Trang chủ" },
  { href: "/products", label: "Sản phẩm" },
  { href: "/cart", label: "Giỏ hàng" },
]

export function Footer() {
  const pathname = usePathname()

  if (pathname.startsWith("/admin") || pathname.startsWith("/warehouse")) {
    return null
  }

  return (
    <footer className="border-t bg-slate-950 text-slate-200">
      <div className="mx-auto grid max-w-6xl gap-8 px-4 py-8 sm:px-6 md:grid-cols-[1.4fr_1fr_1fr]">
        <div className="space-y-3">
          <Link href="/" className="inline-flex text-lg font-semibold text-white">
            SiteMain Tech Shop
          </Link>
          <p className="max-w-md text-sm leading-6 text-slate-400">
            Mua sắm thiết bị công nghệ theo khu vực Bắc - Nam với thông tin tồn
            kho rõ ràng và quy trình đặt hàng nhanh gọn.
          </p>
        </div>

        <div className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-white">
            Liên kết
          </h2>
          <nav className="flex flex-col gap-2 text-sm text-slate-400">
            {quickLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="transition hover:text-white"
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>

        <div className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wide text-white">
            Hỗ trợ
          </h2>
          <div className="space-y-2 text-sm text-slate-400">
            <p className="flex items-center gap-2">
              <PhoneIcon className="size-4" />
              1900 0000
            </p>
            <p className="flex items-center gap-2">
              <MailIcon className="size-4" />
              support@sitemain.local
            </p>
            <p className="flex items-center gap-2">
              <MapPinIcon className="size-4" />
              Bắc - Nam, Việt Nam
            </p>
          </div>
        </div>
      </div>

      <div className="border-t border-white/10 px-4 py-4 text-center text-xs text-slate-500">
        © {new Date().getFullYear()} SiteMain Tech Shop. All rights reserved.
      </div>
    </footer>
  )
}
