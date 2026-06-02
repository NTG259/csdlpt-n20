# FRONTEND PHASE 9 — Feature Auth: /login & /register (chi tiết)

> **Phạm vi:** Hai trang `/login`, `/register` dùng React Hook Form + Zod + shadcn Form, gọi Site Main qua `mainApiClient`.
> **Mục tiêu phase:** Đăng nhập/đăng ký thành công → lưu phiên (Phase 7) → điều hướng `/products`. Lỗi hiển thị đúng `message` Việt hoá từ BE.
> **Nguồn sự thật:** `dto/request/RegisterRequest.java`, `dto/request/LoginRequest.java`, `AuthController.java`, bảng mã lỗi (`ErrorCodes`).

---

## 0. Ràng buộc validation lấy từ BE (phải khớp 1–1)

**`LoginRequest`:**
| Field | Ràng buộc BE | Message BE |
|---|---|---|
| `email` | NotBlank + Email + ≤100 | "Email không được để trống" / "Email không đúng định dạng" |
| `matKhau` | NotBlank + 6–72 | "Mật khẩu phải từ 6 đến 72 ký tự" |

**`RegisterRequest`:**
| Field | Bắt buộc | Ràng buộc BE |
|---|---|---|
| `hoTen` | ✅ | NotBlank, ≤100 |
| `email` | ✅ | NotBlank, Email, ≤100 |
| `soDienThoai` | ✅ | regex `^(0|\+84)\d{9,10}$`, ≤15 |
| `matKhau` | ✅ | 6–72 |
| `maKhuVuc` | ✅ | NotBlank, ≤10 — giá trị thực: `"Bac"` / `"Nam"` |
| `diaChi` | ❌ | ≤300 |
| `ngaySinh` | ❌ | `Past` (quá khứ), định dạng `YYYY-MM-DD` |
| `gioiTinh` | ❌ | ≤10 (vd "Nam", "Nữ") |
| `cccd` | ❌ | regex `^\d{12}$` (đúng 12 số) |

> FE validate trước để UX tốt, nhưng **BE vẫn là nguồn cuối**: lỗi `400 VALIDATION_ERROR` có mảng `details[]` (vd `"email: Email không đúng định dạng"`) — có thể hiển thị thêm.

---

## Task 9.1 — Zod schema khớp BE

- **Mục tiêu:** Validate client đúng ràng buộc trên.
- **File tạo:** `src/features/auth/schemas.ts`
  ```ts
  import { z } from "zod";

  const phoneRegex = /^(0|\+84)\d{9,10}$/;

  export const loginSchema = z.object({
    email: z.string().min(1, "Email không được để trống").email("Email không đúng định dạng").max(100),
    matKhau: z.string().min(6, "Mật khẩu phải từ 6 đến 72 ký tự").max(72),
  });
  export type LoginInput = z.infer<typeof loginSchema>;

  export const registerSchema = z.object({
    hoTen: z.string().min(1, "Họ tên không được để trống").max(100),
    email: z.string().min(1).email("Email không đúng định dạng").max(100),
    soDienThoai: z.string().regex(phoneRegex, "Số điện thoại không đúng định dạng").max(15),
    matKhau: z.string().min(6, "Mật khẩu phải từ 6 đến 72 ký tự").max(72),
    maKhuVuc: z.string().min(1, "Vui lòng chọn khu vực").max(10),
    diaChi: z.string().max(300).optional().or(z.literal("")),
    ngaySinh: z.string().optional().or(z.literal("")),   // YYYY-MM-DD
    gioiTinh: z.string().max(10).optional().or(z.literal("")),
    cccd: z.string().regex(/^\d{12}$/, "CCCD phải gồm đúng 12 chữ số").optional().or(z.literal("")),
  });
  export type RegisterInput = z.infer<typeof registerSchema>;
  ```
- **Lưu ý:** field optional dùng `.or(z.literal(""))` để cho phép ô trống; trước khi gửi, lọc bỏ chuỗi rỗng (BE coi `null`/vắng là không có).
- **Kết quả mong muốn:** Sai định dạng báo lỗi tại field trước khi gọi API.

---

## Task 9.2 — Hook khu vực cho dropdown đăng ký

- **Mục tiêu:** Đổ dropdown khu vực từ `GET /api/regions` (public).
- **File tạo:** `src/features/auth/use-regions.ts`
  ```ts
  import { useQuery } from "@tanstack/react-query";
  import { mainApiClient } from "@/lib/main-api-client";
  import { MAIN_ENDPOINTS } from "@/constants/endpoints";
  import { QK } from "@/constants/query-keys";
  import type { Region } from "@/types/domain";

  export function useRegions() {
    return useQuery({
      queryKey: QK.regions(),
      queryFn: () => mainApiClient.get<Region[]>(MAIN_ENDPOINTS.REGIONS),
      staleTime: Infinity, // dữ liệu tham chiếu gần như tĩnh
    });
  }
  ```
- **Kết quả mong muốn:** Dropdown hiển thị `tenKhuVuc`, value = `maKhuVuc` (`"Bac"`/`"Nam"`).

---

## Task 9.3 — Mutation login/register

- **Mục tiêu:** Gọi Site Main, thành công → `login(auth)` (Phase 7) → điều hướng.
- **File tạo:** `src/features/auth/use-auth-mutations.ts`
  ```ts
  import { useMutation } from "@tanstack/react-query";
  import { useRouter } from "next/navigation";
  import { toast } from "sonner";
  import { mainApiClient } from "@/lib/main-api-client";
  import { MAIN_ENDPOINTS } from "@/constants/endpoints";
  import { useAuth } from "./auth-context";
  import { isApiError } from "@/lib/api-error";
  import type { AuthResponse } from "@/types/domain";
  import type { LoginInput, RegisterInput } from "./schemas";

  export function useLogin() {
    const { login } = useAuth();
    const router = useRouter();
    return useMutation({
      mutationFn: (input: LoginInput) =>
        mainApiClient.post<AuthResponse>(MAIN_ENDPOINTS.LOGIN, input),
      onSuccess: (auth) => { login(auth); toast.success("Đăng nhập thành công"); router.replace("/products"); },
      onError: (e) => toast.error(isApiError(e) ? e.message : "Có lỗi xảy ra"),
    });
  }

  export function useRegister() {
    const { login } = useAuth();
    const router = useRouter();
    return useMutation({
      mutationFn: (input: RegisterInput) => {
        // bỏ field rỗng để BE nhận đúng "không có"
        const body = Object.fromEntries(Object.entries(input).filter(([, v]) => v !== "" && v != null));
        return mainApiClient.post<AuthResponse>(MAIN_ENDPOINTS.REGISTER, body);
      },
      onSuccess: (auth) => { login(auth); toast.success("Đăng ký thành công"); router.replace("/products"); },
      onError: (e) => toast.error(isApiError(e) ? e.message : "Có lỗi xảy ra"),
    });
  }
  ```
- **Quan trọng:** register trả `AuthResponse` ⇒ **tự đăng nhập ngay** sau đăng ký, không cần qua trang login.
- **Mã lỗi cần lưu ý (toast `message` là đủ):** `401 INVALID_CREDENTIALS` (login sai), `409 DUPLICATE_EMAIL`, `409 DUPLICATE_PHONE`, `INVALID_REGION`, `400 VALIDATION_ERROR`.
- **Kết quả mong muốn:** Thành công → vào `/products`; lỗi → toast đúng thông điệp Việt hoá.

---

## Task 9.4 — Trang `/login`

- **Mục tiêu:** Form email + mật khẩu.
- **File tạo:** `src/app/(auth)/login/page.tsx`, `src/features/auth/login-form.tsx`
  - `login-form.tsx`: `useForm<LoginInput>({ resolver: zodResolver(loginSchema) })`, dùng shadcn `Form/Input/Button`, submit gọi `useLogin().mutate`.
  - Có link sang `/register`.
- **Kết quả mong muốn:** Sai email/mật khẩu → toast "Sai thông tin đăng nhập…" (message từ BE); đúng → vào `/products`.

---

## Task 9.5 — Trang `/register`

- **Mục tiêu:** Form đầy đủ field, dropdown khu vực từ `useRegions()`.
- **File tạo:** `src/app/(auth)/register/page.tsx`, `src/features/auth/register-form.tsx`
  - Dropdown dùng shadcn `Select`: options từ `useRegions().data` (`value=maKhuVuc`, label=`tenKhuVuc`).
  - `ngaySinh` dùng `<input type="date">` (gửi `YYYY-MM-DD`).
  - (Tuỳ chọn) gọi `GET /api/auth/check-email`/`check-phone` khi blur để báo sớm — không bắt buộc cho demo.
- **Kết quả mong muốn:** Đăng ký thành công → tự đăng nhập → `/products`. Trùng email/SĐT → toast `DUPLICATE_EMAIL`/`DUPLICATE_PHONE`.

---

## Checklist hoàn thành Phase 9

- [x] `schemas.ts` khớp ràng buộc BE (phone regex, mật khẩu 6–72, cccd 12 số).
- [x] `use-regions.ts` đổ dropdown từ `/api/regions`.
- [x] `use-auth-mutations.ts` — login & register, register tự đăng nhập.
- [x] `/login` và `/register` chạy, lỗi hiển thị `message` Việt hoá.
- [ ] Đăng ký user **Bac** và user **Nam** thành công, vào được `/products`.

> Ghi chú verify: `/login` và `/register` đã trả `200` trên dev server. Nghiệm thu đăng ký user Bac/Nam cần backend Site Main kết nối được SQL Server `localhost:1433`.

> **Tiếp theo:** [Phase 10 — Sản phẩm](FRONTEND_PHASE10_PRODUCTS.md)
