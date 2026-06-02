# FRONTEND PHASE 12 — Feature Đơn hàng (Site Bắc/Nam theo vùng) (chi tiết)

> **Phạm vi:** Trang `/orders` (lịch sử + chi tiết) và tạo đơn từ giỏ; gọi Site Bắc/Nam theo `region` qua `getRegionalApiClient`.
> **Mục tiêu phase:** Đặt hàng từ giỏ thành công → xoá giỏ → xem đơn theo đúng vùng user.

---

## 0. ⚠️ Cảnh báo phạm vi

Giống Phase 11: **đơn hàng do Site Bắc/Nam phụ trách**, KHÔNG có trong Site Main. Mọi endpoint dưới đây là **DỰ KIẾN**, cập nhật khi có doc Site Bắc/Nam (`REGIONAL_ENDPOINTS`, type `Order` ở Phase 4).

**Giả định hợp đồng API (dự kiến, cần auth):**
| Method/Path | Mục đích | data |
|---|---|---|
| `GET /api/orders` | Danh sách đơn của user | `Order[]` |
| `GET /api/orders/{id}` | Chi tiết đơn | `Order` |
| `POST /api/orders` | Tạo đơn từ giỏ hiện tại | `Order` |

---

## Task 12.1 — Hook đơn hàng theo vùng

- **Mục tiêu:** `useOrders`, `useOrder(id)`, `useCreateOrder`.
- **File tạo:** `src/features/orders/use-orders.ts`
  ```ts
  import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
  import { useRouter } from "next/navigation";
  import { toast } from "sonner";
  import { useAuth } from "@/features/auth/auth-context";
  import { getRegionalApiClient } from "@/lib/regional-api-client";
  import { REGIONAL_ENDPOINTS } from "@/constants/endpoints";
  import { QK } from "@/constants/query-keys";
  import { isApiError } from "@/lib/api-error";
  import type { Order } from "@/types/regional";

  export function useOrders() {
    const { region, token, isAuthenticated } = useAuth();
    return useQuery({
      queryKey: QK.orders(region ?? "none"),
      enabled: isAuthenticated && !!region,
      queryFn: () => getRegionalApiClient(region!, token).get<Order[]>(REGIONAL_ENDPOINTS.ORDERS),
    });
  }

  export function useOrder(id: string) {
    const { region, token } = useAuth();
    return useQuery({
      queryKey: [...QK.orders(region ?? "none"), id],
      enabled: !!region && !!id,
      queryFn: () => getRegionalApiClient(region!, token).get<Order>(REGIONAL_ENDPOINTS.ORDER_DETAIL(id)),
    });
  }

  export function useCreateOrder() {
    const { region, token } = useAuth();
    const qc = useQueryClient();
    const router = useRouter();
    return useMutation({
      mutationFn: () => getRegionalApiClient(region!, token).post<Order>(REGIONAL_ENDPOINTS.ORDERS),
      onSuccess: () => {
        qc.invalidateQueries({ queryKey: QK.cart(region ?? "none") });   // giỏ đã chuyển thành đơn
        qc.invalidateQueries({ queryKey: QK.orders(region ?? "none") });
        toast.success("Đặt hàng thành công");
        router.push("/orders");
      },
      onError: (e) => toast.error(isApiError(e) ? e.message : "Đặt hàng thất bại"),
    });
  }
  ```
- **Kết quả mong muốn:** Tạo đơn → giỏ + đơn được invalidate → điều hướng `/orders`; dữ liệu đúng vùng.

---

## Task 12.2 — Trang `/orders`

- **Mục tiêu:** Bảng lịch sử đơn + xem chi tiết.
- **File tạo:**
  - `src/app/orders/page.tsx` — bọc `RequireAuth`; dùng `useOrders()`; shadcn `Table` (mã đơn, ngày đặt, trạng thái, tổng tiền).
  - `src/features/orders/order-list.tsx` — render bảng; rỗng → `EmptyState` "Chưa có đơn hàng".
  - `src/features/orders/order-detail.tsx` — Dialog/hoặc route con hiển thị `items` (`useOrder`).
- **Kết quả mong muốn:** Danh sách đơn từ đúng Site Bắc/Nam; trạng thái rỗng có thông báo thân thiện.

---

## Checklist hoàn thành Phase 12

- [ ] `use-orders.ts` — `useOrders`, `useOrder`, `useCreateOrder` (invalidate cart + orders).
- [ ] `/orders` — bảng + chi tiết + empty state.
- [ ] Nút "Đặt hàng" ở `/cart` gọi `useCreateOrder`.
- [ ] Network: đơn của user Bắc → `:8081`, user Nam → `:8082`.
- [ ] (Khi có BE thật) đối chiếu field `Order`/`OrderItem` & path.

> **Tiếp theo:** [Phase 13 — Hoàn thiện & demo](FRONTEND_PHASE13_FINALIZE.md)
