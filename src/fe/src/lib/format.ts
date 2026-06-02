export function formatVnd(value: number | string) {
  const amount = typeof value === "string" ? Number(value) : value

  if (!Number.isFinite(amount)) {
    return "0 ₫"
  }

  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(amount)
}

export function productImageUrl(imagePath?: string | null) {
  if (!imagePath) {
    return "/placeholder-product.svg"
  }

  if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
    return imagePath
  }

  const baseUrl = process.env.NEXT_PUBLIC_MAIN_API_URL

  if (!baseUrl) {
    return imagePath
  }

  return new URL(imagePath, baseUrl).toString()
}
