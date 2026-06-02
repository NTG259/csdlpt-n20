# FRONTEND — KẾ HOẠCH CHIA TASK SETUP (chi tiết)

> **Phạm vi:** Dựng frontend cho hệ thống **bán hàng trực tuyến đa kho** (đồ án CSDL phân tán) từ con số 0 đến demo được luồng: xem sản phẩm → đăng ký/đăng nhập → giỏ hàng → đơn hàng.
> **Stack:** Bun + Next.js (App Router) + TypeScript + Tailwind CSS + shadcn/ui + TanStack Query + Zod + React Hook Form.
> **Triết lý:** Ưu tiên **đơn giản, dễ demo**. KHÔNG dùng Redux, Zustand, NextAuth, GraphQL, tRPC hay kiến trúc nhiều tầng. State auth lưu bằng `localStorage` + React Context nhẹ.

---

## Cập nhật trạng thái (2026-06-02)

Project FE hiện nằm ở thư mục **`sitemain-fe/`** (cạnh `sitemain/`) và đã hoàn thành **Phase 1 → 11** ở mức code/build. Nghiệm thu dữ liệu thật vẫn cần các backend Main/Bắc/Nam kết nối được database.

| Hạng mục | Trạng thái |
|---|---|
| Next.js 16 + React 19 + TypeScript + Tailwind v4 | ✅ Đã có sẵn và chạy được |
| Dọn boilerplate `src/app/page.tsx`, `globals.css`, cập nhật metadata/layout | ✅ Xong |
| shadcn/ui + `components.json` + `src/lib/utils.ts` | ✅ Xong |
| Bộ component nền cho demo: `button`, `input`, `label`, `card`, `form`, `sonner`, `dialog`, `dropdown-menu`, `skeleton`, `badge`, `separator`, `table`, `select` | ✅ Xong |
| TanStack Query, Zod, React Hook Form, resolvers, devtools | ✅ Xong |
| Cây thư mục `app / components / features / lib / types / constants` | ✅ Đã dựng |
| `.env.example` commit lên git | ✅ Xong |
| `.env.local` local để chạy dev | ✅ Đã tạo local |

---

## 0. Bối cảnh kiến trúc (đọc trước khi code)

Hệ thống có **3 backend riêng biệt** (mô phỏng CSDL phân tán theo khu vực):

| API | Base URL (env) | Trách nhiệm |
|---|---|---|
| **Site Main** | `NEXT_PUBLIC_MAIN_API_URL` (`:8080`) | Auth (register/login/check), **sản phẩm, danh mục, thương hiệu, khu vực** (dữ liệu dùng chung) |
| **Site Bắc** | `NEXT_PUBLIC_NORTH_API_URL` (`:8081`) | **Giỏ hàng, đơn hàng** cho user khu vực Bắc |
| **Site Nam** | `NEXT_PUBLIC_SOUTH_API_URL` (`:8082`) | **Giỏ hàng, đơn hàng** cho user khu vực Nam |

**Quy tắc định tuyến API (cốt lõi của đồ án):**

```
Sản phẩm / Danh mục / Thương hiệu / Khu vực   →  LUÔN gọi Site Main
Auth (đăng ký / đăng nhập / check)            →  LUÔN gọi Site Main
Giỏ hàng / Đơn hàng                           →  Gọi Site Bắc HOẶC Site Nam
                                                  theo  user.maKhuVuc  (Bac → Bắc, Nam → Nam)
```

> ✅ **Mã khu vực thực tế trong DB là `"Bac"` và `"Nam"`.** `AuthResponse.maKhuVuc` trả đúng 2 giá trị này. FE map `maKhuVuc → RegionCode "BAC"|"NAM"` (chuẩn hoá hoa-thường) tại `constants/regions.ts`; sửa 1 chỗ nếu BE đổi mã. Project FE nằm ở thư mục **`sitemain-fe/`**.

**Cấu trúc response chung (mọi API đều giống nhau):**
- Thành công: `{ success: true, message, data, timestamp }`
- Lỗi: `{ success: false, message, errorCode, details, timestamp }`
- Field `null` bị lược bỏ → FE không giả định field luôn tồn tại.

---

## Lộ trình & thứ tự triển khai

| # | Nhóm task | Mục tiêu nhóm | Trạng thái |
|---|---|---|---|
| 1 | Khởi tạo project | `bun dev` chạy được Next.js + Tailwind | ✅ Xong |
| 2 | Cài thư viện nền | shadcn/ui, TanStack Query, Zod, RHF sẵn sàng | ✅ Xong |
| 3 | Cấu trúc thư mục + env | Khung dự án rõ ràng, biến môi trường | ✅ Xong |
| 4 | Types & constants | Type chung khớp API, hằng số khu vực | ✅ Xong |
| 5 | Fetch wrapper | Hàm gọi HTTP dùng chung, bóc tách ApiResponse | ✅ Xong |
| 6 | API clients | `mainApiClient` + `regionalApiClient` | ✅ Xong |
| 7 | Auth state | Lưu token/user, context, chọn vùng theo user | ✅ Xong |
| 8 | Layout & Providers | QueryProvider, Header, Toaster, route layout | ✅ Xong |
| 9 | Feature: Auth | `/login`, `/register` | ✅ Code xong |
| 10 | Feature: Sản phẩm | `/products`, `/products/[maSP]`, trang `/` | ✅ Code xong |
| 11 | Feature: Giỏ hàng | `/cart` (gọi API theo vùng) | ✅ Code xong |
| 12 | Feature: Đơn hàng | `/orders` (gọi API theo vùng) | ⬜ Chưa làm |
| 13 | Hoàn thiện & demo | Route guard, README chạy demo | ⬜ Chưa làm |

---

## 📑 Doc chi tiết từng phase (4 → 13)

Phase 1–3 (khởi tạo, cài lib, cấu trúc) mô tả ngắn ngay trong file này. Từ **Phase 4 trở đi** mỗi phase có một file riêng, chi tiết theo code backend (DTO, validation, endpoint, error code):

| Phase | File chi tiết |
|---|---|
| 4 — Types & Constants | [FRONTEND_PHASE04_TYPES_CONSTANTS.md](FRONTEND_PHASE04_TYPES_CONSTANTS.md) |
| 5 — Fetch wrapper | [FRONTEND_PHASE05_FETCH_WRAPPER.md](FRONTEND_PHASE05_FETCH_WRAPPER.md) |
| 6 — API Clients | [FRONTEND_PHASE06_API_CLIENTS.md](FRONTEND_PHASE06_API_CLIENTS.md) |
| 7 — Auth state | [FRONTEND_PHASE07_AUTH_STATE.md](FRONTEND_PHASE07_AUTH_STATE.md) |
| 8 — Layout & Providers | [FRONTEND_PHASE08_LAYOUT_PROVIDERS.md](FRONTEND_PHASE08_LAYOUT_PROVIDERS.md) |
| 9 — Auth pages | [FRONTEND_PHASE09_AUTH_PAGES.md](FRONTEND_PHASE09_AUTH_PAGES.md) |
| 10 — Sản phẩm | [FRONTEND_PHASE10_PRODUCTS.md](FRONTEND_PHASE10_PRODUCTS.md) |
| 11 — Giỏ hàng | [FRONTEND_PHASE11_CART.md](FRONTEND_PHASE11_CART.md) |
| 12 — Đơn hàng | [FRONTEND_PHASE12_ORDERS.md](FRONTEND_PHASE12_ORDERS.md) |
| 13 — Hoàn thiện & demo | [FRONTEND_PHASE13_FINALIZE.md](FRONTEND_PHASE13_FINALIZE.md) |

> Các mục Phase 4–13 bên dưới trong file này là **bản tóm tắt**; xem file tương ứng để có code mẫu, đối chiếu DTO và checklist đầy đủ.

---

# PHASE 1 — Khởi tạo project ✅

## Task 1.1 — Tạo Next.js project bằng Bun ✅
- **Mục tiêu:** Có project Next.js App Router + TypeScript + Tailwind chạy được.
- **Trạng thái hiện tại:** Đã có sẵn project trong **`sitemain-fe/`** với Next.js App Router, TypeScript, ESLint, Tailwind CSS v4, alias `@/*`.
- **Không cần chạy lại** `bun create next-app`.
- **Kết quả hiện tại:** Có thể chạy tại `sitemain-fe/` bằng `bun dev`.

## Task 1.2 — Cấu hình port FE & dọn boilerplate ✅
- **Mục tiêu:** FE chạy port cố định, gỡ nội dung mẫu thừa.
- **File chỉnh:**
  - `sitemain-fe/package.json` → giữ script `"dev": "next dev"` (mặc định port 3000, không đụng 8080/8081/8082 của BE).
  - `sitemain-fe/src/app/page.tsx` → đã xoá boilerplate create-next-app, thay bằng placeholder nền cho dự án.
  - `sitemain-fe/src/app/globals.css` → đã dọn phần CSS mẫu thừa, giữ cấu hình Tailwind v4 + shadcn.
  - `sitemain-fe/src/app/layout.tsx` → đã cập nhật metadata và font variables cho khớp cấu hình mới.
- **Kết quả hiện tại:** Trang chủ sạch, build/lint ổn.

---

# PHASE 2 — Cài thư viện nền ✅

## Task 2.1 — Khởi tạo shadcn/ui ✅
- **Mục tiêu:** Có hệ component UI sẵn dùng (button, input, card, form...).
- **Lệnh:**
  ```bash
  bunx --bun shadcn@latest init -y -d
  ```
  Sau đó thêm các component cần cho demo:
  ```bash
  bunx --bun shadcn@latest add button input label card form sonner dialog dropdown-menu skeleton badge separator table select
  ```
- **File/thư mục hiện có:** `sitemain-fe/components.json`, `sitemain-fe/src/components/ui/*`, `sitemain-fe/src/lib/utils.ts`.
- **Ghi chú:** Component `form` đã được bổ sung thủ công tại `src/components/ui/form.tsx` để dùng với React Hook Form.
- **Kết quả hiện tại:** Import được các UI primitives và lint pass.

## Task 2.2 — Cài TanStack Query, Zod, React Hook Form ✅
- **Mục tiêu:** Đủ thư viện data-fetching + validate + form.
- **Lệnh:**
  ```bash
  bun add @tanstack/react-query
  bun add zod react-hook-form @hookform/resolvers
  bun add -d @tanstack/react-query-devtools
  ```
- **File:** `sitemain-fe/package.json` (đã cập nhật dependencies).
- **Kết quả hiện tại:** Các package đã được cài xong và lockfile đã cập nhật.

---

# PHASE 3 — Cấu trúc thư mục & biến môi trường ✅

## Task 3.1 — Dựng cây thư mục chuẩn ✅
- **Mục tiêu:** Phân tách rõ ràng theo vai trò, dễ tìm file.
- **Thư mục đã tạo (trong `sitemain-fe/src`):**
  ```
  src/
  ├── app/                 # routes (Next.js App Router)
  │   ├── (auth)/login/
  │   ├── (auth)/register/
  │   ├── products/
  │   │   └── [maSP]/
  │   ├── cart/
  │   ├── orders/
  │   ├── layout.tsx
  │   └── page.tsx
  ├── components/
  │   ├── ui/              # shadcn (đã có)
  │   └── shared/          # Header, Footer, Loading, PageContainer
  ├── features/            # logic theo nghiệp vụ
  │   ├── auth/
  │   ├── products/
  │   ├── cart/
  │   └── orders/
  ├── lib/                 # fetch wrapper, api clients, query client, auth store
  ├── types/               # type dùng chung (api, domain)
  └── constants/           # hằng số (endpoints, regions, query keys)
  ```
- **Kết quả hiện tại:** Cây thư mục đã tồn tại; các thư mục rỗng có `.gitkeep`.

## Task 3.2 — Tạo file biến môi trường ✅
- **Mục tiêu:** Khai báo 3 base URL của 3 backend.
- **File tạo:**
  - `sitemain-fe/.env.local`
    ```env
    NEXT_PUBLIC_MAIN_API_URL=http://localhost:8080
    NEXT_PUBLIC_NORTH_API_URL=http://localhost:8081
    NEXT_PUBLIC_SOUTH_API_URL=http://localhost:8082
    ```
  - `sitemain-fe/.env.example` (bản mẫu đã commit lên git).
- **Kết quả hiện tại:** FE đã có env mẫu và env local để chạy dev.

---

# PHASE 4 — Types & Constants ✅

## Task 4.1 — Type cho response chung của API ✅
- **Mục tiêu:** Bao bọc `ApiResponse`, `ErrorResponse`, `PageResponse` để dùng lại toàn dự án.
- **File tạo:** `src/types/api.ts`
  ```ts
  export interface ApiResponse<T> {
    success: true;
    message: string;
    data: T;
    timestamp: string;
  }

  export type ApiErrorCode =
    | "VALIDATION_ERROR" | "INVALID_CREDENTIALS" | "ACCESS_DENIED"
    | "RESOURCE_NOT_FOUND" | "DUPLICATE_EMAIL" | "DUPLICATE_PHONE"
    | "INVALID_REGION" | "INTERNAL_ERROR";

  export interface ApiErrorResponse {
    success: false;
    message: string;
    errorCode: ApiErrorCode;
    details: string[];
    timestamp: string;
  }

  export interface PageResponse<T> {
    items: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    last: boolean;
  }
  ```
- **Kết quả mong muốn:** Import được type, IntelliSense gợi ý đúng các field.

## Task 4.2 — Type cho domain (auth, product, danh mục...) ✅
- **Mục tiêu:** Type khớp đúng các DTO trong API_DOCUMENTATION.
- **File tạo:** `src/types/domain.ts`
  - `AuthResponse` (token, tokenType, expiresIn, userId, hoTen, email, maKhuVuc, vaiTro)
  - `ProductListItem`, `ProductDetail`
  - `Category` (`CategoryResponse`), `Brand` (`BrandResponse`), `Region` (`RegionResponse`)
  - `CartItem`, `Cart`, `Order`, `OrderItem` — *(định nghĩa tạm theo dự kiến API Bắc/Nam; chỉnh khi có doc Site Bắc/Nam)*
- **Kết quả mong muốn:** Mọi màn hình tham chiếu type tập trung ở đây, không khai báo rải rác.

## Task 4.3 — Hằng số khu vực, endpoint, query keys ✅
- **Mục tiêu:** Tránh hard-code chuỗi rải rác.
- **File tạo:**
  - `src/constants/regions.ts` — enum vùng + map `maKhuVuc → "BAC" | "NAM"`:
    ```ts
    export type RegionCode = "BAC" | "NAM";
    // DB đang lưu "Bac" / "Nam".
    // Chuẩn hoá hoa-thường trước khi map sang vùng định tuyến API.
    export const KHU_VUC_TO_REGION: Record<string, RegionCode> = {
      bac: "BAC",
      nam: "NAM",
    };
    ```
  - `src/constants/endpoints.ts` — đường dẫn path (vd `AUTH_LOGIN = "/api/auth/login"`).
  - `src/constants/query-keys.ts` — key cho TanStack Query (`["products", params]`, ...).
- **Kết quả mong muốn:** Đổi endpoint/mã vùng chỉ sửa 1 nơi.

---

# PHASE 5 — Fetch wrapper dùng chung ✅

## Task 5.1 — Viết `apiFetch` (fetch wrapper) ✅
- **Mục tiêu:** Một hàm gọi HTTP duy nhất: tự gắn `Content-Type`, gắn `Authorization` nếu có token, parse JSON, bóc `data` khi success, **ném `ApiError` khi lỗi**.
- **File tạo:**
  - `src/lib/api-error.ts` — class `ApiError extends Error` chứa `status`, `errorCode`, `details`.
  - `src/lib/api-fetch.ts`:
    ```ts
    interface ApiFetchOptions extends RequestInit {
      baseUrl: string;
      token?: string | null;
    }
    // - Ghép baseUrl + path
    // - Header: Content-Type JSON + Bearer token (nếu có)
    // - Đọc JSON, nếu res.ok && body.success → return body.data as T
    // - Ngược lại → throw new ApiError(body.message, res.status, body.errorCode, body.details)
    export async function apiFetch<T>(path: string, options: ApiFetchOptions): Promise<T> { ... }
    ```
- **Kết quả mong muốn:** Gọi `apiFetch<ProductDetail>("/api/products/SP001", { baseUrl })` trả thẳng object `data`, lỗi sẽ throw `ApiError` để UI bắt và hiển thị `message`.

## Task 5.2 — Xử lý 401 tập trung ✅
- **Mục tiêu:** Token hết hạn → tự xoá phiên, để app điều hướng về `/login`.
- **File chỉnh:** `src/lib/api-fetch.ts` (khi `status === 401 && errorCode === "INVALID_CREDENTIALS"` → gọi callback clear session).
- **Kết quả mong muốn:** Gọi API hết hạn token không làm crash app; người dùng được đưa về đăng nhập.

---

# PHASE 6 — API Clients ✅

## Task 6.1 — `mainApiClient` (Site Main) ✅
- **Mục tiêu:** Client cố định base URL Site Main cho auth + dữ liệu tham chiếu.
- **File tạo:** `src/lib/main-api-client.ts`
  ```ts
  const MAIN_URL = process.env.NEXT_PUBLIC_MAIN_API_URL!;
  export const mainApiClient = {
    get:  <T>(path: string, token?: string|null) => apiFetch<T>(path, { method: "GET", baseUrl: MAIN_URL, token }),
    post: <T>(path: string, body: unknown, token?: string|null) =>
            apiFetch<T>(path, { method: "POST", baseUrl: MAIN_URL, token, body: JSON.stringify(body) }),
  };
  ```
- **Kết quả mong muốn:** Mọi lời gọi sản phẩm/danh mục/thương hiệu/khu vực/auth đi qua client này.

## Task 6.2 — `regionalApiClient` (Site Bắc / Site Nam) ✅
- **Mục tiêu:** Client chọn base URL theo `RegionCode`, dùng cho giỏ hàng & đơn hàng.
- **File tạo:** `src/lib/regional-api-client.ts`
  ```ts
  const URL_BY_REGION: Record<RegionCode, string> = {
    BAC: process.env.NEXT_PUBLIC_NORTH_API_URL!,
    NAM: process.env.NEXT_PUBLIC_SOUTH_API_URL!,
  };
  export function getRegionalApiClient(region: RegionCode, token?: string|null) {
    const baseUrl = URL_BY_REGION[region];
    return {
      get:  <T>(path: string) => apiFetch<T>(path, { method: "GET", baseUrl, token }),
      post: <T>(path: string, body: unknown) => apiFetch<T>(path, { method: "POST", baseUrl, token, body: JSON.stringify(body) }),
      // patch/delete tương tự
    };
  }
  ```
- **Kết quả mong muốn:** `getRegionalApiClient("BAC")` gọi `:8081`, `getRegionalApiClient("NAM")` gọi `:8082`.

---

# PHASE 7 — Auth state (token + user + chọn vùng) ✅

## Task 7.1 — Lưu phiên đăng nhập ✅
- **Mục tiêu:** Sau login, lưu `accessToken` + thông tin user (từ `AuthResponse`) vào `localStorage`.
- **File tạo:** `src/lib/auth-storage.ts`
  - `saveSession(auth: AuthResponse)`, `getSession()`, `clearSession()`.
  - Lưu dưới 1 key, vd `"sitemain_session"`.
- **Kết quả mong muốn:** Reload trang vẫn giữ đăng nhập; logout xoá sạch.

## Task 7.2 — Suy ra vùng (`RegionCode`) từ user ✅
- **Mục tiêu:** Từ `user.maKhuVuc` → `"BAC" | "NAM"` để chọn API vùng.
- **File tạo:** `src/features/auth/region.ts`
  ```ts
  export function getUserRegion(maKhuVuc?: string): RegionCode | null {
    if (!maKhuVuc) return null;
    return KHU_VUC_TO_REGION[maKhuVuc.trim().toLowerCase()] ?? null;
  }
  ```
- **Kết quả mong muốn:** Có hàm thuần trả về vùng; nếu mã lạ → `null` (UI báo "tài khoản chưa gán khu vực hợp lệ").

## Task 7.3 — `AuthProvider` (React Context, không Redux/Zustand) ✅
- **Mục tiêu:** Cung cấp `user`, `token`, `region`, `login()`, `logout()` cho toàn app.
- **File tạo:** `src/features/auth/auth-context.tsx`
  - Khởi tạo state từ `getSession()`.
  - `useAuth()` hook trả về `{ user, token, region, isAuthenticated, login, logout }`.
- **Kết quả mong muốn:** Component bất kỳ gọi `useAuth()` lấy được trạng thái đăng nhập + vùng.

---

# PHASE 8 — Layout, Providers, tiện ích chung ✅

## Task 8.1 — QueryClient + Provider ✅
- **Mục tiêu:** Bật TanStack Query toàn app.
- **File tạo:** `src/lib/query-client.ts` (cấu hình `staleTime`, retry) + `src/app/providers.tsx` (gói `QueryClientProvider`, `AuthProvider`, `Toaster`, Devtools).
- **File chỉnh:** `src/app/layout.tsx` → bọc `children` bằng `<Providers>`.
- **Kết quả mong muốn:** `useQuery`/`useMutation` dùng được ở mọi page; toast hiển thị được.

## Task 8.2 — Header + khung trang dùng chung ✅
- **Mục tiêu:** Thanh điều hướng có link Sản phẩm / Giỏ hàng / Đơn hàng + trạng thái đăng nhập.
- **File tạo:** `src/components/shared/header.tsx`, `page-container.tsx`, `loading.tsx`.
- **Kết quả mong muốn:** Header hiển thị tên user + nút Đăng xuất khi đã login; nút Đăng nhập khi chưa.

---

# PHASE 9 — Feature: Auth (login / register) ✅

## Task 9.1 — Schema validate bằng Zod ✅
- **Mục tiêu:** Validate form khớp ràng buộc BE (email, mật khẩu 6–72, SĐT regex, maKhuVuc...).
- **File tạo:** `src/features/auth/schemas.ts` (`loginSchema`, `registerSchema`).
- **Kết quả mong muốn:** Sai định dạng hiển thị lỗi ngay tại field trước khi gửi API.

## Task 9.2 — Hook gọi API auth (TanStack Query mutation) ✅
- **Mục tiêu:** `useLogin`, `useRegister` gọi `mainApiClient`, khi thành công gọi `login()` của context.
- **File tạo:** `src/features/auth/use-auth-mutations.ts`.
  - `POST /api/auth/login`, `POST /api/auth/register` → `AuthResponse`.
- **Kết quả mong muốn:** Login thành công → lưu phiên + điều hướng `/products`; lỗi → toast `message` từ `ApiError`.

## Task 9.3 — Trang `/login` ✅
- **Mục tiêu:** Form đăng nhập (email, mật khẩu) dùng RHF + zodResolver + shadcn Form.
- **File tạo:** `src/app/(auth)/login/page.tsx`, `src/features/auth/login-form.tsx`.
- **Kết quả mong muốn:** Đăng nhập đúng → vào trang sản phẩm; sai → báo "Sai email hoặc mật khẩu".

## Task 9.4 — Trang `/register` ✅
- **Mục tiêu:** Form đăng ký đầy đủ field; dropdown **khu vực** đổ từ `GET /api/regions`.
- **File tạo:** `src/app/(auth)/register/page.tsx`, `src/features/auth/register-form.tsx`, `src/features/auth/use-regions.ts`.
- **Kết quả mong muốn:** Đăng ký thành công → tự đăng nhập (vì BE trả `AuthResponse`) → vào `/products`. Trùng email/SĐT → toast đúng thông điệp.

---

# PHASE 10 — Feature: Sản phẩm (Site Main) ✅

## Task 10.1 — Hook danh sách & chi tiết sản phẩm ✅
- **Mục tiêu:** `useProducts(params)` (phân trang/lọc) + `useProduct(maSP)`.
- **File tạo:** `src/features/products/use-products.ts`.
  - `GET /api/products?page&size&sort&maDanhMuc&maThuongHieu&trangThai` → `PageResponse<ProductListItem>`.
  - `GET /api/products/{maSP}` → `ProductDetail`.
- **Kết quả mong muốn:** Dữ liệu cache, có loading/skeleton, đổi trang không reload toàn trang.

## Task 10.2 — Hook dữ liệu tham chiếu (filter) ✅
- **Mục tiêu:** `useCategories`, `useBrands` cho bộ lọc.
- **File tạo:** `src/features/products/use-reference.ts` (gọi `GET /api/categories`, `/api/brands`).
- **Kết quả mong muốn:** Dropdown lọc danh mục/thương hiệu hoạt động.

## Task 10.3 — Trang `/products` (danh sách + tìm kiếm + lọc + phân trang) ✅
- **Mục tiêu:** Lưới sản phẩm (Card), tìm kiếm text, bộ lọc danh mục/thương hiệu/trạng thái, sắp xếp, size trang, nút phân trang.
- **File tạo:** `src/app/products/page.tsx`, `src/features/products/product-list-page.tsx`, `product-card.tsx`, `product-grid.tsx`, `product-filters.tsx`, `pagination.tsx`.
- **Kết quả hiện tại:** URL đồng bộ `q`, `maDanhMuc`, `maThuongHieu`, `trangThai`, `sort`, `size`, `page`. `q` lọc frontend trên items của trang hiện tại vì backend `/api/products` chưa có tham số tìm kiếm toàn cục.

## Task 10.4 — Trang `/products/[maSP]` (chi tiết) ✅
- **Mục tiêu:** Trang chi tiết: ảnh, giá, mô tả, thông số, nút **Thêm vào giỏ**.
- **File tạo:** `src/app/products/[maSP]/page.tsx`, `src/features/products/product-detail.tsx`.
- **Kết quả mong muốn:** Mã không tồn tại (`404 RESOURCE_NOT_FOUND`) → hiển thị "Không tìm thấy sản phẩm". Nút thêm giỏ yêu cầu đăng nhập.

## Task 10.5 — Trang chủ `/` ✅
- **Mục tiêu:** Landing đơn giản: banner + vài sản phẩm nổi bật + CTA sang `/products`.
- **File chỉnh/tạo:** `src/app/page.tsx`.
- **Kết quả mong muốn:** Trang chủ gọn, dẫn người dùng vào luồng mua hàng.

---

# PHASE 11 — Feature: Giỏ hàng (Site Bắc/Nam theo vùng) ✅

> Endpoint giỏ hàng dùng path `/api/cart*` nhưng base URL được chọn theo user: Bắc → `NEXT_PUBLIC_NORTH_API_URL` (`:8081`), Nam → `NEXT_PUBLIC_SOUTH_API_URL` (`:8082`). Mọi lời gọi đi qua `getRegionalApiClient(region, token)`, không gọi Site Main.

## Task 11.1 — Hook giỏ hàng theo vùng ✅
- **Mục tiêu:** `useCart`, `useAddToCart`, `useUpdateCartItem`, `useRemoveCartItem` — lấy `region` + `token` từ `useAuth()`.
- **File tạo:** `src/features/cart/use-cart.ts`.
  - Nếu chưa đăng nhập hoặc `region === null` → không gọi API, yêu cầu đăng nhập.
- **Kết quả hiện tại:** Query key `QK.cart(region)` tách cache theo vùng; user Bắc thao tác giỏ trên `:8081`, user Nam trên `:8082`.

## Task 11.2 — Trang `/cart` ✅
- **Mục tiêu:** Danh sách item, tăng/giảm số lượng, xoá, tổng tiền, nút **Đặt hàng**.
- **File tạo:** `src/app/cart/page.tsx`, `src/features/cart/cart-item-row.tsx`, `cart-summary.tsx`, `cart-rules.ts`, `src/types/cart.ts`.
- **Kết quả hiện tại:** Giỏ phản ánh đúng dữ liệu vùng; đổi số lượng cập nhật tổng tiền; chặn vượt tồn; hết hàng tách riêng; chưa đăng nhập hiển thị CTA đăng nhập.

---

# PHASE 12 — Feature: Đơn hàng (Site Bắc/Nam theo vùng)

> ⚠️ Tương tự giỏ hàng: path **dự kiến** `/api/orders`, gọi qua `getRegionalApiClient(region)`.

## Task 12.1 — Hook đơn hàng theo vùng
- **Mục tiêu:** `useOrders` (danh sách), `useOrder(id)` (chi tiết), `useCreateOrder` (đặt từ giỏ).
- **File tạo:** `src/features/orders/use-orders.ts`.
- **Kết quả mong muốn:** Đặt hàng thành công → xoá giỏ + điều hướng `/orders`; đơn hiển thị đúng theo vùng user.

## Task 12.2 — Trang `/orders`
- **Mục tiêu:** Lịch sử đơn hàng của user (bảng), bấm xem chi tiết.
- **File tạo:** `src/app/orders/page.tsx`, `src/features/orders/order-list.tsx`, `order-detail.tsx` (hoặc dialog).
- **Kết quả mong muốn:** Hiển thị danh sách đơn từ đúng Site Bắc/Nam; trạng thái rỗng có thông báo "Chưa có đơn hàng".

---

# PHASE 13 — Hoàn thiện & chuẩn bị demo

## Task 13.1 — Bảo vệ route cần đăng nhập
- **Mục tiêu:** `/cart`, `/orders` yêu cầu đăng nhập.
- **File tạo:** `src/components/shared/require-auth.tsx` (client guard dùng `useAuth()`; chưa login → redirect `/login`).
- **Kết quả mong muốn:** Truy cập trực tiếp `/cart` khi chưa login → tự về `/login`.

## Task 13.2 — Trạng thái lỗi & loading toàn cục
- **Mục tiêu:** UX nhất quán khi gọi API.
- **File tạo:** `src/app/error.tsx`, `src/app/not-found.tsx`, dùng `<Skeleton>` ở các trang list/detail.
- **Kết quả mong muốn:** Không có màn hình trắng; lỗi mạng/API hiển thị thông điệp thân thiện.

## Task 13.3 — README hướng dẫn chạy demo
- **Mục tiêu:** Người chấm đồ án chạy được trong 1 phút.
- **File tạo:** `sitemain-fe/README.md` — yêu cầu: bật 3 BE (8080/8081/8082), copy `.env.example` → `.env.local`, `bun install`, `bun dev`, kịch bản demo: đăng ký user Bắc & user Nam → thấy giỏ/đơn gọi 2 BE khác nhau.
- **Kết quả mong muốn:** Làm theo README dựng được toàn bộ luồng demo phân tán.

---

## Phụ lục A — Mapping endpoint ↔ client

| Nghiệp vụ | Method/Path | Client | Auth |
|---|---|---|---|
| Đăng ký | `POST /api/auth/register` | `mainApiClient` | ❌ |
| Đăng nhập | `POST /api/auth/login` | `mainApiClient` | ❌ |
| Check email/phone | `GET /api/auth/check-*` | `mainApiClient` | ❌ |
| Danh sách SP | `GET /api/products` | `mainApiClient` | ❌ |
| Chi tiết SP | `GET /api/products/{maSP}` | `mainApiClient` | ❌ |
| Danh mục | `GET /api/categories` | `mainApiClient` | ❌ |
| Thương hiệu | `GET /api/brands` | `mainApiClient` | ❌ |
| Khu vực | `GET /api/regions` | `mainApiClient` | ❌ |
| Giỏ hàng | `/api/cart*` | `regionalApiClient(region)` | ✅ |
| Đơn hàng | `/api/orders*` *(dự kiến)* | `regionalApiClient(region)` | ✅ |

## Phụ lục B — Checklist nghiệm thu nhanh

- [ ] `bun dev` chạy, không lỗi console.
- [ ] Xem `/products`, tìm kiếm + lọc + phân trang OK (gọi `:8080`; cần backend Main/SQL Server chạy được để nghiệm thu dữ liệu thật).
- [ ] Đăng ký + đăng nhập OK, reload vẫn giữ phiên.
- [ ] User vùng Bắc: `/cart`, `/orders` gọi `:8081` (kiểm tra Network).
- [ ] User vùng Nam: `/cart`, `/orders` gọi `:8082`.
- [ ] Token hết hạn / chưa login vào `/cart` → về `/login`.
- [ ] Lỗi API hiển thị `message` tiếng Việt, không crash.

---

> **Ghi chú:** FE hiện đã gọi giỏ hàng qua Site Bắc/Nam theo vùng. Endpoint đơn hàng (`/api/orders`) vẫn là phần tiếp theo ở Phase 12.
