export const MAIN_ENDPOINTS = {
  REGISTER: "/api/auth/register",
  LOGIN: "/api/auth/login",
  CHECK_EMAIL: "/api/auth/check-email",
  CHECK_PHONE: "/api/auth/check-phone",
  PRODUCTS: "/api/products",
  PRODUCT_DETAIL: (maSP: string) => `/api/products/${maSP}`,
  PRODUCT_CREATE: "/api/products",
  PRODUCT_UPDATE: (maSP: string) => `/api/products/${encodeURIComponent(maSP)}`,
  PRODUCT_DELETE: (maSP: string) => `/api/products/${encodeURIComponent(maSP)}`,
  PRODUCT_TON_KHO: (maSP: string) =>
    `/api/products/${encodeURIComponent(maSP)}/ton-kho`,
  CATEGORIES: "/api/categories",
  CATEGORY_ITEM: (maDanhMuc: string) =>
    `/api/categories/${encodeURIComponent(maDanhMuc)}`,
  BRANDS: "/api/brands",
  BRAND_ITEM: (maThuongHieu: string) =>
    `/api/brands/${encodeURIComponent(maThuongHieu)}`,
  REGIONS: "/api/regions",
  ADMIN_STATS_REVENUE: "/api/admin/thong-ke/doanh-thu",
} as const

export const REGIONAL_ENDPOINTS = {
  CART: "/api/cart",
  CART_ITEMS: "/api/cart/items",
  CART_ITEM: (maSP: string) => `/api/cart/items/${encodeURIComponent(maSP)}`,
  ORDERS: "/api/orders",
  ORDER_DETAIL: (id: string) => `/api/orders/${id}`,
} as const
