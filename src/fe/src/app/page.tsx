import Link from "next/link"

import { Badge } from "@/components/ui/badge"
import { buttonVariants } from "@/components/ui/button"
import { PageContainer } from "@/components/shared/page-container"
import { HomeProducts } from "@/features/products/home-products"
import { cn } from "@/lib/utils"

export default function Home() {
  return (
    <PageContainer className="grid gap-10 py-10">
      <section className="relative overflow-hidden rounded-[2rem] border bg-[radial-gradient(circle_at_20%_20%,_rgba(20,184,166,0.24),_transparent_28%),radial-gradient(circle_at_90%_10%,_rgba(251,146,60,0.20),_transparent_24%),linear-gradient(135deg,_rgba(255,255,255,0.98),_rgba(248,250,252,0.92))] p-8 shadow-sm sm:p-10">
        <div className="relative z-10 grid max-w-3xl gap-6">
          <div className="flex flex-wrap gap-2">
            <Badge variant="secondary">Tech Shop</Badge>
          </div>

          <div className="grid gap-4">
            <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
              Mua sắm theo danh mục, thương hiệu và khu vực giao hàng.
            </h1>
          </div>

          <div className="flex flex-wrap gap-3">
            <Link
              href="/products"
              className={cn(buttonVariants({ size: "lg" }), "gap-2")}
            >
              Xem sản phẩm
            </Link>
            <Link
              href="/login"
              className={buttonVariants({ variant: "outline", size: "lg" })}
            >
              Đăng nhập
            </Link>
          </div>
        </div>
      </section>

      <HomeProducts />
    </PageContainer>
  )
}
