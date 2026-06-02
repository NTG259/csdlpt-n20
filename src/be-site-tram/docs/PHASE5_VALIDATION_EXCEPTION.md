# PHASE 5 — VALIDATION & EXCEPTION (chi tiết)

> **Phạm vi:** Validate đầu vào + xử lý lỗi tập trung, trả về `ErrorResponse` chuẩn (đã định ở Phase 4).
> **Mục tiêu phase:** Báo lỗi rõ ràng, đúng HTTP status, không lộ stacktrace, đồng bộ `errorCode`.

> **Tiến độ cập nhật 2026-06-02:**
> - Đã chuẩn hóa bộ `ErrorCodes` dùng chung cho response và exception
> - Đã hoàn thành `BusinessException` + 5 exception con theo đúng mã lỗi/status
> - Đã thêm `GlobalExceptionHandler` bắt validation, business, `DataIntegrityViolationException` và fallback 500
> - `LoginRequest` đã được siết thêm `@Size(max=100)` cho email và `@Size(min=6,max=72)` cho mật khẩu
> - Đã có test tập trung cho các case: request body invalid, constraint violation, business 409, DB integrity 409, fallback 500

---

## 0. Bộ `errorCode` thống nhất (khớp Phase 4)

| errorCode | HTTP | Khi nào |
|---|---|---|
| `VALIDATION_ERROR` | 400 | sai định dạng field (`@Valid` fail) |
| `DUPLICATE_EMAIL` | 409 | email đã tồn tại (UGI) |
| `DUPLICATE_PHONE` | 409 | SĐT đã tồn tại (UGI) |
| `INVALID_CREDENTIALS` | 401 | đăng nhập sai email/mật khẩu |
| `ACCESS_DENIED` | 403 | đã xác thực nhưng không đủ quyền |
| `RESOURCE_NOT_FOUND` | 404 | sản phẩm/khu vực không tồn tại |
| `INVALID_REGION` | 400 | maKhuVuc không có trong KhuVuc |
| `INTERNAL_ERROR` | 500 | lỗi không lường trước (fallback) |

---

## Task 5.1 — Validation đầu vào (Jakarta Bean Validation)
- **Kích hoạt:** `@Valid` ở tham số controller; vi phạm → `MethodArgumentNotValidException`.
- **Annotation trên DTO request:**

| Field | Annotation | Ghi chú |
|---|---|---|
| email | `@NotBlank @Email @Size(max=100)` | |
| soDienThoai | `@NotBlank @Pattern(regexp="^(0\|\\+84)\\d{9,10}$")` | chốt: bắt buộc (Phase 4) |
| matKhau | `@NotBlank @Size(min=6, max=72)` | 72 = giới hạn BCrypt |
| hoTen | `@NotBlank @Size(max=100)` | |
| maKhuVuc | `@NotBlank` | tồn tại → check ở service (`INVALID_REGION`) |
| cccd | `@Pattern(regexp="^\\d{12}$")` (nếu nhập) | khớp CHECK len=12 |
| ngaySinh | `@Past` | |

> **2 mức validate:**
> 1. **Cú pháp** (annotation) → `VALIDATION_ERROR` 400.
> 2. **Nghiệp vụ** (tồn tại maKhuVuc, trùng email/SĐT) → ném custom exception ở service.

---

## Task 5.2 — Custom exception (`exception/`)
| Class | Kế thừa | errorCode / HTTP |
|---|---|---|
| `BusinessException` (cha, mang `errorCode` + `httpStatus`) | RuntimeException | — |
| `DuplicateEmailException` | BusinessException | DUPLICATE_EMAIL / 409 |
| `DuplicatePhoneException` | BusinessException | DUPLICATE_PHONE / 409 |
| `InvalidCredentialsException` | BusinessException | INVALID_CREDENTIALS / 401 |
| `ResourceNotFoundException` | BusinessException | RESOURCE_NOT_FOUND / 404 |
| `InvalidRegionException` | BusinessException | INVALID_REGION / 400 |

> Gợi ý: `BusinessException(String errorCode, HttpStatus status, String message)` → handler chỉ cần đọc field, không cần `instanceof` từng loại.

---

## Task 5.3 — GlobalExceptionHandler
- **Class:** `exception/GlobalExceptionHandler` (`@RestControllerAdvice`)
- **Các `@ExceptionHandler`:**

| Bắt | Xử lý |
|---|---|
| `MethodArgumentNotValidException` | gom field error → `details` (vd `"email: không đúng định dạng"`), `VALIDATION_ERROR`, 400 |
| `ConstraintViolationException` | tương tự (cho `@RequestParam`/`@PathVariable` validate) |
| `BusinessException` | đọc `errorCode` + `httpStatus` từ exception → `ErrorResponse` |
| `DataIntegrityViolationException` | chốt chặn DB (UNIQUE) → 409 (map về DUPLICATE nếu nhận diện được) |
| `Exception` (fallback) | log đầy đủ, trả `INTERNAL_ERROR` 500, **không lộ stacktrace** ra client |

- **Định dạng trả về:** luôn là `ErrorResponse` (Phase 4) với `success=false`, `details` rỗng nếu không phải lỗi field.

---

## ✅ Checklist Phase 5
- [x] DTO request gắn đủ annotation validation
- [x] `BusinessException` cha + 5 exception con mang errorCode/status
- [x] `GlobalExceptionHandler` bắt: validation, business, DataIntegrity, fallback
- [x] `details[]` liệt kê lỗi field cho `VALIDATION_ERROR`
- [x] Fallback 500 KHÔNG lộ stacktrace; có log nội bộ
- [x] Test: gửi email sai định dạng → 400; mật khẩu ngắn → 400; business/data-integrity/fallback đã có test tập trung

---
*Hết Phase 5. Tiếp theo: Phase 6 — Security & JWT.*
