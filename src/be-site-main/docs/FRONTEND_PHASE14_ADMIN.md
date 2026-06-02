# FRONTEND PHASE 14 — Khu vực Admin (Quản trị) (chi tiết)

> **Phạm vi:** Xây khu vực `/admin` **ngay trong `sitemain-fe`** (route group, không tách project), **giữ nguyên toàn bộ cấu hình & hạ tầng của fe-client hiện có** (Bun, Next 16 App Router, Tailwind v4, shadcn/ui, TanStack Query, Zod, RHF, `apiFetch`/`mainApiClient`, auth context).
>
> **Mục tiêu:** Quản lý **Sản phẩm**, **Danh mục**, **Thương hiệu** (CRUD) và **xem Thống kê**. Tất cả gọi **Site Main** (`mainApiClient`) — đây là domain của Site Main, không liên quan định tuyến regional.
>
> **Quyền truy cập:** chỉ `vaiTro === "ADMIN"`.

---

## 0. Trạng thái API backend (đọc trước khi code)

Backend Site Main đã có API admin cho phase này:

- CRUD sản phẩm/danh mục/thương hiệu: `POST/PUT/DELETE /api/products`, `/api/categories`, `/api/brands`.
- Thống kê doanh thu toàn hệ thống: `GET /api/admin/thong-ke/doanh-thu`.
- Phân quyền: mọi endpoint ghi catalog và toàn bộ `/api/admin/**` yêu cầu Bearer token có role `ADMIN`.
- Contract chính thức cho FE nằm trong [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md) mục **5.4 Admin — quản trị catalog & thống kê**.

Khi gọi mà token thiếu/sai/hết hạn → `401 INVALID_CREDENTIALS`; token hợp lệ nhưng không phải ADMIN → `403 ACCESS_DENIED`.

---

## 1. Hợp đồng API thực tế — nguồn sự thật cho FE

Tất cả bọc trong `ApiResponse<T>` / `ErrorResponse` như phần còn lại của hệ thống (xem `API_DOCUMENTATION.md`). Field domain giữ đúng tên backend đang dùng (`maSP`, `tenSP`, `giaBan`, `maDanhMuc`, `maThuongHieu`, `trangThai`...).

### 1.1. Sản phẩm

| Method | Path | Auth | Body | data trả về |
|---|---|---|---|---|
| GET | `/api/products?...&trangThai=` | ❌ public | — | `PageResponse<ProductListItem>` (admin xem cả `trangThai=false`) |
| GET | `/api/products/{maSP}` | ❌ public | — | `ProductDetail` |
| POST | `/api/products` | 🔐 ADMIN | `ProductUpsert` | `ProductDetail` |
| PUT | `/api/products/{maSP}` | 🔐 ADMIN | `ProductUpsert` (`maSP` phải trùng path) | `ProductDetail` |
| DELETE | `/api/products/{maSP}` | 🔐 ADMIN | — | `null` (xoá hoặc soft-delete `trangThai=false`) |

```ts
// ProductUpsert (khớp ProductDetailResponse + phần detail)
interface ProductUpsert {
  maSP: string;           // khi PUT phải trùng với path {maSP}
  tenSP: string;
  giaBan: number;         // > 0
  donViTinh: string;
  hinhAnh?: string;
  trangThai: boolean;
  maDanhMuc: string;
  maThuongHieu: string;
  moTa?: string;
  thongSoKyThuat?: string;
}
```

### 1.2. Danh mục

| Method | Path | Auth | Body | data |
|---|---|---|---|---|
| GET | `/api/categories` | ❌ | — | `Category[]` |
| POST | `/api/categories` | 🔐 ADMIN | `CategoryUpsert` | `Category` |
| PUT | `/api/categories/{maDanhMuc}` | 🔐 ADMIN | `CategoryUpsert` (`maDanhMuc` phải trùng path) | `Category` |
| DELETE | `/api/categories/{maDanhMuc}` | 🔐 ADMIN | — | `null` |

```ts
interface CategoryUpsert {
  maDanhMuc: string;      // khi PUT phải trùng với path {maDanhMuc}
  tenDanhMuc: string;
  maDanhMucCha?: string;  // danh mục cha (tùy chọn)
  moTa?: string;
  trangThai: boolean;
}
```

### 1.3. Thương hiệu

| Method | Path | Auth | Body | data |
|---|---|---|---|---|
| GET | `/api/brands` | ❌ | — | `Brand[]` |
| POST | `/api/brands` | 🔐 ADMIN | `BrandUpsert` | `Brand` |
| PUT | `/api/brands/{maThuongHieu}` | 🔐 ADMIN | `BrandUpsert` (`maThuongHieu` phải trùng path) | `Brand` |
| DELETE | `/api/brands/{maThuongHieu}` | 🔐 ADMIN | — | `null` |

```ts
interface BrandUpsert {
  maThuongHieu: string;   // khi PUT phải trùng với path {maThuongHieu}
  tenThuongHieu: string;
  trangThai: boolean;
}
```

### 1.4. Thống kê

| Method | Path | Auth | data |
|---|---|---|---|
| GET | `/api/admin/thong-ke/doanh-thu` | 🔐 ADMIN | `ThongKeDoanhThu` |

```ts
interface DoanhThuTheoKho {
  siteXuat: string;
  maKhuVuc: string;
  maKhoXuat: string;
  tenKho: string;
  soDonHang: number;
  soPhieuXuat: number;
  tongSoLuongXuat: number;
  doanhThu: number;
}
interface DoanhThuTheoVung {
  maKhuVuc: string;
  soDonHang: number;
  soPhieuXuat: number;
  soKhoThamGiaXuat: number;
  tongSoLuongXuat: number;
  doanhThu: number;
}
interface DoanhThuToanHeThong {
  tongSoDonHang: number;
  tongSoPhieuXuat: number;
  tongSoKhoThamGiaXuat: number;
  tongSoLuongXuat: number;
  tongDoanhThu: number;
}
interface ThongKeDoanhThu {
  theoKho: DoanhThuTheoKho[];
  theoVung: DoanhThuTheoVung[];
  toanHeThong: DoanhThuToanHeThong;
}
```

Query optional: `tuNgay`, `denNgay` (`YYYY-MM-DD`), `maKho`, `maKhuVuc`, `maSP`, `chiTinhDaXuat`. Dashboard nên dùng `toanHeThong.tongDoanhThu` làm KPI chính, biểu đồ theo `theoVung`, bảng chi tiết theo `theoKho`.

> Lỗi chung dùng lại bảng mã ở `API_DOCUMENTATION.md`: `VALIDATION_ERROR` (400, kèm `details[]`), `RESOURCE_NOT_FOUND` (404), `ACCESS_DENIED` (403), `INVALID_CREDENTIALS` (401), và `DUPLICATE_*` nếu mã trùng khi tạo.

---

## Task 14.1 — Mở rộng hạ tầng dùng chung (không phá fe-client)

**Mục tiêu:** bổ sung tối thiểu vào lib/constants hiện có để admin tái dùng, **không sửa hành vi client cũ**.

- **`src/lib/main-api-client.ts`** — thêm `put` và `delete` (hiện mới có `get`/`post`):
  ```ts
  put: <T>(path: string, body: unknown, options?: MutationOptions) =>
    apiFetch<T>(path, { method: "PUT", baseUrl: getMainBaseUrl(), body,
      token: options?.token, headers: options?.headers, signal: options?.signal,
      cache: options?.cache, next: options?.next }),

  delete: <T>(path: string, options?: MutationOptions) =>
    apiFetch<T>(path, { method: "DELETE", baseUrl: getMainBaseUrl(),
      token: options?.token, headers: options?.headers, signal: options?.signal,
      cache: options?.cache, next: options?.next }),
  ```
- **`src/constants/endpoints.ts`** — thêm path admin vào `MAIN_ENDPOINTS`:
  ```ts
  PRODUCT_CREATE: "/api/products",
  PRODUCT_UPDATE: (maSP: string) => `/api/products/${encodeURIComponent(maSP)}`,
  PRODUCT_DELETE: (maSP: string) => `/api/products/${encodeURIComponent(maSP)}`,
  CATEGORY_ITEM: (ma: string) => `/api/categories/${encodeURIComponent(ma)}`,
  BRAND_ITEM: (ma: string) => `/api/brands/${encodeURIComponent(ma)}`,
  ADMIN_STATS_REVENUE: "/api/admin/thong-ke/doanh-thu",
  ```
- **`src/constants/query-keys.ts`** — thêm key admin (tách khỏi key client để cache không lẫn):
  ```ts
  adminProducts: (params?: unknown) => ["admin", "products", params ?? {}] as const,
  adminStatsRevenue: (params?: unknown) => ["admin", "stats", "revenue", params ?? {}] as const,
  // categories/brands dùng lại QK.categories()/QK.brands() đã có
  ```
- **`src/types/admin.ts`** (mới) — khai `ProductUpsert`, `CategoryUpsert`, `BrandUpsert`, `ThongKeDoanhThu`, `DoanhThuTheoKho`, `DoanhThuTheoVung`, `DoanhThuToanHeThong`, `RevenueStatsFilter` (đúng mục 1). Tái dùng `Category`/`Brand`/`ProductDetail`/`ProductListItem` từ `types/domain.ts`.

**Kết quả:** client cũ chạy y nguyên; admin có đủ "đồ nghề" gọi API.

---

## Task 14.2 — Phân quyền & guard route

**Mục tiêu:** chỉ ADMIN vào được `/admin/**`; người khác bị chặn.

- **File:** `src/features/admin/require-admin.tsx`
  ```tsx
  "use client";
  import { useEffect } from "react";
  import { useRouter } from "next/navigation";
  import { useAuth } from "@/features/auth/auth-context";

  export function RequireAdmin({ children }: { children: React.ReactNode }) {
    const { isAuthenticated, user } = useAuth();
    const router = useRouter();
    const isAdmin = user?.vaiTro === "ADMIN";

    useEffect(() => {
      if (!isAuthenticated) router.replace("/login?redirect=/admin");
      else if (!isAdmin) router.replace("/admin/forbidden");
    }, [isAuthenticated, isAdmin, router]);

    if (!isAuthenticated || !isAdmin) return null; // tránh nháy nội dung
    return <>{children}</>;
  }
  ```
- **Kiểm tra:** `useAuth().user.vaiTro` — xác nhận `StoredSession.user` có `vaiTro` (xem `auth-storage.ts`; `AuthResponse.vaiTro` đã có trong `types/domain.ts`). Nếu session chưa lưu `vaiTro`, bổ sung khi `saveSession`.
- **Phòng tuyến kép:** guard FE chỉ là UX; backend vẫn enforce `403 ACCESS_DENIED`. Hook admin phải xử lý `ApiError` 403 → toast "Không đủ quyền".
- **Trang `src/app/admin/forbidden/page.tsx`** — thông báo 403 + link về `/`.

**Kết quả:** USER/khách bị đẩy ra; ADMIN vào được.

---

## Task 14.3 — Khung layout Admin (shell)

**Mục tiêu:** layout riêng cho `/admin` với sidebar điều hướng + header, tách hẳn giao diện store.

- **File:** `src/app/admin/layout.tsx`
  ```tsx
  import { RequireAdmin } from "@/features/admin/require-admin";
  import { AdminSidebar } from "@/features/admin/admin-sidebar";

  export default function AdminLayout({ children }: { children: React.ReactNode }) {
    return (
      <RequireAdmin>
        <div className="grid min-h-screen grid-cols-[240px_1fr]">
          <AdminSidebar />
          <main className="p-6">{children}</main>
        </div>
      </RequireAdmin>
    );
  }
  ```
- **File:** `src/features/admin/admin-sidebar.tsx` — nav dùng `next/link` + `usePathname()` để active state. Mục: Thống kê (`/admin`), Sản phẩm (`/admin/products`), Danh mục (`/admin/categories`), Thương hiệu (`/admin/brands`); footer có tên admin + nút Đăng xuất (`useAuth().logout`).
- **Tái dùng:** `AuthProvider` & `QueryClientProvider` đã bọc ở `app/providers.tsx` (root) nên admin tự có; **không cần** provider mới.
- **Icon:** `lucide-react` (đã cài). Component nền: `Card`, `Separator`, `Button` (shadcn đã có).

**Kết quả:** vào `/admin` thấy shell với sidebar; điều hướng giữa 4 mục.

---

## Task 14.4 — Dashboard Thống kê doanh thu (`/admin`)

**Mục tiêu:** trang mặc định của admin: KPI doanh thu toàn hệ thống, bộ lọc ngày/khu/kho/sản phẩm, biểu đồ doanh thu theo vùng và bảng chi tiết theo kho.

- **Hook:** `src/features/admin/use-admin-stats.ts`
  ```ts
  import { useQuery } from "@tanstack/react-query";
  import { useAuth } from "@/features/auth/auth-context";
  import { mainApiClient } from "@/lib/main-api-client";
  import { MAIN_ENDPOINTS } from "@/constants/endpoints";
  import { QK } from "@/constants/query-keys";
import type { RevenueStatsFilter, ThongKeDoanhThu } from "@/types/admin";

export function useRevenueStats(filters: RevenueStatsFilter) {
  const { token } = useAuth();
  return useQuery({
    queryKey: QK.adminStatsRevenue(filters),
    queryFn: () =>
      mainApiClient.get<ThongKeDoanhThu>(MAIN_ENDPOINTS.ADMIN_STATS_REVENUE, {
        token,
        query: filters,
      }),
    staleTime: 30_000,
  });
}
```
- **File:** `src/app/admin/page.tsx` — render form filter (`tuNgay`, `denNgay`, `maKhuVuc`, `maKho`, `maSP`, `chiTinhDaXuat`) + `<RevenueKpiCards />` + `<RevenueByRegionChart />` + `<RevenueByWarehouseTable />`.
- **KPI:** `toanHeThong.tongDoanhThu`, `tongSoDonHang`, `tongSoPhieuXuat`, `tongSoLuongXuat`, `tongSoKhoThamGiaXuat`.
- **Biểu đồ:** **không thêm dependency mới** — vẽ thanh ngang theo `theoVung[].doanhThu` bằng CSS trong `Card`.
- **Bảng chi tiết:** dùng `theoKho`, cột: site, mã vùng, mã kho, tên kho, số đơn, số phiếu xuất, số lượng xuất, doanh thu.
- **Trạng thái:** loading → `Skeleton`; error → thẻ "Không tải được thống kê doanh thu" + nút thử lại. Nếu lỗi `500 INTERNAL_ERROR`, hiển thị note "Kiểm tra DB/linked server SITE_BAC/SITE_NAM".

**Kết quả:** `/admin` hiển thị dashboard doanh thu thật từ Site Main backend.

---

## Task 14.5 — Quản lý Sản phẩm (`/admin/products`)

Đây là module nặng nhất. Chia 3 phần: danh sách, form thêm/sửa, xoá.

### 14.5.a — Hook CRUD `src/features/admin/use-admin-products.ts`
```ts
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/auth-context";
import { mainApiClient } from "@/lib/main-api-client";
import { MAIN_ENDPOINTS } from "@/constants/endpoints";
import { QK } from "@/constants/query-keys";
import { isApiError } from "@/lib/api-error";
import type { PageResponse } from "@/types/api";
import type { ProductListItem, ProductDetail } from "@/types/domain";
import type { ProductUpsert } from "@/types/admin";

export function useAdminProducts(params: Record<string, unknown>) {
  const { token } = useAuth();
  return useQuery({
    queryKey: QK.adminProducts(params),
    // admin xem cả hàng ngừng bán → KHÔNG ép trangThai
    queryFn: () => mainApiClient.get<PageResponse<ProductListItem>>(
      MAIN_ENDPOINTS.PRODUCTS, { token, query: { size: 20, ...params } }),
    placeholderData: keepPreviousData,
  });
}

export function useCreateProduct() {
  const { token } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ProductUpsert) =>
      mainApiClient.post<ProductDetail>(MAIN_ENDPOINTS.PRODUCT_CREATE, input, { token }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["admin", "products"] }); toast.success("Đã tạo sản phẩm"); },
    onError: (e) => toast.error(isApiError(e) ? e.message : "Tạo sản phẩm thất bại"),
  });
}

export function useUpdateProduct() {
  const { token } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ maSP, data }: { maSP: string; data: ProductUpsert }) =>
      mainApiClient.put<ProductDetail>(MAIN_ENDPOINTS.PRODUCT_UPDATE(maSP), data, { token }),
    onSuccess: (_d, v) => {
      qc.invalidateQueries({ queryKey: ["admin", "products"] });
      qc.invalidateQueries({ queryKey: QK.product(v.maSP) }); // đồng bộ trang client
      toast.success("Đã cập nhật sản phẩm");
    },
    onError: (e) => toast.error(isApiError(e) ? e.message : "Cập nhật thất bại"),
  });
}

export function useDeleteProduct() {
  const { token } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (maSP: string) => mainApiClient.delete<void>(MAIN_ENDPOINTS.PRODUCT_DELETE(maSP), { token }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["admin", "products"] }); toast.success("Đã xoá sản phẩm"); },
    onError: (e) => toast.error(isApiError(e) ? e.message : "Xoá thất bại"),
  });
}
```

### 14.5.b — Schema form (Zod) `src/features/admin/product-schema.ts`
```ts
import { z } from "zod";
export const productSchema = z.object({
  maSP: z.string().min(1, "Mã sản phẩm bắt buộc").max(20),
  tenSP: z.string().min(1, "Tên bắt buộc"),
  giaBan: z.coerce.number().positive("Giá phải > 0"),
  donViTinh: z.string().min(1, "Đơn vị tính bắt buộc"),
  hinhAnh: z.string().optional(),
  trangThai: z.boolean().default(true),
  maDanhMuc: z.string().min(1, "Chọn danh mục"),
  maThuongHieu: z.string().min(1, "Chọn thương hiệu"),
  moTa: z.string().optional(),
  thongSoKyThuat: z.string().optional(),
});
export type ProductForm = z.infer<typeof productSchema>;
```
> Khi **sửa**, `maSP` readonly (PK). Khi **tạo**, cho nhập. Dùng cùng schema, set `disabled` field theo mode.

### 14.5.c — UI
- **`src/app/admin/products/page.tsx`** — `useAdminProducts`, thanh lọc (theo danh mục/thương hiệu/trạng thái — tái dùng `useReference`/`use-reference.ts` đã có để đổ option), nút "Thêm sản phẩm", `Table` (shadcn) cột: ảnh, maSP, tenSP, danh mục, thương hiệu, giá (`formatVnd` từ `lib/format.ts`), trạng thái (`Badge`), thao tác (Sửa/Xoá), `Pagination` (tái dùng `features/products/pagination.tsx`).
- **`src/features/admin/product-form-dialog.tsx`** — `Dialog` + `Form` (RHF + `zodResolver`). Select danh mục/thương hiệu nạp từ `useCategories`/`useBrands` (đã có ở `use-reference.ts`). Submit → `useCreateProduct` hoặc `useUpdateProduct` rồi đóng dialog.
- **`src/features/admin/confirm-delete.tsx`** — `Dialog` xác nhận dùng chung (truyền tên + callback) cho cả 3 module.

### 14.5.d — Map lỗi validation từ backend
Khi `ApiError.errorCode === "VALIDATION_ERROR"`, `details[]` dạng `"giaBan: Giá phải > 0"`. Tạo helper `applyServerErrors(form, error)` để `form.setError` theo từng field → hiển thị ngay dưới input.

**Kết quả:** liệt kê (gồm hàng ngừng bán), lọc/phân trang; thêm/sửa qua dialog; xoá có xác nhận; lỗi validate hiện đúng field.

---

## Task 14.6 — Quản lý Danh mục (`/admin/categories`)

**Mục tiêu:** CRUD danh mục, hỗ trợ chọn **danh mục cha**.

- **Hook:** `src/features/admin/use-admin-categories.ts` — `useCategories()` (đã có) cho danh sách + `useCreateCategory`/`useUpdateCategory`/`useDeleteCategory` (mẫu như 14.5.a, endpoint `CATEGORIES` / `CATEGORY_ITEM`). `onSuccess` → `invalidateQueries({ queryKey: QK.categories() })`.
- **Schema:** `tenDanhMuc` bắt buộc; `maDanhMucCha` optional (Select từ chính danh sách danh mục, loại trừ chính nó khi sửa để tránh tự làm cha); `trangThai` boolean; `maDanhMuc` nhập khi tạo.
- **UI:** `src/app/admin/categories/page.tsx` — `Table` (mã, tên, danh mục cha, trạng thái, thao tác) + dialog form + confirm-delete (tái dùng).
- **Ràng buộc nghiệp vụ:** xoá danh mục hiện là soft-delete (`trangThai=false`). FE hiển thị lại trạng thái sau khi invalidate cache.

**Kết quả:** quản lý danh mục phẳng/cây 1 cấp; chọn cha; thêm/sửa/xoá.

---

## Task 14.7 — Quản lý Thương hiệu (`/admin/brands`)

**Mục tiêu:** CRUD thương hiệu (đơn giản nhất — chỉ tên + trạng thái).

- **Hook:** `src/features/admin/use-admin-brands.ts` — `useBrands()` (đã có) + `useCreateBrand`/`useUpdateBrand`/`useDeleteBrand` (endpoint `BRANDS` / `BRAND_ITEM`), invalidate `QK.brands()`.
- **Schema:** `tenThuongHieu` bắt buộc; `trangThai` boolean; `maThuongHieu` khi tạo.
- **UI:** `src/app/admin/brands/page.tsx` — `Table` (mã, tên, trạng thái, thao tác) + dialog form + confirm-delete.

**Kết quả:** quản lý thương hiệu đầy đủ CRUD.

---

## Task 14.8 — Hoàn thiện & kiểm thử thủ công

- **Đồng bộ cache client:** sau sửa/xoá sản phẩm/danh mục/thương hiệu, invalidate cả query của store (`QK.products`, `QK.product`, `QK.categories`, `QK.brands`) để trang khách thấy thay đổi.
- **Trạng thái UI mọi bảng:** loading (`Skeleton`), empty (`EmptyState` + nút "Thêm…"), error (thông báo + thử lại).
- **Toast** (`sonner` đã cấu hình) cho mọi mutation thành công/lỗi.
- **Ảnh sản phẩm:** giai đoạn này dùng **URL string** (`hinhAnh`); upload file là phần mở rộng sau và chưa nằm trong scope phase 14.
- **403 thực tế:** đăng nhập user thường rồi vào `/admin` → phải bị đẩy ra `/admin/forbidden`; gọi API admin (nếu lách guard) → toast "Không đủ quyền".
- **Đăng nhập admin:** dùng luồng `/login` sẵn có; sau khi có token ADMIN, vào `/admin`.

**Checklist tổng:**
- [x] 14.1 `mainApiClient.put/delete`, endpoints admin, query-keys admin, `types/admin.ts`.
- [x] 14.2 `RequireAdmin` + trang `forbidden`; xác nhận session lưu `vaiTro`.
- [x] 14.3 `admin/layout.tsx` + sidebar (active state, đăng xuất).
- [x] 14.4 Dashboard: KPI doanh thu + filter + biểu đồ theo vùng + bảng theo kho (không thêm dep).
- [x] 14.5 Sản phẩm: list (cả hàng ngừng bán) + lọc + phân trang + form thêm/sửa + xoá + map lỗi validate.
- [x] 14.6 Danh mục: CRUD + chọn danh mục cha.
- [x] 14.7 Thương hiệu: CRUD.
- [x] 14.8 Invalidate cache client, trạng thái UI, toast, kiểm thử 403.
- [x] Đối chiếu lại lần cuối với `API_DOCUMENTATION.md` mục 5.4 trước khi code FE.

---

## Phụ lục — Cây thư mục dự kiến (trong `sitemain-fe/src`)

```
app/admin/
  layout.tsx                 # RequireAdmin + shell
  page.tsx                   # dashboard thống kê
  products/page.tsx
  categories/page.tsx
  brands/page.tsx
  forbidden/page.tsx
features/admin/
  require-admin.tsx
  admin-sidebar.tsx
  confirm-delete.tsx
  use-admin-stats.ts
  use-admin-products.ts      product-schema.ts      product-form-dialog.tsx
  use-admin-categories.ts    category-schema.ts     category-form-dialog.tsx
  use-admin-brands.ts        brand-schema.ts        brand-form-dialog.tsx
types/admin.ts
# Sửa nhẹ: lib/main-api-client.ts, constants/endpoints.ts, constants/query-keys.ts
```

> **Nguyên tắc xuyên suốt:** chỉ **thêm**, hạn chế **sửa** code fe-client hiện có (chỉ mở rộng `mainApiClient`, `endpoints`, `query-keys`). Admin & client dùng chung 1 app, 1 deploy, 1 `.env.local` (`NEXT_PUBLIC_MAIN_API_URL`), khác nhau ở route `/admin` và layout.
