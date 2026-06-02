import { Suspense } from "react"

import { Loading } from "@/components/shared/loading"
import { ProductListPage } from "@/features/products/product-list-page"

export default function ProductsPage() {
  return (
    <Suspense fallback={<Loading label="Đang tải trang sản phẩm" />}>
      <ProductListPage />
    </Suspense>
  )
}
