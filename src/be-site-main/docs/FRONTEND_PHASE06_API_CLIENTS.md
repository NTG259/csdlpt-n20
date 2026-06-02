# FRONTEND PHASE 6 — API Clients (chi tiết)

> **Phạm vi:** Hai client mỏng trên `apiFetch`: `mainApiClient` (cố định Site Main) và `getRegionalApiClient(region)` (chọn Site Bắc/Nam theo vùng).
> **Mục tiêu phase:** Hook không cần biết base URL; chỉ chọn đúng client theo loại nghiệp vụ. Đây là nơi hiện thực **quy tắc định tuyến phân tán**.
> **Nguồn sự thật:** quy tắc định tuyến trong [FRONTEND_SETUP_TASKS.md](FRONTEND_SETUP_TASKS.md), `MAIN_ENDPOINTS`/`REGIONAL_ENDPOINTS` (Phase 4).

---

## 0. Tiền đề

- Phase 5 xong: `apiFetch`, `ApiError`.
- `.env.local`:
  ```env
  NEXT_PUBLIC_MAIN_API_URL=http://localhost:8080
  NEXT_PUBLIC_NORTH_API_URL=http://localhost:8081
  NEXT_PUBLIC_SOUTH_API_URL=http://localhost:8082
  ```
- `RegionCode` từ `@/constants/regions`.

---

## Task 6.1 — `mainApiClient` (Site Main, :8080)

- **Mục tiêu:** Client cố định Site Main cho **auth + sản phẩm + danh mục + thương hiệu + khu vực**.
- **Phụ trách (đối chiếu controller BE):** `AuthController`, `ProductController`, `CategoryController`, `BrandController`, `RegionController`.
- **File tạo:** `src/lib/main-api-client.ts`
  ```ts
  import { apiFetch } from "./api-fetch";

  const BASE = process.env.NEXT_PUBLIC_MAIN_API_URL!;

  type Query = Record<string, string | number | boolean | undefined>;

  export const mainApiClient = {
    get: <T>(path: string, opts?: { token?: string | null; query?: Query }) =>
      apiFetch<T>(path, { method: "GET", baseUrl: BASE, ...opts }),

    post: <T>(path: string, body: unknown, opts?: { token?: string | null }) =>
      apiFetch<T>(path, { method: "POST", baseUrl: BASE, body, ...opts }),
  };
  ```
- **Ví dụ dùng:**
  ```ts
  // Login (public)
  const auth = await mainApiClient.post<AuthResponse>(MAIN_ENDPOINTS.LOGIN, { email, matKhau });
  // Danh sách SP (public, có query)
  const page = await mainApiClient.get<PageResponse<ProductListItem>>(
    MAIN_ENDPOINTS.PRODUCTS, { query: { page: 0, size: 12, maDanhMuc } },
  );
  ```
- **Kết quả mong muốn:** Mọi nghiệp vụ Site Main đi qua đúng `:8080`.

---

## Task 6.2 — `getRegionalApiClient(region)` (Site Bắc :8081 / Nam :8082)

- **Mục tiêu:** Trả về client trỏ đúng base URL theo `RegionCode`; dùng cho **giỏ hàng + đơn hàng**.
- **File tạo:** `src/lib/regional-api-client.ts`
  ```ts
  import { apiFetch } from "./api-fetch";
  import type { RegionCode } from "@/constants/regions";

  const URL_BY_REGION: Record<RegionCode, string> = {
    BAC: process.env.NEXT_PUBLIC_NORTH_API_URL!,
    NAM: process.env.NEXT_PUBLIC_SOUTH_API_URL!,
  };

  export function getRegionalApiClient(region: RegionCode, token?: string | null) {
    const baseUrl = URL_BY_REGION[region];
    return {
      get:    <T>(path: string) => apiFetch<T>(path, { method: "GET",    baseUrl, token }),
      post:   <T>(path: string, body?: unknown) => apiFetch<T>(path, { method: "POST",   baseUrl, token, body }),
      patch:  <T>(path: string, body?: unknown) => apiFetch<T>(path, { method: "PATCH",  baseUrl, token, body }),
      delete: <T>(path: string) => apiFetch<T>(path, { method: "DELETE", baseUrl, token }),
    };
  }
  ```
- **Ví dụ dùng:**
  ```ts
  const client = getRegionalApiClient(region, token); // region = "BAC" | "NAM"
  const cart = await client.get<Cart>(REGIONAL_ENDPOINTS.CART);
  ```
- **Lưu ý:** Hầu hết endpoint vùng cần auth → luôn truyền `token`. Nếu `region` chưa xác định (`null`) thì **không** tạo client (xử lý ở hook Phase 11/12).
- **Kết quả mong muốn:** `getRegionalApiClient("BAC")` → `:8081`; `getRegionalApiClient("NAM")` → `:8082` (kiểm chứng ở tab Network).

---

## Task 6.3 — Bảng định tuyến (tham chiếu nhanh)

| Nghiệp vụ | Endpoint | Client | Auth |
|---|---|---|---|
| Đăng ký / đăng nhập | `MAIN_ENDPOINTS.REGISTER/LOGIN` | `mainApiClient` | ❌ |
| Check email / phone | `MAIN_ENDPOINTS.CHECK_*` | `mainApiClient` | ❌ |
| Sản phẩm (list/detail) | `MAIN_ENDPOINTS.PRODUCTS*` | `mainApiClient` | ❌ |
| Danh mục / thương hiệu / khu vực | `MAIN_ENDPOINTS.*` | `mainApiClient` | ❌ |
| Giỏ hàng | `REGIONAL_ENDPOINTS.CART*` | `getRegionalApiClient(region)` | ✅ |
| Đơn hàng | `REGIONAL_ENDPOINTS.ORDERS*` | `getRegionalApiClient(region)` | ✅ |

> **Nguyên tắc vàng:** dữ liệu dùng chung → `mainApiClient`; dữ liệu theo vùng người dùng → `getRegionalApiClient`.

---

## Checklist hoàn thành Phase 6

- [x] `src/lib/main-api-client.ts` — `mainApiClient.get/post`.
- [x] `src/lib/regional-api-client.ts` — `getRegionalApiClient` map BAC→8081, NAM→8082.
- [ ] Gọi thử `mainApiClient.get(MAIN_ENDPOINTS.REGIONS)` → trả mảng `[{maKhuVuc:"Bac"...},{maKhuVuc:"Nam"...}]`.
- [x] Xác nhận env URL đọc được ở client (`NEXT_PUBLIC_*`).

> Ghi chú verify: tại thời điểm chạy Phase 6, backend `:8080` khởi động được nhưng DB SQL Server `localhost:1433` từ chối kết nối, nên `mainApiClient.get(MAIN_ENDPOINTS.REGIONS)` nhận `500 INTERNAL_ERROR` từ BE thay vì dữ liệu thật.

> **Tiếp theo:** [Phase 7 — Auth state](FRONTEND_PHASE07_AUTH_STATE.md)
