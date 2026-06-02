# FRONTEND PHASE 10 — Feature Sản phẩm (Site Main) (chi tiết)

> **Phạm vi:** Trang `/products` (danh sách + lọc + phân trang), `/products/[maSP]` (chi tiết), trang chủ `/`. Toàn bộ gọi Site Main qua `mainApiClient` (public, không cần token).
> **Mục tiêu phase:** Xem sản phẩm mượt, lọc theo danh mục/thương hiệu, phân trang đúng cấu trúc `PageResponse`.
> **Nguồn sự thật:** `ProductController.java`, `ProductListItemResponse.java`, `ProductDetailResponse.java`, `CategoryController`, `BrandController`, `common/PageResponse.java`.

---

## 0. Hợp đồng API lấy từ BE

**`GET /api/products`** (public) — query params (theo `ProductController`):
| Param | Kiểu | Mặc định | Ghi chú |
|---|---|---|---|
| `maDanhMuc` | string | — | lọc danh mục |
| `maThuongHieu` | string | — | lọc thương hiệu |
| `trangThai` | boolean | — | lọc trạng thái KD |
| `page` | int | `0` | 0-based |
| `size` | int | `10` | |
| `sort` | string | `ngayTao,desc` | vd `tenSP,asc` |

→ `data` = `PageResponse<ProductListItem>` (`items, page, size, totalElements, totalPages, last`).

**`GET /api/products/{maSP}`** (public) → `ProductDetail`. Mã sai → `404 RESOURCE_NOT_FOUND`.

**`GET /api/categories`**, **`GET /api/brands`** → mảng `Category[]` / `Brand[]` (không phân trang).

---

## Task 10.1 — Hook danh sách & chi tiết sản phẩm

- **Mục tiêu:** `useProducts(params)` + `useProduct(maSP)`.
- **File tạo:** `src/features/products/use-products.ts`
  ```ts
  import { useQuery } from "@tanstack/react-query";
  import { mainApiClient } from "@/lib/main-api-client";
  import { MAIN_ENDPOINTS } from "@/constants/endpoints";
  import { QK } from "@/constants/query-keys";
  import type { PageResponse } from "@/types/api";
  import type { ProductListItem, ProductDetail } from "@/types/domain";

  export interface ProductQuery {
    page?: number; size?: number; sort?: string;
    maDanhMuc?: string; maThuongHieu?: string; trangThai?: boolean;
  }

  export function useProducts(params: ProductQuery) {
    return useQuery({
      queryKey: QK.products(params),
      queryFn: () => mainApiClient.get<PageResponse<ProductListItem>>(
        MAIN_ENDPOINTS.PRODUCTS,
        { query: { size: 12, ...params } },
      ),
      placeholderData: (prev) => prev, // giữ data cũ khi đổi trang (mượt)
    });
  }

  export function useProduct(maSP: string) {
    return useQuery({
      queryKey: QK.product(maSP),
      queryFn: () => mainApiClient.get<ProductDetail>(MAIN_ENDPOINTS.PRODUCT_DETAIL(maSP)),
      enabled: !!maSP,
    });
  }
  ```
- **Kết quả mong muốn:** Đổi trang/lọc không nhấp nháy (nhờ `placeholderData`); chi tiết cache theo `maSP`.

---

## Task 10.2 — Hook dữ liệu tham chiếu (filter)

- **Mục tiêu:** `useCategories`, `useBrands` cho dropdown lọc.
- **File tạo:** `src/features/products/use-reference.ts`
  ```ts
  import { useQuery } from "@tanstack/react-query";
  import { mainApiClient } from "@/lib/main-api-client";
  import { MAIN_ENDPOINTS } from "@/constants/endpoints";
  import { QK } from "@/constants/query-keys";
  import type { Category, Brand } from "@/types/domain";

  export const useCategories = () => useQuery({
    queryKey: QK.categories(),
    queryFn: () => mainApiClient.get<Category[]>(MAIN_ENDPOINTS.CATEGORIES),
    staleTime: Infinity,
  });

  export const useBrands = () => useQuery({
    queryKey: QK.brands(),
    queryFn: () => mainApiClient.get<Brand[]>(MAIN_ENDPOINTS.BRANDS),
    staleTime: Infinity,
  });
  ```
- **Kết quả mong muốn:** Dropdown danh mục/thương hiệu có dữ liệu, cache lâu.

---

## Task 10.3 — Tiện ích format & ảnh

- **Mục tiêu:** Hiển thị giá tiền + ảnh nhất quán.
- **File tạo:** `src/lib/format.ts`
  ```ts
  export const formatVnd = (v: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v);

  // hinhAnh BE là đường dẫn dạng "/images/products/SP001.jpg" trên Site Main
  export const productImageUrl = (hinhAnh?: string) =>
    hinhAnh ? `${process.env.NEXT_PUBLIC_MAIN_API_URL}${hinhAnh}` : "/placeholder.png";
  ```
- **Lưu ý:** `giaBan` nhận `number`; nếu BE trả chuỗi decimal thì `Number(giaBan)` trước khi format.
- **Kết quả mong muốn:** Giá hiển thị `19.990.000 ₫`; ảnh lỗi có placeholder.

---

## Task 10.4 — Trang `/products` (danh sách + lọc + phân trang)

- **Mục tiêu:** Lưới Card + bộ lọc + phân trang.
- **File tạo:**
  - `src/app/products/page.tsx` — đọc filter/page từ `useSearchParams`, gọi `useProducts`.
  - `src/features/products/product-card.tsx` — ảnh, tên, giá, link `/products/[maSP]`.
  - `src/features/products/product-grid.tsx` — grid responsive + skeleton khi loading.
  - `src/features/products/product-filters.tsx` — `Select` danh mục/thương hiệu (dùng `useCategories/useBrands`).
  - `src/features/products/pagination.tsx` — Trước/Sau dựa vào `page`, `last`, `totalPages`.
- **Lưu ý:** giữ filter/page trên URL (`?page=&maDanhMuc=`) để chia sẻ link & back/forward đúng.
- **Kết quả mong muốn:** Lọc + chuyển trang chạy mượt; ảnh + giá đúng; loading có skeleton.

---

## Task 10.5 — Trang `/products/[maSP]` (chi tiết)

- **Mục tiêu:** Hiển thị đầy đủ + nút "Thêm vào giỏ".
- **File tạo:** `src/app/products/[maSP]/page.tsx`, `src/features/products/product-detail.tsx`
  - Dùng `useProduct(maSP)`; render `tenSP, giaBan, donViTinh, moTa?, thongSoKyThuat?, tenDanhMuc, tenThuongHieu`.
  - `moTa`/`thongSoKyThuat` có thể vắng → render điều kiện.
  - Lỗi `404 RESOURCE_NOT_FOUND` (bắt qua `error` của query / `isApiError`) → hiển thị "Không tìm thấy sản phẩm".
  - Nút "Thêm vào giỏ": nếu `!isAuthenticated` → điều hướng `/login`; nếu `region === null` → toast "Tài khoản chưa gán khu vực hợp lệ"; ngược lại gọi `useAddToCart` (Phase 11).
- **Kết quả mong muốn:** Chi tiết hiển thị đúng; mã sai báo không tìm thấy; nút giỏ tôn trọng đăng nhập + vùng.

---

## Task 10.6 — Trang chủ `/`

- **Mục tiêu:** Landing đơn giản.
- **File chỉnh:** `src/app/page.tsx` — banner + vài sản phẩm mới (`useProducts({ size: 8 })`) + CTA "Xem tất cả" → `/products`.
- **Kết quả mong muốn:** Trang chủ gọn, dẫn vào luồng mua hàng.

---

## Checklist hoàn thành Phase 10

- [ ] `use-products.ts` — `useProducts` (query + placeholderData), `useProduct`.
- [ ] `use-reference.ts` — `useCategories`, `useBrands`.
- [ ] `format.ts` — `formatVnd`, `productImageUrl`.
- [ ] `/products` — grid + filter + pagination, đồng bộ URL.
- [ ] `/products/[maSP]` — chi tiết + xử lý 404 + nút thêm giỏ.
- [ ] `/` — landing + sản phẩm nổi bật.
- [ ] Network: mọi request sản phẩm/danh mục/thương hiệu trỏ `:8080`.

> **Tiếp theo:** [Phase 11 — Giỏ hàng](FRONTEND_PHASE11_CART.md)
