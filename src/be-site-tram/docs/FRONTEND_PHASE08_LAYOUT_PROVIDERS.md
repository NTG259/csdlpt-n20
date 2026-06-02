# FRONTEND PHASE 8 — Layout, Providers & UI chung (chi tiết)

> **Phạm vi:** Bật TanStack Query + AuthProvider + Toaster toàn app; dựng Header điều hướng và các component khung dùng lại.
> **Mục tiêu phase:** Mọi page dùng được `useQuery`/`useMutation`/`useAuth`/toast; có thanh điều hướng hiển thị trạng thái đăng nhập + vùng.

---

## 0. Tiền đề

- Phase 2 đã `add` shadcn `sonner` (toast), `button`, `dropdown-menu`...
- Phase 7 đã có `AuthProvider`, `useAuth`.

---

## Task 8.1 — QueryClient

- **Mục tiêu:** Cấu hình mặc định hợp lý cho data Site Main (ít đổi) và data vùng (đổi thường xuyên).
- **File tạo:** `src/lib/query-client.ts`
  ```ts
  import { QueryClient } from "@tanstack/react-query";
  import { isApiError } from "./api-error";

  export function makeQueryClient() {
    return new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 60_000,          // 1 phút
          refetchOnWindowFocus: false,
          retry: (count, err) => {
            // Đừng retry lỗi nghiệp vụ (4xx) — chỉ retry lỗi mạng/5xx
            if (isApiError(err) && err.status < 500) return false;
            return count < 1;
          },
        },
        mutations: { retry: false },
      },
    });
  }
  ```
- **Kết quả mong muốn:** Lỗi 4xx không bị retry vô ích; dữ liệu tham chiếu cache 1 phút.

---

## Task 8.2 — Providers gốc

- **Mục tiêu:** Gói toàn app: QueryClientProvider → AuthProvider → children + Toaster + Devtools.
- **File tạo:** `src/app/providers.tsx`
  ```tsx
  "use client";
  import { useState } from "react";
  import { QueryClientProvider } from "@tanstack/react-query";
  import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
  import { Toaster } from "@/components/ui/sonner";
  import { AuthProvider } from "@/features/auth/auth-context";
  import { makeQueryClient } from "@/lib/query-client";

  export function Providers({ children }: { children: React.ReactNode }) {
    const [queryClient] = useState(makeQueryClient);
    return (
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          {children}
          <Toaster richColors position="top-right" />
        </AuthProvider>
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    );
  }
  ```
- **File chỉnh:** `src/app/layout.tsx` — bọc `children`:
  ```tsx
  import { Providers } from "./providers";
  // ...trong <body>:
  // <Providers>{children}</Providers>
  ```
- **Kết quả mong muốn:** `useQuery`/`useMutation`/`useAuth`/`toast()` hoạt động ở mọi page.

---

## Task 8.3 — Header điều hướng

- **Mục tiêu:** Thanh trên cùng: logo/CTA + link Sản phẩm/Giỏ hàng/Đơn hàng + trạng thái đăng nhập + nhãn vùng.
- **File tạo:** `src/components/shared/header.tsx`
  ```tsx
  "use client";
  import Link from "next/link";
  import { useAuth } from "@/features/auth/auth-context";
  import { REGION_LABEL } from "@/constants/regions";
  import { Button } from "@/components/ui/button";

  export function Header() {
    const { user, region, isAuthenticated, logout } = useAuth();
    return (
      <header className="border-b">
        <div className="mx-auto flex max-w-6xl items-center justify-between p-4">
          <Link href="/" className="font-bold">SiteMain Shop</Link>
          <nav className="flex items-center gap-4">
            <Link href="/products">Sản phẩm</Link>
            <Link href="/cart">Giỏ hàng</Link>
            <Link href="/orders">Đơn hàng</Link>
            {isAuthenticated ? (
              <span className="flex items-center gap-2 text-sm">
                {user?.hoTen} {region && <em>({REGION_LABEL[region]})</em>}
                <Button variant="outline" size="sm" onClick={logout}>Đăng xuất</Button>
              </span>
            ) : (
              <Link href="/login"><Button size="sm">Đăng nhập</Button></Link>
            )}
          </nav>
        </div>
      </header>
    );
  }
  ```
- **Kết quả mong muốn:** Đã login → hiện tên + vùng (Miền Bắc/Nam) + nút Đăng xuất; chưa login → nút Đăng nhập.

---

## Task 8.4 — Component khung dùng lại

- **Mục tiêu:** Tránh lặp layout/loading/empty.
- **File tạo:**
  - `src/components/shared/page-container.tsx` — wrapper `max-w-6xl mx-auto p-4`.
  - `src/components/shared/loading.tsx` — spinner/skeleton dùng chung.
  - `src/components/shared/empty-state.tsx` — trạng thái rỗng (vd "Chưa có đơn hàng").
- **File chỉnh:** `src/app/layout.tsx` — render `<Header />` phía trên `{children}`.
- **Kết quả mong muốn:** Các page chỉ lo nội dung, dùng lại khung sẵn.

---

## Checklist hoàn thành Phase 8

- [ ] `src/lib/query-client.ts` — `makeQueryClient` (retry thông minh).
- [ ] `src/app/providers.tsx` — Query + Auth + Toaster + Devtools.
- [ ] `src/app/layout.tsx` — bọc `<Providers>` + render `<Header/>`.
- [ ] `src/components/shared/` — `header`, `page-container`, `loading`, `empty-state`.
- [ ] Chạy `bun dev`: Header hiển thị, toast thử nghiệm chạy, Devtools mở được.

> **Tiếp theo:** [Phase 9 — Auth pages](FRONTEND_PHASE09_AUTH_PAGES.md)
