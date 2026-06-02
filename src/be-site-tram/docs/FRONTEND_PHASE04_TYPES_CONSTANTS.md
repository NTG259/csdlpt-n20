# FRONTEND PHASE 4 — Types & Constants (chi tiết)

> **Phạm vi:** Khai báo toàn bộ type TypeScript khớp 1–1 với DTO backend + các hằng số (khu vực, endpoint, query key).
> **Mục tiêu phase:** Sau phase này, mọi tầng phía trên (fetch wrapper, hooks, UI) đều tham chiếu type & hằng số tập trung, không khai báo rải rác, không hard-code chuỗi.
> **Nguồn sự thật:** code backend tại `sitemain/src/main/java/csdlpt/sitemain/` (DTO `dto/response`, `dto/request`, `common/`, `domain/enums`).

---

## 0. Tiền đề (đã có ở Phase 1–3)

- Project `sitemain-fe/` chạy được; thư mục `src/types`, `src/constants` đã tạo (Phase 3).
- `.env.local` đã có 3 base URL (`MAIN`/`NORTH`/`SOUTH`).
- Đã cài Zod (Phase 2) — dùng ở Phase 9, nhưng type ở đây là interface thuần.

> ⚠️ **Quan trọng — mã khu vực:** DB lưu `maKhuVuc = "Bac"` và `"Nam"`. Mọi type/hằng số dưới đây phản ánh đúng giá trị này (KHÔNG phải `KV01/KV02`).

---

## Task 4.1 — Type response chung (`ApiResponse`, `ErrorResponse`, `PageResponse`)

- **Mục tiêu:** Bọc đúng cấu trúc response thống nhất mà **mọi** endpoint BE trả về.
- **Đối chiếu BE:**
  - `common/ApiResponse.java` → `{ success, message, data, timestamp }`, field `null` bị lược bỏ (`@JsonInclude(NON_NULL)`).
  - `common/ErrorResponse.java` → `{ success:false, message, errorCode, details[], timestamp }`.
  - `common/PageResponse.java` → `{ items, page, size, totalElements, totalPages, last }`.
- **File tạo:** `src/types/api.ts`
  ```ts
  // Khớp common/ApiResponse.java — data luôn có khi success=true
  export interface ApiResponse<T> {
    success: true;
    message: string;
    data: T;
    timestamp: string;
  }

  // Khớp common/ErrorCodes.java (8 mã)
  export type ApiErrorCode =
    | "VALIDATION_ERROR"
    | "INVALID_CREDENTIALS"
    | "ACCESS_DENIED"
    | "RESOURCE_NOT_FOUND"
    | "DUPLICATE_EMAIL"
    | "DUPLICATE_PHONE"
    | "INVALID_REGION"
    | "INTERNAL_ERROR";

  // Khớp common/ErrorResponse.java
  export interface ApiErrorResponse {
    success: false;
    message: string;       // luôn an toàn để hiển thị (đã Việt hoá)
    errorCode: ApiErrorCode;
    details: string[];     // chỉ đầy đủ khi errorCode = VALIDATION_ERROR
    timestamp: string;
  }

  // Khớp common/PageResponse.java
  export interface PageResponse<T> {
    items: T[];
    page: number;          // 0-based
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
  }
  ```
- **Kết quả mong muốn:** Import được 4 type; `ApiErrorCode` gợi ý đúng 8 mã lỗi.
- **Kiểm chứng:** Tạo biến thử `const e: ApiErrorCode = "DUPLICATE_EMAIL"` không báo lỗi; gõ sai mã → TS báo đỏ.

---

## Task 4.2 — Type domain (auth, sản phẩm, dữ liệu tham chiếu)

- **Mục tiêu:** Type khớp đúng từng DTO response của Site Main.
- **Đối chiếu BE (chính xác theo record Java):**

| Type FE | DTO BE | Field |
|---|---|---|
| `AuthResponse` | `dto/response/AuthResponse.java` | `token, tokenType, expiresIn(number), userId, hoTen, email, maKhuVuc, vaiTro` |
| `CheckAvailability` | `CheckAvailabilityResponse.java` | `available: boolean` |
| `ProductListItem` | `ProductListItemResponse.java` | `maSP, tenSP, giaBan(number), donViTinh, hinhAnh, trangThai, tenDanhMuc, tenThuongHieu` |
| `ProductDetail` | `ProductDetailResponse.java` | + `ngayTao, maDanhMuc, maThuongHieu, moTa?, thongSoKyThuat?` |
| `Category` | `CategoryResponse.java` | `maDanhMuc, tenDanhMuc, maDanhMucCha?, moTa?, trangThai` |
| `Brand` | `BrandResponse.java` | `maThuongHieu, tenThuongHieu, trangThai` |
| `Region` | `RegionResponse.java` | `maKhuVuc, tenKhuVuc` |

- **File tạo:** `src/types/domain.ts`
  ```ts
  // vaiTro: khớp enum domain/enums/VaiTro.java
  export type VaiTro = "ADMIN" | "WAREHOUSE_STAFF" | "USER";

  // dto/response/AuthResponse.java — trả về cho cả register (201) và login (200)
  export interface AuthResponse {
    token: string;
    tokenType: string;     // "Bearer"
    expiresIn: number;     // ms, mặc định 86400000 (24h)
    userId: string;
    hoTen: string;
    email: string;
    maKhuVuc: string;      // "Bac" | "Nam" (giá trị thực trong DB)
    vaiTro: VaiTro;
  }

  export interface CheckAvailability {
    available: boolean;    // true = chưa ai dùng, có thể đăng ký
  }

  export interface ProductListItem {
    maSP: string;
    tenSP: string;
    giaBan: number;        // BigDecimal phía BE → number phía FE
    donViTinh: string;
    hinhAnh: string;
    trangThai: boolean;
    tenDanhMuc: string;
    tenThuongHieu: string;
  }

  export interface ProductDetail {
    maSP: string;
    tenSP: string;
    giaBan: number;
    donViTinh: string;
    hinhAnh: string;
    trangThai: boolean;
    ngayTao: string;       // LocalDateTime ISO
    maDanhMuc: string;
    tenDanhMuc: string;
    maThuongHieu: string;
    tenThuongHieu: string;
    moTa?: string;         // có thể vắng mặt (NON_NULL)
    thongSoKyThuat?: string;
  }

  export interface Category {
    maDanhMuc: string;
    tenDanhMuc: string;
    maDanhMucCha?: string; // null/vắng nếu là gốc
    moTa?: string;
    trangThai: boolean;
  }

  export interface Brand {
    maThuongHieu: string;
    tenThuongHieu: string;
    trangThai: boolean;
  }

  export interface Region {
    maKhuVuc: string;      // "Bac" | "Nam"
    tenKhuVuc: string;
  }
  ```
- **Lưu ý quan trọng:**
  - Field `null` bị BE lược bỏ → các field optional (`moTa?`, `maDanhMucCha?`...) phải để dấu `?`, KHÔNG được giả định luôn có.
  - `giaBan` là `BigDecimal` phía BE, FE nhận thành `number` (JSON) — format tiền tệ ở UI bằng `Intl.NumberFormat`.
- **Kết quả mong muốn:** Mọi màn hình import type từ `@/types/domain`, không định nghĩa lại.

---

## Task 4.3 — Type giỏ hàng / đơn hàng (dự kiến — Site Bắc/Nam)

- **Mục tiêu:** Có type tạm cho cart/order để code Phase 11–12 không kẹt.
- **⚠️ Lưu ý:** Site Main **KHÔNG** có cart/order. Các type này là **dự kiến**, đặt riêng để dễ thay khi có doc Site Bắc/Nam.
- **File tạo:** `src/types/regional.ts`
  ```ts
  // DỰ KIẾN — chỉnh lại khi có API Site Bắc/Nam thật.
  export interface CartItem {
    maSP: string;
    tenSP: string;
    hinhAnh: string;
    giaBan: number;
    soLuong: number;
    thanhTien: number;
  }
  export interface Cart {
    items: CartItem[];
    tongTien: number;
  }
  export interface OrderItem {
    maSP: string;
    tenSP: string;
    soLuong: number;
    donGia: number;
    thanhTien: number;
  }
  export interface Order {
    maDonHang: string;
    ngayDat: string;
    trangThai: string;
    tongTien: number;
    items: OrderItem[];
  }
  ```
- **Kết quả mong muốn:** Phase 11–12 import được type, có TODO rõ ràng để cập nhật sau.

---

## Task 4.4 — Hằng số khu vực (`regions.ts`)

- **Mục tiêu:** Ánh xạ `maKhuVuc` (BE) → vùng định tuyến API; đây là **trái tim của đồ án phân tán**.
- **Đối chiếu BE:** `maKhuVuc` thực tế = `"Bac"` / `"Nam"` (DB), độ dài ≤ 10 ký tự.
- **File tạo:** `src/constants/regions.ts`
  ```ts
  // Vùng định tuyến API nội bộ FE
  export type RegionCode = "BAC" | "NAM";

  // Mã khu vực lưu trong DB là "Bac" / "Nam".
  // Chuẩn hoá hoa-thường rồi map → RegionCode.
  // ⭐ SỬA DUY NHẤT Ở ĐÂY nếu BE đổi mã khu vực.
  export const KHU_VUC_TO_REGION: Record<string, RegionCode> = {
    bac: "BAC",
    nam: "NAM",
  };

  // Nhãn hiển thị cho UI (Header, badge...)
  export const REGION_LABEL: Record<RegionCode, string> = {
    BAC: "Miền Bắc",
    NAM: "Miền Nam",
  };
  ```
- **Kết quả mong muốn:** `KHU_VUC_TO_REGION["bac"] === "BAC"`. Logic suy ra vùng (Phase 7) chỉ tham chiếu file này.

---

## Task 4.5 — Hằng số endpoint (`endpoints.ts`)

- **Mục tiêu:** Gom mọi path API về 1 nơi; tách rõ **MAIN** (Site Main) và **REGIONAL** (Bắc/Nam).
- **Đối chiếu BE:** `@RequestMapping` của các controller (`/api/auth`, `/api/products`, `/api/categories`, `/api/brands`, `/api/regions`).
- **File tạo:** `src/constants/endpoints.ts`
  ```ts
  // === SITE MAIN (:8080) ===
  export const MAIN_ENDPOINTS = {
    REGISTER: "/api/auth/register",
    LOGIN: "/api/auth/login",
    CHECK_EMAIL: "/api/auth/check-email", // ?email=
    CHECK_PHONE: "/api/auth/check-phone", // ?phone=
    PRODUCTS: "/api/products",
    PRODUCT_DETAIL: (maSP: string) => `/api/products/${maSP}`,
    CATEGORIES: "/api/categories",
    BRANDS: "/api/brands",
    REGIONS: "/api/regions",
  } as const;

  // === SITE BẮC/NAM (:8081 / :8082) — DỰ KIẾN ===
  export const REGIONAL_ENDPOINTS = {
    CART: "/api/cart",
    CART_ITEM: (maSP: string) => `/api/cart/items/${maSP}`,
    ORDERS: "/api/orders",
    ORDER_DETAIL: (id: string) => `/api/orders/${id}`,
  } as const;
  ```
- **Kết quả mong muốn:** Đổi path chỉ sửa 1 file; phân biệt rõ endpoint nào gọi site nào.

---

## Task 4.6 — Query keys (`query-keys.ts`)

- **Mục tiêu:** Chuẩn hoá key cho TanStack Query để cache/invalidate nhất quán.
- **File tạo:** `src/constants/query-keys.ts`
  ```ts
  export const QK = {
    products: (params?: unknown) => ["products", params ?? {}] as const,
    product: (maSP: string) => ["product", maSP] as const,
    categories: () => ["categories"] as const,
    brands: () => ["brands"] as const,
    regions: () => ["regions"] as const,
    cart: (region: string) => ["cart", region] as const,
    orders: (region: string) => ["orders", region] as const,
  };
  ```
- **Kết quả mong muốn:** Invalidate `QK.cart(region)` sau khi thêm/xoá item; không trùng key giữa các vùng.

---

## Checklist hoàn thành Phase 4

- [x] `src/types/api.ts` — `ApiResponse`, `ApiErrorResponse`, `PageResponse`, `ApiErrorCode`.
- [x] `src/types/domain.ts` — `AuthResponse`, `ProductListItem`, `ProductDetail`, `Category`, `Brand`, `Region`, `VaiTro`, `CheckAvailability`.
- [x] `src/types/regional.ts` — `Cart`, `Order` (dự kiến).
- [x] `src/constants/regions.ts` — `RegionCode`, `KHU_VUC_TO_REGION`, `REGION_LABEL`.
- [x] `src/constants/endpoints.ts` — `MAIN_ENDPOINTS`, `REGIONAL_ENDPOINTS`.
- [x] `src/constants/query-keys.ts` — `QK`.
- [x] `bun run build` (hoặc `tsc --noEmit`) không lỗi type.

> **Tiếp theo:** [Phase 5 — Fetch wrapper](FRONTEND_PHASE05_FETCH_WRAPPER.md)
