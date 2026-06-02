export const QK = {
  products: (params?: unknown) => ["products", params ?? {}] as const,
  product: (maSP: string) => ["product", maSP] as const,
  tonKho: (maSP: string) => ["ton-kho", maSP] as const,
  categories: () => ["categories"] as const,
  brands: () => ["brands"] as const,
  regions: () => ["regions"] as const,
  cart: (region: string) => ["cart", region] as const,
  orders: (region: string) => ["orders", region] as const,
  adminOrders: (params?: unknown) =>
    ["admin", "orders", params ?? {}] as const,
  adminOrderDetail: (id: string, site: string) =>
    ["admin", "orders", id, site] as const,
  adminProducts: (params?: unknown) =>
    ["admin", "products", params ?? {}] as const,
  adminStatsRevenue: (params?: unknown) =>
    ["admin", "stats", "revenue", params ?? {}] as const,
  adminStatsRevenueByMonth: () =>
    ["admin", "stats", "revenue-by-month"] as const,
  adminStatsTopProducts: () => ["admin", "stats", "top-products"] as const,
  adminStatsMultiWarehouse: () =>
    ["admin", "stats", "multi-warehouse"] as const,
  warehouse: (region: string) => ["warehouse", region] as const,
  warehouseContext: (region: string) => ["warehouse", region, "context"] as const,
  warehouseDashboard: (region: string, params?: unknown) =>
    ["warehouse", region, "dashboard", params ?? {}] as const,
  warehouseExports: (region: string, params?: unknown) =>
    ["warehouse", region, "exports", params ?? {}] as const,
  warehouseExportDetail: (region: string, id?: string | null) =>
    ["warehouse", region, "exports", "detail", id ?? null] as const,
  warehouseImports: (region: string, params?: unknown) =>
    ["warehouse", region, "imports", params ?? {}] as const,
  warehouseImportDetail: (region: string, id?: string | null) =>
    ["warehouse", region, "imports", "detail", id ?? null] as const,
  warehouseReadyOrders: (region: string, params?: unknown) =>
    ["warehouse", region, "ready-orders", params ?? {}] as const,
  warehouseStock: (region: string, params?: unknown) =>
    ["warehouse", region, "stock", params ?? {}] as const,
}
