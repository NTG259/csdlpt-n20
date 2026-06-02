# FRONTEND PHASE 7 — Auth state: token + user + chọn vùng (chi tiết)

> **Phạm vi:** Lưu phiên đăng nhập (token + user) vào `localStorage`, suy ra vùng từ `maKhuVuc`, và cung cấp state qua React Context (KHÔNG Redux/Zustand/NextAuth).
> **Mục tiêu phase:** Bất kỳ component nào gọi `useAuth()` lấy được `{ user, token, region, isAuthenticated, login, logout }`. Reload trang vẫn giữ đăng nhập.
> **Nguồn sự thật:** `dto/response/AuthResponse.java` (login & register cùng trả `AuthResponse`), giá trị `maKhuVuc = "Bac"/"Nam"`.

---

## 0. Tiền đề & bối cảnh quan trọng

- `register` và `login` **đều trả `AuthResponse`** (xem `AuthController`: register → 201, login → 200) ⇒ FE có đủ thông tin user ngay sau khi đăng ký/đăng nhập, **không cần** gọi thêm `/users/me`.
- ⚠️ **Backend hiện CHƯA có endpoint lấy profile** (`/api/users/me`). Vì vậy "thông tin user" = đúng các field trong `AuthResponse` đã lưu lúc login. Không phát sinh request profile.
- Token sống 24h (`expiresIn = 86400000` ms). Có thể lưu kèm thời điểm hết hạn để chủ động ẩn UI khi quá hạn.

---

## Task 7.1 — Lưu/đọc/xoá phiên (`auth-storage.ts`)

- **Mục tiêu:** Persist phiên qua `localStorage` dưới 1 key duy nhất.
- **File tạo:** `src/lib/auth-storage.ts`
  ```ts
  import type { AuthResponse } from "@/types/domain";

  const KEY = "sitemain_session";

  // Tách user khỏi token cho gọn (user = AuthResponse trừ token)
  export interface StoredSession {
    token: string;
    expiresAt: number;          // epoch ms = Date.now() + expiresIn
    user: Omit<AuthResponse, "token" | "tokenType" | "expiresIn">;
  }

  export function saveSession(auth: AuthResponse): StoredSession {
    const session: StoredSession = {
      token: auth.token,
      expiresAt: Date.now() + auth.expiresIn,
      user: {
        userId: auth.userId,
        hoTen: auth.hoTen,
        email: auth.email,
        maKhuVuc: auth.maKhuVuc,
        vaiTro: auth.vaiTro,
      },
    };
    localStorage.setItem(KEY, JSON.stringify(session));
    return session;
  }

  export function getSession(): StoredSession | null {
    if (typeof window === "undefined") return null; // an toàn SSR
    const raw = localStorage.getItem(KEY);
    if (!raw) return null;
    try {
      const s = JSON.parse(raw) as StoredSession;
      if (s.expiresAt && s.expiresAt < Date.now()) { clearSession(); return null; }
      return s;
    } catch { return null; }
  }

  export function clearSession() {
    localStorage.removeItem(KEY);
  }
  ```
- **Lưu ý SSR:** Next.js render server trước → mọi truy cập `localStorage` phải guard `typeof window`.
- **Kết quả mong muốn:** Reload trang vẫn đăng nhập; token quá `expiresAt` tự bị xoá.

---

## Task 7.2 — Suy ra vùng từ user (`region.ts`)

- **Mục tiêu:** `maKhuVuc` ("Bac"/"Nam") → `RegionCode` ("BAC"/"NAM").
- **File tạo:** `src/features/auth/region.ts`
  ```ts
  import { KHU_VUC_TO_REGION, type RegionCode } from "@/constants/regions";

  export function getUserRegion(maKhuVuc?: string): RegionCode | null {
    if (!maKhuVuc) return null;
    return KHU_VUC_TO_REGION[maKhuVuc.trim().toLowerCase()] ?? null;
  }
  ```
- **Kết quả mong muốn:** `getUserRegion("Bac")` → `"BAC"`, `getUserRegion("Nam")` → `"NAM"`, `getUserRegion("XYZ")` → `null`.
- **Vì sao chuẩn hoá `toLowerCase()`:** an toàn nếu BE trả `"Bac"`, `"BAC"`, hay `"bac"`.

---

## Task 7.3 — `AuthProvider` + `useAuth` (React Context)

- **Mục tiêu:** State đăng nhập toàn cục, gọn nhẹ.
- **File tạo:** `src/features/auth/auth-context.tsx`
  ```tsx
  "use client";
  import { createContext, useContext, useEffect, useMemo, useState, useCallback } from "react";
  import { useRouter } from "next/navigation";
  import type { AuthResponse } from "@/types/domain";
  import { getSession, saveSession, clearSession, type StoredSession } from "@/lib/auth-storage";
  import { getUserRegion } from "./region";
  import { setUnauthorizedHandler } from "@/lib/auth-events";
  import type { RegionCode } from "@/constants/regions";

  interface AuthContextValue {
    user: StoredSession["user"] | null;
    token: string | null;
    region: RegionCode | null;
    isAuthenticated: boolean;
    login: (auth: AuthResponse) => void;
    logout: () => void;
  }

  const AuthContext = createContext<AuthContextValue | null>(null);

  export function AuthProvider({ children }: { children: React.ReactNode }) {
    const router = useRouter();
    const [session, setSession] = useState<StoredSession | null>(null);

    // Khôi phục phiên sau khi mount (tránh lệch SSR/CSR)
    useEffect(() => { setSession(getSession()); }, []);

    const logout = useCallback(() => {
      clearSession();
      setSession(null);
      router.replace("/login");
    }, [router]);

    // Đăng ký handler 401 từ apiFetch (Phase 5)
    useEffect(() => {
      setUnauthorizedHandler(() => logout());
      return () => setUnauthorizedHandler(null);
    }, [logout]);

    const login = useCallback((auth: AuthResponse) => {
      setSession(saveSession(auth));
    }, []);

    const value = useMemo<AuthContextValue>(() => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      region: getUserRegion(session?.user.maKhuVuc),
      isAuthenticated: !!session?.token,
      login,
      logout,
    }), [session, login, logout]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
  }

  export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth phải dùng bên trong <AuthProvider>");
    return ctx;
  }
  ```
- **Lưu ý:**
  - Khôi phục phiên trong `useEffect` (sau mount) để tránh hydration mismatch; trong lúc chưa mount coi như chưa đăng nhập.
  - `region` được tính từ `maKhuVuc` mỗi lần session đổi.
- **Kết quả mong muốn:** `const { user, token, region, isAuthenticated } = useAuth()` chạy ở mọi client component; `logout()` dọn phiên + về `/login`.

---

## Checklist hoàn thành Phase 7

- [ ] `src/lib/auth-storage.ts` — `saveSession/getSession/clearSession`, có `expiresAt` + guard SSR.
- [ ] `src/features/auth/region.ts` — `getUserRegion` (Bac→BAC, Nam→NAM).
- [ ] `src/features/auth/auth-context.tsx` — `AuthProvider`, `useAuth`, nối `setUnauthorizedHandler`.
- [ ] Test tay: gán session giả → reload vẫn còn; gọi `logout()` → về `/login`.

> **Tiếp theo:** [Phase 8 — Layout & Providers](FRONTEND_PHASE08_LAYOUT_PROVIDERS.md)
