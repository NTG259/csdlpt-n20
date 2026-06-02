# FRONTEND PHASE 11 — Feature Giỏ hàng (Site Bắc/Nam theo vùng) (chi tiết)

> **Phạm vi:** Trang `/cart` thao tác giỏ hàng, gọi **Site Bắc (:8081)** hoặc **Site Nam (:8082)** theo `region` của user qua `getRegionalApiClient`.
> **Mục tiêu phase:** User vùng Bắc thao tác giỏ trên `:8081`, user vùng Nam trên `:8082` — minh hoạ trực tiếp CSDL phân tán.

---

## 0. ⚠️ Cảnh báo phạm vi (rất quan trọng)

Backend trong repo này là **Site Main** — **KHÔNG** chứa API giỏ hàng/đơn hàng (xem `KE_HOACH_TASK_SITE_MAIN.md`: GioHang/ChiTietGioHang/DonHang do **Site Bắc/Nam** giữ ở giai đoạn sau).

Do đó **toàn bộ endpoint trong phase này là DỰ KIẾN**. Khi có tài liệu Site Bắc/Nam, chỉ cần cập nhật:
- Path trong `REGIONAL_ENDPOINTS` (`@/constants/endpoints`, Phase 4).
- Type `Cart`/`CartItem` trong `@/types/regional` (Phase 4).

Phần **định tuyến theo vùng đã hoàn chỉnh** (`getRegionalApiClient`, `useAuth().region`) — feature này chỉ ráp đúng path/field khi BE sẵn sàng.

**Giả định hợp đồng API (dự kiến, cần auth — Bearer token):**
| Method/Path | Mục đích | data trả về |
|---|---|---|
| `GET /api/cart` | Lấy giỏ của user | `Cart` |
| `POST /api/cart/items` | Thêm item `{ maSP, soLuong }` | `Cart` |
| `PATCH /api/cart/items/{maSP}` | Đổi số lượng `{ soLuong }` | `Cart` |
| `DELETE /api/cart/items/{maSP}` | Xoá item | `Cart` |

> Cấu trúc response vẫn là `ApiResponse<Cart>` như Site Main (giả định đồng nhất toàn hệ thống).

---

## Task 11.1 — Hook giỏ hàng theo vùng

- **Mục tiêu:** `useCart`, `useAddToCart`, `useUpdateCartItem`, `useRemoveCartItem` — tự lấy `region` + `token` từ `useAuth()`.
- **File tạo:** `src/features/cart/use-cart.ts`
  ```ts
  import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
  import { toast } from "sonner";
  import { useAuth } from "@/features/auth/auth-context";
  import { getRegionalApiClient } from "@/lib/regional-api-client";
  import { REGIONAL_ENDPOINTS } from "@/constants/endpoints";
  import { QK } from "@/constants/query-keys";
  import { isApiError } from "@/lib/api-error";
  import type { Cart } from "@/types/regional";

  export function useCart() {
    const { region, token, isAuthenticated } = useAuth();
    return useQuery({
      queryKey: QK.cart(region ?? "none"),
      enabled: isAuthenticated && !!region,   // chưa login / chưa có vùng → không gọi
      queryFn: () => getRegionalApiClient(region!, token).get<Cart>(REGIONAL_ENDPOINTS.CART),
    });
  }

  export function useAddToCart() {
    const { region, token } = useAuth();
    const qc = useQueryClient();
    return useMutation({
      mutationFn: (input: { maSP: string; soLuong: number }) =>
        getRegionalApiClient(region!, token).post<Cart>(REGIONAL_ENDPOINTS.CART + "/items", input),
      onSuccess: (cart) => {
        qc.setQueryData(QK.cart(region ?? "none"), cart);
        toast.success("Đã thêm vào giỏ");
      },
      onError: (e) => toast.error(isApiError(e) ? e.message : "Không thêm được"),
    });
  }

  export function useUpdateCartItem() {
    const { region, token } = useAuth();
    const qc = useQueryClient();
    return useMutation({
      mutationFn: (input: { maSP: string; soLuong: number }) =>
        getRegionalApiClient(region!, token).patch<Cart>(REGIONAL_ENDPOINTS.CART_ITEM(input.maSP), { soLuong: input.soLuong }),
      onSuccess: (cart) => qc.setQueryData(QK.cart(region ?? "none"), cart),
    });
  }

  export function useRemoveCartItem() {
    const { region, token } = useAuth();
    const qc = useQueryClient();
    return useMutation({
      mutationFn: (maSP: string) =>
        getRegionalApiClient(region!, token).delete<Cart>(REGIONAL_ENDPOINTS.CART_ITEM(maSP)),
      onSuccess: () => qc.invalidateQueries({ queryKey: QK.cart(region ?? "none") }),
    });
  }
  ```
- **Lưu ý phân tán:** `queryKey` gắn `region` ⇒ cache giỏ Bắc & Nam tách biệt; đổi user khác vùng không lẫn dữ liệu.
- **Kết quả mong muốn:** Thao tác giỏ của user Bắc đi `:8081`, user Nam đi `:8082`.

---

## Task 11.2 — Trang `/cart`

- **Mục tiêu:** Liệt kê item, đổi số lượng, xoá, tổng tiền, nút "Đặt hàng".
- **File tạo:**
  - `src/app/cart/page.tsx` — bọc `RequireAuth` (Phase 13); dùng `useCart()`.
  - `src/features/cart/cart-item-row.tsx` — ảnh, tên, đơn giá, ô số lượng (gọi `useUpdateCartItem`), nút xoá (`useRemoveCartItem`).
  - `src/features/cart/cart-summary.tsx` — tổng tiền (`formatVnd`), nút "Đặt hàng" → gọi `useCreateOrder` (Phase 12).
- **Trạng thái cần xử lý:**
  - Chưa đăng nhập → `RequireAuth` đẩy về `/login`.
  - `region === null` → thông báo "Tài khoản chưa gán khu vực hợp lệ".
  - Giỏ rỗng → `EmptyState` "Giỏ hàng trống" + link `/products`.
- **Kết quả mong muốn:** Giỏ phản ánh đúng dữ liệu vùng; đổi số lượng cập nhật tổng tiền tức thì.

---

## Checklist hoàn thành Phase 11

- [ ] `REGIONAL_ENDPOINTS.CART*` đã khai (dự kiến) ở Phase 4.
- [ ] `use-cart.ts` — query + 3 mutation, `enabled` theo login & region, cache theo vùng.
- [ ] `/cart` — danh sách + sửa số lượng + xoá + tổng tiền + nút đặt hàng.
- [ ] Network: user Bắc → `:8081`, user Nam → `:8082`.
- [ ] (Khi có BE thật) đối chiếu lại field `Cart`/`CartItem` & path.

> **Tiếp theo:** [Phase 12 — Đơn hàng](FRONTEND_PHASE12_ORDERS.md)
