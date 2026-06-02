# FRONTEND PHASE 11 — Feature Giỏ hàng (chi tiết)

> **Phạm vi:** Trang `/cart` — xem giỏ, đổi số lượng, xoá, và **kiểm soát đặt hàng theo tồn kho khả dụng** (`soLuongKhaDung`).
> **Nguồn API:** Giỏ hàng gọi **Site Bắc/Site Nam theo khu vực user** (`BAC -> :8081`, `NAM -> :8082`) qua `getRegionalApiClient(region, token)`. Shape request/response bám theo [`FRONTEND_CART_API.md`](FRONTEND_CART_API.md).

---

## 0. Trạng thái API & định tuyến regional

Phase này **không gọi Site Main** cho giỏ hàng. FE lấy `region` từ `AuthProvider`:

- `region === "BAC"` -> `NEXT_PUBLIC_NORTH_API_URL` (`:8081`)
- `region === "NAM"` -> `NEXT_PUBLIC_SOUTH_API_URL` (`:8082`)

Tất cả request cart đều kèm Bearer token và dùng cùng path `/api/cart*`.

| Method/Path | Mục đích | data trả về |
|---|---|---|
| `GET /api/cart` | Lấy giỏ của user | `GioHang` |
| `POST /api/cart/items` | Thêm `{ maSP, soLuong }` (cộng dồn) | `GioHang` |
| `PUT /api/cart/items/{maSP}` | Đặt lại số lượng `{ soLuong }` (ghi đè) | `GioHang` |
| `DELETE /api/cart/items/{maSP}` | Xoá 1 dòng | `null` |
| `DELETE /api/cart` | Xoá toàn bộ giỏ | `null` |

> ⚠️ **Lưu ý so với bản cũ:** update dùng **`PUT`** (không phải `PATCH`); 2 endpoint `DELETE` trả **`null`** ⇒ phải `invalidate` rồi refetch chứ không `setQueryData` từ response. Tất cả endpoint **cần Bearer token**.
>
> **Cache:** dùng `QK.cart(region)` để tách cache giỏ hàng Bắc/Nam, tránh dùng nhầm dữ liệu giữa 2 site.

---

## 1. Mô hình dữ liệu & quy tắc tồn kho (cốt lõi của phase)

Backend trả về giỏ đã **tách sẵn 2 nhóm** và kèm `soLuongKhaDung` cho mỗi dòng:

```ts
// src/types/cart.ts
export interface ChiTietGioHang {
  maSP: string;
  tenSP: string;
  hinhAnh: string;
  donViTinh: string;
  soLuong: number;        // số lượng user để trong giỏ
  giaBan: number;
  thanhTien: number;      // giaBan * soLuong
  soLuongKhaDung: number; // tồn kho khả dụng hiện tại
}

export interface GioHang {
  sanPhamHopLe: ChiTietGioHang[];   // soLuongKhaDung > 0
  sanPhamHetHang: ChiTietGioHang[]; // soLuongKhaDung === 0
  tongSoLuong: number;              // CHỈ tính sanPhamHopLe
  tongTien: number;                 // CHỈ tính sanPhamHopLe
}
```

**3 trạng thái mỗi dòng** (đây là thứ điều khiển toàn bộ UI):

| Trạng thái | Điều kiện | Có tính vào đơn? | Hành động cho phép |
|---|---|---|---|
| `ok` | `0 < soLuong ≤ soLuongKhaDung` | ✅ | Tăng/giảm (tối đa `soLuongKhaDung`), xoá |
| `vuot_ton` (vượt tồn) | nằm trong `sanPhamHopLe` **nhưng** `soLuong > soLuongKhaDung` | ⚠️ **chặn đặt hàng** | "Cập nhật về N", giảm số lượng, xoá |
| `het_hang` | nằm trong `sanPhamHetHang` (`soLuongKhaDung === 0`) | ❌ (BE đã loại khỏi tổng) | **Chỉ** xoá |

> 🔑 **Mấu chốt:** `vuot_ton` rất nguy hiểm vì BE vẫn tính `thanhTien` theo `soLuong` đầy đủ và xếp dòng đó vào `sanPhamHopLe` (tức vẫn vào `tongTien`). FE **phải tự phát hiện** và chặn checkout cho tới khi user sửa.

### Helper trạng thái & validate giỏ

```ts
// src/features/cart/cart-rules.ts
import type { GioHang, ChiTietGioHang } from "@/types/cart";

export type CartLineStatus = "ok" | "vuot_ton" | "het_hang";

export function getLineStatus(item: ChiTietGioHang): CartLineStatus {
  if (item.soLuongKhaDung <= 0) return "het_hang";
  if (item.soLuong > item.soLuongKhaDung) return "vuot_ton";
  return "ok";
}

export interface CartValidation {
  hasOrderableItems: boolean;        // có ít nhất 1 dòng còn hàng
  overStockItems: ChiTietGioHang[];  // dòng vuot_ton (cần sửa trước khi đặt)
  hasHetHang: boolean;               // có dòng hết hàng (chỉ để cảnh báo)
  canCheckout: boolean;              // điều kiện bật nút "Đặt hàng"
  blockReason?: string;              // lý do chặn (hiện trên tooltip / dưới nút)
}

export function validateCart(cart?: GioHang): CartValidation {
  const hopLe = cart?.sanPhamHopLe ?? [];
  const hetHang = cart?.sanPhamHetHang ?? [];
  const overStock = hopLe.filter((i) => i.soLuong > i.soLuongKhaDung);
  const hasOrderableItems = hopLe.length > 0;

  let blockReason: string | undefined;
  if (!hasOrderableItems) blockReason = "Giỏ chưa có sản phẩm còn hàng để đặt.";
  else if (overStock.length > 0)
    blockReason = `Có ${overStock.length} sản phẩm vượt quá tồn kho. Vui lòng giảm số lượng trước khi đặt.`;

  return {
    hasOrderableItems,
    overStockItems: overStock,
    hasHetHang: hetHang.length > 0,
    canCheckout: hasOrderableItems && overStock.length === 0,
    blockReason,
  };
}
```

---

## Task 11.1 — Hook giỏ hàng (`use-cart.ts`)

- **Mục tiêu:** `useCart`, `useAddToCart`, `useUpdateCartItem`, `useRemoveCartItem`, `useClearCart` — gọi `getRegionalApiClient(region, token)` từ `useAuth()`, không gọi `mainApiClient`.
- **File tạo:** `src/features/cart/use-cart.ts`

```ts
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuth } from "@/features/auth/auth-context";
import { getRegionalApiClient } from "@/lib/regional-api-client";
import { QK } from "@/constants/query-keys";
import { isApiError } from "@/lib/api-error";
import type { GioHang } from "@/types/cart";

const CART = "/api/cart";

export function useCart() {
  const { token, region, isAuthenticated } = useAuth();
  return useQuery({
    queryKey: QK.cart(region ?? "unknown"),
    enabled: isAuthenticated && !!token && !!region,
    queryFn: () => getRegionalApiClient(region!, token).get<GioHang>(CART),
  });
}

export function useAddToCart() {
  const { token, region } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: { maSP: string; soLuong: number }) =>
      getRegionalApiClient(region!, token).post<GioHang>(`${CART}/items`, input),
    // POST trả về GioHang mới → cập nhật cache luôn
    onSuccess: (cart) => {
      qc.setQueryData(QK.cart(region!), cart);
      toast.success("Đã thêm vào giỏ");
    },
    onError: (e) => toast.error(isApiError(e) ? e.message : "Không thêm được"),
  });
}

export function useUpdateCartItem() {
  const { token, region } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    // PUT — ghi đè số lượng (không cộng dồn)
    mutationFn: (input: { maSP: string; soLuong: number }) =>
      getRegionalApiClient(region!, token).put<GioHang>(`${CART}/items/${input.maSP}`, {
        soLuong: input.soLuong,
      }),
    onSuccess: (cart) => qc.setQueryData(QK.cart(region!), cart),
    onError: (e) => toast.error(isApiError(e) ? e.message : "Không cập nhật được"),
  });
}

export function useRemoveCartItem() {
  const { token, region } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    // DELETE trả null → KHÔNG setQueryData, phải invalidate để refetch
    mutationFn: (maSP: string) =>
      getRegionalApiClient(region!, token).delete<void>(`${CART}/items/${maSP}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.cart(region!) }),
    onError: (e) => toast.error(isApiError(e) ? e.message : "Không xoá được"),
  });
}

export function useClearCart() {
  const { token, region } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => getRegionalApiClient(region!, token).delete<void>(CART),
    onSuccess: () => qc.invalidateQueries({ queryKey: QK.cart(region!) }),
  });
}
```

- **Ghi nhớ:** `POST`/`PUT` trả `GioHang` → `setQueryData`; `DELETE` trả `null` → `invalidateQueries` (xem [`FRONTEND_CART_API.md` §4.4–4.5](FRONTEND_CART_API.md)).
- **`QK.cart(region)`** khai ở `@/constants/query-keys` để tách cache theo Site Bắc/Nam.

---

## Task 11.2 — Dòng sản phẩm (`cart-item-row.tsx`)

- **Mục tiêu:** mỗi dòng tự render theo `getLineStatus` và **không cho vượt `soLuongKhaDung`**.
- **File tạo:** `src/features/cart/cart-item-row.tsx`

**Quy tắc UI theo trạng thái:**

- **`ok`:** stepper bình thường. **Disable nút `+`** khi `soLuong >= soLuongKhaDung`. Nhập tay > `soLuongKhaDung` → **kẹp (clamp)** về `soLuongKhaDung` + toast "Chỉ còn N sản phẩm". Hiện nhãn nhỏ "Còn N" khi tồn thấp (vd `soLuongKhaDung <= 5`).
- **`vuot_ton`:** viền/nền cảnh báo (vàng), badge "Vượt tồn — chỉ còn N". Nút **"Cập nhật về N"** gọi `useUpdateCartItem({ maSP, soLuong: soLuongKhaDung })`. Stepper vẫn cho giảm.
- **`het_hang`:** làm mờ (opacity ~60%), badge đỏ "Hết hàng", **ẩn/disable stepper**, chỉ còn nút "Xoá".

**Debounce cập nhật số lượng:** stepper/input cập nhật UI ngay bằng local state, nhưng chỉ gọi `PUT /api/cart/items/{maSP}` sau khi user ngừng thay đổi khoảng `650ms`. Trong lúc chờ hiện "Sẽ cập nhật sau giây lát"; khi request đang chạy hiện spinner + "Đang cập nhật...". Nút "Cập nhật về N" và "Xóa" vẫn gọi API ngay vì là hành động rõ ràng.

```tsx
"use client";
import { toast } from "sonner";
import { useUpdateCartItem, useRemoveCartItem } from "./use-cart";
import { getLineStatus } from "./cart-rules";
import { formatVnd } from "@/lib/format";
import type { ChiTietGioHang } from "@/types/cart";

export function CartItemRow({ item }: { item: ChiTietGioHang }) {
  const update = useUpdateCartItem();
  const remove = useRemoveCartItem();
  const status = getLineStatus(item);
  const max = item.soLuongKhaDung;

  const setSoLuong = (next: number) => {
    if (next < 1) return;                    // <1 thì dùng nút Xoá
    if (max > 0 && next > max) {             // chặn vượt tồn ngay tại FE
      toast.warning(`Chỉ còn ${max} ${item.donViTinh}`);
      next = max;
    }
    if (next === item.soLuong) return;
    update.mutate({ maSP: item.maSP, soLuong: next });
  };

  return (
    <div data-status={status} className="flex items-center gap-3 py-3">
      {/* ảnh + tên + đơn giá ... */}
      <div className="flex-1">
        <p className="font-medium">{item.tenSP}</p>
        <p className="text-sm text-muted-foreground">{formatVnd(item.giaBan)} / {item.donViTinh}</p>

        {status === "vuot_ton" && (
          <button
            className="text-sm text-amber-600 underline"
            onClick={() => update.mutate({ maSP: item.maSP, soLuong: max })}
          >
            Vượt tồn — chỉ còn {max}. Cập nhật về {max}
          </button>
        )}
        {status === "het_hang" && (
          <span className="text-sm font-medium text-destructive">Hết hàng</span>
        )}
        {status === "ok" && max <= 5 && (
          <span className="text-sm text-muted-foreground">Còn {max}</span>
        )}
      </div>

      {/* Stepper — vô hiệu khi hết hàng */}
      {status !== "het_hang" && (
        <div className="flex items-center gap-2">
          <button onClick={() => setSoLuong(item.soLuong - 1)} disabled={update.isPending}>−</button>
          <span className="w-8 text-center">{item.soLuong}</span>
          <button
            onClick={() => setSoLuong(item.soLuong + 1)}
            disabled={update.isPending || item.soLuong >= max}   // chặn vượt tồn
          >
            +
          </button>
        </div>
      )}

      <div className="w-28 text-right">{formatVnd(item.thanhTien)}</div>
      <button onClick={() => remove.mutate(item.maSP)} disabled={remove.isPending}>Xoá</button>
    </div>
  );
}
```

- **Kết quả mong muốn:** không thể tăng số lượng quá tồn; dòng vượt tồn có lối sửa nhanh; dòng hết hàng chỉ xoá được.

---

## Task 11.3 — Tóm tắt giỏ & nút Đặt hàng (`cart-summary.tsx`)

- **Mục tiêu:** dùng `validateCart` để **bật/tắt nút "Đặt hàng"** và nêu rõ lý do khi bị chặn.
- **File tạo:** `src/features/cart/cart-summary.tsx`

```tsx
"use client";
import { validateCart } from "./cart-rules";
import { formatVnd } from "@/lib/format";
import type { GioHang } from "@/types/cart";
// import { useCreateOrder } from "@/features/orders/use-orders"; // Phase 12

export function CartSummary({ cart }: { cart: GioHang }) {
  const v = validateCart(cart);
  // const createOrder = useCreateOrder();

  return (
    <aside className="space-y-3 rounded-lg border p-4">
      <div className="flex justify-between">
        <span>Số lượng (còn hàng)</span>
        <span>{cart.tongSoLuong}</span>
      </div>
      <div className="flex justify-between text-lg font-semibold">
        <span>Tổng tiền</span>
        <span>{formatVnd(cart.tongTien)}</span>
      </div>

      {v.hasHetHang && (
        <p className="text-sm text-muted-foreground">
          * Sản phẩm hết hàng không được tính vào tổng và sẽ không được đặt.
        </p>
      )}
      {!v.canCheckout && v.blockReason && (
        <p className="text-sm text-destructive">{v.blockReason}</p>
      )}

      <button
        className="w-full"
        disabled={!v.canCheckout /* || createOrder.isPending */}
        title={v.canCheckout ? undefined : v.blockReason}
        onClick={() => {
          /* createOrder.mutate(...) — Phase 12, chỉ gửi sanPhamHopLe hợp lệ */
        }}
      >
        Đặt hàng
      </button>
    </aside>
  );
}
```

**Điều kiện bật nút "Đặt hàng" (`canCheckout`):**
1. `sanPhamHopLe.length > 0` — có ít nhất 1 sản phẩm còn hàng.
2. **Không** còn dòng `vuot_ton` (mọi dòng hợp lệ phải có `soLuong ≤ soLuongKhaDung`).

Nếu một trong hai sai → **disable nút** + hiện `blockReason`. Sản phẩm `het_hang` **không chặn** đặt hàng (BE đã loại khỏi tổng) nhưng vẫn cảnh báo để user tự xoá.

> 🛡️ **Phòng tuyến nhiều lớp:** (1) stepper chặn tăng quá tồn; (2) `canCheckout` chặn submit; (3) Phase 12 vẫn nên xử lý lỗi tồn kho từ BE khi tạo đơn (tồn có thể đổi giữa lúc xem giỏ và lúc đặt) → nếu BE trả lỗi, refetch `useCart()` để đồng bộ lại `soLuongKhaDung`.

---

## Task 11.4 — Trang `/cart` (`app/cart/page.tsx`)

- **Mục tiêu:** ráp danh sách + summary, xử lý đủ các trạng thái rỗng/lỗi.
- **File tạo:** `src/app/cart/page.tsx` — bọc `RequireAuth` (Phase 13), dùng `useCart()`.

**Trạng thái cần xử lý:**
- Chưa đăng nhập → `RequireAuth` đẩy về `/login`.
- `isLoading` → skeleton danh sách.
- `isError` → thông báo lỗi + nút "Thử lại" (`refetch`).
- Giỏ rỗng (`sanPhamHopLe` & `sanPhamHetHang` đều rỗng) → `EmptyState` "Giỏ hàng trống" + link `/products`.
- Có hàng: render `sanPhamHopLe` trước, rồi khối **"Sản phẩm hết hàng"** (`sanPhamHetHang`) tách riêng phía dưới + nút "Xoá tất cả hết hàng" (lặp `useRemoveCartItem`); cột phải là `CartSummary`.

```tsx
"use client";
import { useCart } from "@/features/cart/use-cart";
import { CartItemRow } from "@/features/cart/cart-item-row";
import { CartSummary } from "@/features/cart/cart-summary";

export default function CartPage() {
  const { data: cart, isLoading, isError, refetch } = useCart();

  if (isLoading) return <CartSkeleton />;
  if (isError) return <ErrorState onRetry={() => refetch()} />;
  if (!cart || (cart.sanPhamHopLe.length === 0 && cart.sanPhamHetHang.length === 0))
    return <EmptyState />;

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <div>
        {cart.sanPhamHopLe.map((it) => <CartItemRow key={it.maSP} item={it} />)}

        {cart.sanPhamHetHang.length > 0 && (
          <section className="mt-6 opacity-80">
            <h3 className="text-sm font-semibold text-destructive">Sản phẩm hết hàng</h3>
            {cart.sanPhamHetHang.map((it) => <CartItemRow key={it.maSP} item={it} />)}
          </section>
        )}
      </div>
      <CartSummary cart={cart} />
    </div>
  );
}
```

---

## Checklist hoàn thành Phase 11

- [x] `src/types/cart.ts` — `GioHang` / `ChiTietGioHang` khớp [`FRONTEND_CART_API.md`](FRONTEND_CART_API.md) (có `soLuongKhaDung`, tách `sanPhamHopLe`/`sanPhamHetHang`).
- [x] `cart-rules.ts` — `getLineStatus` + `validateCart`.
- [x] `use-cart.ts` — query + 4 mutation qua `getRegionalApiClient(region, token)`; **`PUT`** cho update; `DELETE` dùng `invalidate` vì trả `null`.
- [x] `cart-item-row.tsx` — debounce update số lượng; chặn `+` khi `soLuong >= soLuongKhaDung`; clamp khi nhập tay; nút "Cập nhật về N" cho `vuot_ton`; hết hàng chỉ cho xoá; có trạng thái loading theo dòng.
- [x] `cart-summary.tsx` — nút "Đặt hàng" **disable khi `!canCheckout`** + hiện `blockReason`.
- [x] `/cart` — đủ trạng thái loading / error / rỗng; tách khối hết hàng.
- [x] Nút thêm sản phẩm ở `/`, `/products`, `/products/[maSP]` gọi `useAddToCart()` theo Site Bắc/Nam.
- [x] Build checks: `bunx tsc --noEmit`, `bun run lint`, `bun run build`.
- [ ] Network/E2E: user Bắc gọi `:8081`, user Nam gọi `:8082` khi backend regional đang chạy.

> **Tiếp theo:** [Phase 12 — Đơn hàng](FRONTEND_PHASE12_ORDERS.md) — khi tạo đơn, chỉ gửi `sanPhamHopLe` hợp lệ và xử lý lỗi tồn kho trả về từ BE.
