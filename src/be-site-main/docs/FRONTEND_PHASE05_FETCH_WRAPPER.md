# FRONTEND PHASE 5 — Fetch wrapper dùng chung (chi tiết)

> **Phạm vi:** Một hàm `apiFetch` duy nhất cho mọi lời gọi HTTP tới cả 3 backend, tự bóc tách `ApiResponse` và ném `ApiError` khi lỗi.
> **Mục tiêu phase:** Sau phase này, hook/UI gọi API chỉ nhận về `data` (kiểu T) hoặc bắt `ApiError` — không phải tự parse JSON, tự check `success`, tự đọc status.
> **Nguồn sự thật:** `common/ApiResponse.java`, `common/ErrorResponse.java`, `exception/GlobalExceptionHandler.java`, `config/SecurityConfig.java`.

---

## 0. Tiền đề

- Phase 4 xong: có `ApiResponse`, `ApiErrorResponse`, `ApiErrorCode` trong `@/types/api`.
- **CORS:** BE đã `setAllowedOrigins(http://localhost:3000)` + cho header `Authorization`, `Content-Type` (xem `SecurityConfig.corsConfigurationSource`). Vì dùng **Bearer token** (không cookie), FE **không cần** `credentials: "include"`.

---

## Task 5.1 — Class `ApiError`

- **Mục tiêu:** Một loại lỗi chuẩn mang đủ thông tin để UI rẽ nhánh (status, errorCode, details).
- **Đối chiếu BE:** `GlobalExceptionHandler` luôn trả `ErrorResponse { message, errorCode, details[] }` kèm HTTP status tương ứng.
- **File tạo:** `src/lib/api-error.ts`
  ```ts
  import type { ApiErrorCode } from "@/types/api";

  export class ApiError extends Error {
    status: number;
    errorCode: ApiErrorCode | "UNKNOWN";
    details: string[];

    constructor(
      message: string,
      status: number,
      errorCode: ApiErrorCode | "UNKNOWN" = "UNKNOWN",
      details: string[] = [],
    ) {
      super(message);
      this.name = "ApiError";
      this.status = status;
      this.errorCode = errorCode;
      this.details = details;
    }
  }

  export function isApiError(e: unknown): e is ApiError {
    return e instanceof ApiError;
  }
  ```
- **Kết quả mong muốn:** `catch (e) { if (isApiError(e)) toast(e.message) }` dùng được ở mọi nơi.

---

## Task 5.2 — Hàm `apiFetch`

- **Mục tiêu:** Ghép URL, gắn header, parse JSON, bóc `data` khi success, ném `ApiError` khi lỗi.
- **Quy ước FE (theo doc BE mục 1.3):** kiểm tra `res.ok && body.success` → trả `body.data`; ngược lại ném lỗi từ `body.message/errorCode/details`.
- **File tạo:** `src/lib/api-fetch.ts`
  ```ts
  import { ApiError } from "./api-error";
  import type { ApiResponse, ApiErrorResponse } from "@/types/api";

  export interface ApiFetchOptions extends Omit<RequestInit, "body"> {
    baseUrl: string;
    token?: string | null;
    body?: unknown;        // object thuần, sẽ tự JSON.stringify
    query?: Record<string, string | number | boolean | undefined>;
  }

  function buildUrl(base: string, path: string, query?: ApiFetchOptions["query"]) {
    const url = new URL(path, base);
    if (query) {
      for (const [k, v] of Object.entries(query)) {
        if (v !== undefined && v !== "") url.searchParams.set(k, String(v));
      }
    }
    return url.toString();
  }

  export async function apiFetch<T>(path: string, options: ApiFetchOptions): Promise<T> {
    const { baseUrl, token, body, query, headers, ...rest } = options;

    const res = await fetch(buildUrl(baseUrl, path, query), {
      ...rest,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    // Một số lỗi (vd 401 từ SecurityConfig) vẫn trả JSON ErrorResponse.
    let json: ApiResponse<T> | ApiErrorResponse | null = null;
    const text = await res.text();
    if (text) {
      try { json = JSON.parse(text); } catch { /* body không phải JSON */ }
    }

    if (res.ok && json && json.success) {
      return (json as ApiResponse<T>).data;
    }

    // Lỗi: ưu tiên thông tin từ ErrorResponse, fallback theo status.
    const err = json as ApiErrorResponse | null;
    throw new ApiError(
      err?.message ?? `Lỗi máy chủ (${res.status})`,
      res.status,
      err?.errorCode ?? "UNKNOWN",
      err?.details ?? [],
    );
  }
  ```
- **Lưu ý:**
  - Đọc bằng `res.text()` trước rồi `JSON.parse` để chịu được response rỗng/không phải JSON.
  - `query` giúp build query string an toàn (bỏ field `undefined`) — dùng cho `/api/products` (page/size/sort/maDanhMuc...).
- **Kết quả mong muốn:**
  - `await apiFetch<ProductDetail>(MAIN_ENDPOINTS.PRODUCT_DETAIL("SP001"), { baseUrl })` → trả thẳng object `ProductDetail`.
  - Mã không tồn tại → ném `ApiError` với `status 404`, `errorCode "RESOURCE_NOT_FOUND"`.

---

## Task 5.3 — Xử lý 401 tập trung (token hết hạn)

- **Mục tiêu:** Khi token sai/hết hạn, dọn phiên để app điều hướng về `/login`, không lặp lỗi khắp nơi.
- **Đối chiếu BE:** Gọi API cần auth mà thiếu/hết token → `SecurityConfig` trả `401` + `errorCode "INVALID_CREDENTIALS"`.
- **Cách làm (đơn giản, không phụ thuộc React):** đăng ký 1 callback "khi 401" mà `apiFetch` gọi tới.
- **File tạo:** `src/lib/auth-events.ts`
  ```ts
  type Handler = () => void;
  let onUnauthorized: Handler | null = null;
  export function setUnauthorizedHandler(fn: Handler | null) { onUnauthorized = fn; }
  export function emitUnauthorized() { onUnauthorized?.(); }
  ```
- **File chỉnh:** `src/lib/api-fetch.ts` — ngay trước khi `throw`:
  ```ts
  import { emitUnauthorized } from "./auth-events";
  // ...
  if (res.status === 401) emitUnauthorized();
  ```
- **Liên kết Phase 7/8:** `AuthProvider` sẽ `setUnauthorizedHandler(() => { clearSession(); router.replace("/login") })`.
- **Kết quả mong muốn:** Token hết hạn khi vào `/cart` → tự logout + về `/login`, không crash.

---

## Checklist hoàn thành Phase 5

- [x] `src/lib/api-error.ts` — `ApiError`, `isApiError`.
- [x] `src/lib/api-fetch.ts` — `apiFetch<T>` (gắn Bearer, build query, bóc `data`, ném `ApiError`).
- [x] `src/lib/auth-events.ts` — hook 401 tập trung.
- [x] Thử gọi `apiFetch<PageResponse<ProductListItem>>("/api/products", { baseUrl: MAIN_URL, query: { size: 2 } })` ra dữ liệu thật từ `:8080`.
- [x] Thử gọi sản phẩm sai mã → bắt được `ApiError` với `errorCode "RESOURCE_NOT_FOUND"`.

> **Tiếp theo:** [Phase 6 — API Clients](FRONTEND_PHASE06_API_CLIENTS.md)
