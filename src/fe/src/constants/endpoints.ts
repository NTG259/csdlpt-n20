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
  ADMIN_ORDERS: "/api/admin/orders",
  ADMIN_ORDER_DETAIL: (maDonHang: string) =>
    `/api/admin/orders/${encodeURIComponent(maDonHang)}`,
  ADMIN_ORDER_STATUS: (maDonHang: string) =>
    `/api/admin/orders/${encodeURIComponent(maDonHang)}/trang-thai`,
  ADMIN_STATS_REVENUE: "/api/admin/thong-ke/doanh-thu",
  ADMIN_STATS_REVENUE_BY_MONTH: "/api/admin/thong-ke/doanh-thu-theo-thang",
  ADMIN_STATS_TOP_PRODUCTS: "/api/admin/thong-ke/san-pham-ban-chay",
  ADMIN_STATS_MULTI_WAREHOUSE: "/api/admin/thong-ke/don-hang-nhieu-kho",
} as const

export const REGIONAL_ENDPOINTS = {
  CART: "/api/cart",
  CART_ITEMS: "/api/cart/items",
  CART_ITEM: (maSP: string) => `/api/cart/items/${encodeURIComponent(maSP)}`,
  ORDERS: "/api/orders",
  ORDER_DETAIL: (id: string) => `/api/orders/${id}`,
  ORDER_CONFIRM_RECEIVED: (id: string) =>
    `/api/orders/${encodeURIComponent(id)}/xac-nhan-nhan-hang`,
  WAREHOUSE_ME: "/api/warehouse/me",
  WAREHOUSE_DASHBOARD: "/api/warehouse/dashboard",
  WAREHOUSE_EXPORTS: "/api/warehouse/phieu-xuat",
  WAREHOUSE_EXPORT_DETAIL: (id: string) =>
    `/api/warehouse/phieu-xuat/${encodeURIComponent(id)}`,
  WAREHOUSE_CONFIRM_INTERNAL_EXPORT: (id: string) =>
    `/api/warehouse/phieu-xuat/${encodeURIComponent(id)}/xac-nhan-noi-bo`,
  WAREHOUSE_CONFIRM_CUSTOMER_EXPORT: (id: string) =>
    `/api/warehouse/phieu-xuat/${encodeURIComponent(id)}/xac-nhan-giao-khach`,
  WAREHOUSE_IMPORTS: "/api/warehouse/phieu-nhap",
  WAREHOUSE_IMPORT_DETAIL: (id: string) =>
    `/api/warehouse/phieu-nhap/${encodeURIComponent(id)}`,
  WAREHOUSE_CONFIRM_IMPORT: (id: string) =>
    `/api/warehouse/phieu-nhap/${encodeURIComponent(id)}/xac-nhan`,
  WAREHOUSE_READY_TO_SHIP: "/api/warehouse/orders/ready-to-ship",
  WAREHOUSE_CREATE_CUSTOMER_EXPORT: (orderId: string) =>
    `/api/warehouse/orders/${encodeURIComponent(orderId)}/tao-phieu-giao-khach`,
  WAREHOUSE_STOCK: "/api/warehouse/ton-kho",
} as const
