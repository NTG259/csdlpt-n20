import { ProductDetailView } from "@/features/products/product-detail"

interface ProductDetailPageProps {
  params: Promise<{
    maSP: string
  }>
}

export default async function ProductDetailPage({
  params,
}: ProductDetailPageProps) {
  const { maSP } = await params

  return <ProductDetailView maSP={decodeURIComponent(maSP)} />
}
