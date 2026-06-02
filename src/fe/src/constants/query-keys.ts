export const QK = {
  products: (params?: unknown) => ["products", params ?? {}] as const,
  product: (maSP: string) => ["product", maSP] as const,
  tonKho: (maSP: string) => ["ton-kho", maSP] as const,
  categories: () => ["categories"] as const,
  brands: () => ["brands"] as const,
  regions: () => ["regions"] as const,
  cart: (region: string) => ["cart", region] as const,
  orders: (region: string) => ["orders", region] as const,
  adminProducts: (params?: unknown) =>
    ["admin", "products", params ?? {}] as const,
  adminStatsRevenue: (params?: unknown) =>
    ["admin", "stats", "revenue", params ?? {}] as const,
}
