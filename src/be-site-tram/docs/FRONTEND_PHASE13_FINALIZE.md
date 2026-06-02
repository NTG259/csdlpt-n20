# FRONTEND PHASE 13 — Hoàn thiện & chuẩn bị demo (chi tiết)

> **Phạm vi:** Bảo vệ route, trạng thái lỗi/loading toàn cục, và README hướng dẫn chạy demo phân tán.
> **Mục tiêu phase:** App sẵn sàng demo đồ án CSDL phân tán: mượt, không màn hình trắng, người chấm chạy được trong 1 phút.

---

## Task 13.1 — Bảo vệ route cần đăng nhập (`RequireAuth`)

- **Mục tiêu:** `/cart`, `/orders` chỉ cho user đã đăng nhập.
- **File tạo:** `src/components/shared/require-auth.tsx`
  ```tsx
  "use client";
  import { useEffect, useState } from "react";
  import { useRouter } from "next/navigation";
  import { useAuth } from "@/features/auth/auth-context";
  import { Loading } from "./loading";

  export function RequireAuth({ children }: { children: React.ReactNode }) {
    const { isAuthenticated } = useAuth();
    const router = useRouter();
    const [checked, setChecked] = useState(false);

    useEffect(() => {
      // AuthProvider khôi phục session trong useEffect → chờ 1 tick rồi mới quyết định
      if (!isAuthenticated) router.replace("/login");
      setChecked(true);
    }, [isAuthenticated, router]);

    if (!checked || !isAuthenticated) return <Loading />;
    return <>{children}</>;
  }
  ```
- **Dùng:** bọc nội dung `app/cart/page.tsx`, `app/orders/page.tsx`.
- **Kết quả mong muốn:** Vào thẳng `/cart` khi chưa login → tự về `/login`.
- **Liên kết:** kết hợp với handler 401 ở Phase 5/7 (token hết hạn cũng đẩy về login).

---

## Task 13.2 — Lỗi & loading toàn cục

- **Mục tiêu:** UX nhất quán, không màn hình trắng.
- **File tạo:**
  - `src/app/error.tsx` — error boundary route ("Đã có lỗi xảy ra" + nút thử lại `reset()`).
  - `src/app/not-found.tsx` — 404 thân thiện + link về `/`.
  - `src/app/loading.tsx` — loading cấp route (skeleton).
  - Dùng `<Skeleton>` (shadcn) ở `product-grid`, `product-detail`, `order-list`.
- **Kết quả mong muốn:** Lỗi mạng/API hiển thị `message` thân thiện; chuyển trang có skeleton.

---

## Task 13.3 — README chạy demo

- **Mục tiêu:** Hướng dẫn dựng + kịch bản demo phân tán.
- **File chỉnh:** `sitemain-fe/README.md` — nội dung:
  ```md
  ## Yêu cầu
  - Bun, Node 18+
  - 3 backend chạy: Site Main :8080, Site Bắc :8081, Site Nam :8082

  ## Chạy FE
  1. cp .env.example .env.local   # (Windows: copy)
  2. bun install
  3. bun dev                      # http://localhost:3000

  ## Kịch bản demo CSDL phân tán
  1. Mở /products — dữ liệu sản phẩm từ Site Main (:8080).
  2. Đăng ký user A, chọn khu vực "Bac".
  3. Đăng ký user B, chọn khu vực "Nam".
  4. Đăng nhập user A → vào /cart, /orders → mở DevTools > Network:
     các request giỏ/đơn trỏ http://localhost:8081 (Site Bắc).
  5. Đăng nhập user B → request giỏ/đơn trỏ http://localhost:8082 (Site Nam).
  → Chứng minh cùng một FE định tuyến tới CSDL theo khu vực người dùng.
  ```
- **Lưu ý:** BE đã cấu hình CORS cho `http://localhost:3000` (xem `SecurityConfig`) nên FE gọi trực tiếp được, không cần proxy.
- **Kết quả mong muốn:** Làm theo README dựng được toàn bộ luồng demo.

---

## Task 13.4 — Rà soát cuối (tuỳ chọn nhưng nên làm)

- `bun run build` không lỗi type/lint.
- Kiểm tra mọi gọi sản phẩm/danh mục/thương hiệu/khu vực/auth → `:8080`.
- Kiểm tra giỏ/đơn theo vùng → `:8081`/`:8082`.
- Xoá `console.log` thừa; đảm bảo không lộ token ra UI.

---

## Checklist nghiệm thu toàn dự án (FE)

- [ ] `bun dev` chạy, không lỗi console.
- [ ] `/products`: lọc + phân trang OK (gọi `:8080`).
- [ ] Đăng ký + đăng nhập OK; reload vẫn giữ phiên; logout dọn sạch.
- [ ] User vùng **Bac**: `/cart`, `/orders` gọi `:8081`.
- [ ] User vùng **Nam**: `/cart`, `/orders` gọi `:8082`.
- [ ] Chưa login vào `/cart` → về `/login`; token hết hạn → tự logout.
- [ ] Lỗi API hiển thị `message` tiếng Việt, không crash.
- [ ] README demo đầy đủ, chạy lại được từ máy trắng.

> **Quay lại:** [Mục lục các phase — FRONTEND_SETUP_TASKS.md](FRONTEND_SETUP_TASKS.md)
