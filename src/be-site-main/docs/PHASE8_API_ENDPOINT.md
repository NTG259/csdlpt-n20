# PHASE 8 — API ENDPOINT (chi tiết)

> **Phạm vi:** 5 controller, 9 endpoint tối thiểu. Mọi response bọc `ApiResponse` / `PageResponse`.
> **Mục tiêu phase:** API rõ ràng, dễ test Postman, dễ giải thích.

> **Tiến độ cập nhật 2026-06-02:**
> - Đã tạo `AuthController`, `ProductController`, `CategoryController`, `BrandController`, `RegionController`
> - Toàn bộ 9 endpoint Phase 8 đang bọc response thành `ApiResponse`, trong đó `GET /api/products` trả `ApiResponse<PageResponse<...>>`
> - `SecurityConfig` đã mở public cho `GET /api/auth/check-email` và `GET /api/auth/check-phone`
> - Đã có test MVC cho auth/catalog endpoint, gồm `@Valid`, wrapper JSON và binding `Pageable`
> - Đã thêm [requests.http](./requests.http) để demo nhanh bằng REST Client / Postman

---

## 0. Quy ước chung
- Base path `/api`. Trả `ResponseEntity<ApiResponse<T>>` hoặc `ApiResponse<T>` trực tiếp.
- Controller **chỉ** nhận request, gọi service, bọc response — không chứa logic nghiệp vụ.
- `@Valid` cho body; `@RequestParam`/`@PathVariable` cho query/path.
- Status: tạo mới → 201 (đăng ký); còn lại 200. Lỗi do `GlobalExceptionHandler` (Phase 5).

---

## 1. Bảng endpoint

| # | Method | URL | Auth | Body | Controller → Service | data |
|---|---|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | ❌ | `RegisterRequest` | AuthController → AuthService.register | `AuthResponse` (201) |
| 2 | POST | `/api/auth/login` | ❌ | `LoginRequest` | AuthService.login | `AuthResponse` |
| 3 | GET | `/api/auth/check-email?email=` | ❌ | — | AuthService.isEmailAvailable | `CheckAvailabilityResponse` |
| 4 | GET | `/api/auth/check-phone?phone=` | ❌ | — | AuthService.isPhoneAvailable | `CheckAvailabilityResponse` |
| 5 | GET | `/api/products?page=&size=&maDanhMuc=&maThuongHieu=&trangThai=` | ❌ | — | ProductService.getProducts | `PageResponse<ProductListItemResponse>` |
| 6 | GET | `/api/products/{maSP}` | ❌ | — | ProductService.getProductDetail | `ProductDetailResponse` |
| 7 | GET | `/api/categories` | ❌ | — | CategoryService.getAll | `List<CategoryResponse>` |
| 8 | GET | `/api/brands` | ❌ | — | BrandService.getAll | `List<BrandResponse>` |
| 9 | GET | `/api/regions` | ❌ | — | RegionService.getAll | `List<RegionResponse>` |

**Controller:** `AuthController`, `ProductController`, `CategoryController`, `BrandController`, `RegionController`.

---

## 2. Chi tiết tham số

### `/api/products` (endpoint 5)
| Param | Kiểu | Mặc định | Ghi chú |
|---|---|---|---|
| `page` | int | 0 | 0-based (Spring `Pageable`) |
| `size` | int | 10 | |
| `sort` | string | `ngayTao,desc` | tùy chọn |
| `maDanhMuc` | string | null | lọc theo danh mục |
| `maThuongHieu` | string | null | lọc theo thương hiệu |
| `trangThai` | Boolean | null | lọc theo trạng thái |

> Dùng `@RequestParam(required=false)` cho các filter; Spring tự bind `Pageable` (bật qua `@PageableDefault` nếu muốn mặc định).

---

## 3. Response mẫu

**Đăng ký (201):**
```json
{
  "success": true,
  "message": "Đăng ký thành công",
  "data": {
    "token": "eyJhbGciOi...", "tokenType": "Bearer", "expiresIn": 31536000,
    "userId": "3f9a...-uuid", "hoTen": "Nguyen Van A",
    "email": "a@example.com", "maKhuVuc": "KV01", "vaiTro": "USER"
  },
  "timestamp": "2026-06-02T10:00:00"
}
```

**check-email (200):**
```json
{ "success": true, "message": "OK", "data": { "available": false }, "timestamp": "..." }
```

**Chi tiết sản phẩm không tồn tại (404):**
```json
{ "success": false, "message": "Không tìm thấy sản phẩm SP999",
  "errorCode": "RESOURCE_NOT_FOUND", "details": [], "timestamp": "..." }
```

> Danh sách sản phẩm / đăng nhập: xem `PHASE4_DTO_RESPONSE.md`.

---

## ✅ Checklist Phase 8
- [x] 5 controller, 9 endpoint chạy
- [x] Mọi response bọc `ApiResponse` / `PageResponse`
- [x] Đăng ký trả 201; check-email/phone trả `available`
- [x] `/api/products` bind `Pageable` + filter `required=false`
- [x] Public endpoint gọi không cần token
- [x] Lưu Postman collection / `requests.http` trong `docs/`

---
*Hết Phase 8. Tiếp theo: Phase 9 — Dữ liệu mẫu & kiểm thử.*
