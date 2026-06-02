# Tài liệu Backend SITE MAIN — CSDL phân tán "Bán hàng đa kho"

Bộ tài liệu kế hoạch + thiết kế chi tiết cho backend Site Main (Java 21, Spring Boot 4.x, SQL Server).

## Mục lục theo phase

| Phase | Tài liệu | Nội dung |
|---|---|---|
| Tổng quan | [KE_HOACH_TASK_SITE_MAIN.md](KE_HOACH_TASK_SITE_MAIN.md) | Kế hoạch tổng 10 phase, thứ tự triển khai, tổng hợp class |
| 1 | [PHASE1_SETUP.md](PHASE1_SETUP.md) | Khởi tạo project, dependency, package, `application.yml` |
| 2 | [PHASE2_DOMAIN_ENTITY.md](PHASE2_DOMAIN_ENTITY.md) | Mapping entity **đủ 18 bảng** (Nhóm A Site Main + Nhóm B khu vực) |
| 3 | [PHASE3_REPOSITORY.md](PHASE3_REPOSITORY.md) | Repository, query, projection, phân trang |
| 4 | [PHASE4_DTO_RESPONSE.md](PHASE4_DTO_RESPONSE.md) | DTO request/response + wrapper chuẩn (`ApiResponse`...) |
| 5 | [PHASE5_VALIDATION_EXCEPTION.md](PHASE5_VALIDATION_EXCEPTION.md) | Validation + `GlobalExceptionHandler` + bộ `errorCode` |
| 6 | [PHASE6_SECURITY_JWT.md](PHASE6_SECURITY_JWT.md) | Spring Security + JWT + public/protected |
| 7 | [PHASE7_SERVICE.md](PHASE7_SERVICE.md) | Service nghiệp vụ (auth, product, lookup) |
| 8 | [PHASE8_API_ENDPOINT.md](PHASE8_API_ENDPOINT.md) | 9 endpoint + tham số + response mẫu |
| 9 | [PHASE9_SAMPLE_DATA_TEST.md](PHASE9_SAMPLE_DATA_TEST.md) | Dữ liệu mẫu + kịch bản test |
| 10 | [PHASE10_NEXT_PHASE.md](PHASE10_NEXT_PHASE.md) | Chuẩn bị nối Site Bắc/Nam |

## Thứ tự đọc/triển khai khuyến nghị
1 → 2 → 3 → (4 + 5) → **7+8 cho luồng đọc sản phẩm trước** → 6 (security) → 7+8 cho auth → 9 → 10.

## Quy ước xuyên suốt (đã chốt)
- Khóa: VARCHAR → `String`; UNIQUEIDENTIFIER → `UUID` (`@JdbcTypeCode(SqlTypes.UUID)`).
- `NguoiDung.MaND` = `User_Global_Index.MaND` (sinh UUID 1 lần ở service).
- `TrangThai TINYINT(0,1)` → `Boolean` + `BooleanToTinyIntConverter`.
- Quan hệ `@ManyToOne`/`@OneToOne` = LAZY; API đọc dùng projection / `join fetch`.
- Mọi response bọc `ApiResponse`/`PageResponse`; lỗi bọc `ErrorResponse` (errorCode SNAKE_CASE).
- Không bao giờ trả `matKhau` ra DTO.
- `ddl-auto: validate` (DB do nhóm CSDL thiết kế sẵn).
