# KẾ HOẠCH TASK — BACKEND SITE MAIN

> **Đề tài:** Hệ thống bán hàng trực tuyến đa kho (CSDL phân tán 3 site)
> **Phạm vi tài liệu:** Backend cho **Site Main** giai đoạn đầu — xác thực + dữ liệu sản phẩm dùng chung.
> **Công nghệ:** Java 21 · Spring Boot 4.x · Gradle (Kotlin DSL) · SQL Server · Spring Web · Spring Data JPA · Spring Security + JWT · Lombok · Validation.
> **Nguyên tắc:** Đơn giản — rõ kiến trúc — dễ demo — dễ bảo vệ đồ án. Tránh over-engineering. KHÔNG Kafka/Redis/Docker/microservice ở giai đoạn này.

> **Tiến độ cập nhật 2026-06-02:**
> - Phase 1: đã hoàn thành
> - Phase 2 (Nhóm A / Site Main): đã hoàn thành
> - Phase 3 (repository + projection): đã hoàn thành
> - Phase 4 (DTO + response wrapper): đã hoàn thành
> - Phase 5 (validation + exception): đã hoàn thành
> - Phase 6 (security + JWT): đã hoàn thành
> - Phase 7 (service nghiệp vụ): đã hoàn thành
> - Phase 8 (API endpoint): đã hoàn thành

---

## 0. Bối cảnh & ranh giới Site Main

| Vai trò | Site Main giữ | Site Bắc / Nam giữ (giai đoạn sau) |
|---|---|---|
| Dữ liệu | Toàn hệ thống, dùng chung | Phân mảnh theo khu vực |
| Bảng phụ trách | KhuVuc, DanhMuc, ThuongHieu, DanhMuc_ThuongHieu, SanPham_Core, SanPham_Detail, NguoiDung, User_Global_Index | GioHang, ChiTietGioHang, DonHang, ChiTietDonHang, Kho, TonKho, PhieuXuat/Nhap... |
| Nghiệp vụ | Đăng ký, đăng nhập, check email/SĐT toàn cục, xem sản phẩm/danh mục/thương hiệu | Giỏ hàng, đơn hàng, tồn kho |

**Giai đoạn đầu KHÔNG làm:** giỏ hàng, đơn hàng, kho, tồn kho, kết nối liên site, giao dịch phân tán.

> **Điểm cần nhấn trong báo cáo:** `User_Global_Index` chính là cơ chế đảm bảo **email/SĐT duy nhất toàn hệ thống** — đây là bảng "chỉ mục toàn cục" giúp các site biết user thuộc khu vực nào mà không phải quét toàn bộ dữ liệu phân tán. Site Main là nơi sở hữu và kiểm tra bảng này.

---

## PHASE 1 — Khởi tạo project

> **Mục tiêu phase:** Có project chạy được, kết nối SQL Server thành công, cấu trúc package rõ ràng.

### Task 1.1 — Bổ sung dependency còn thiếu
- **Mục tiêu:** Đủ thư viện cho JPA, Security, JWT, Validation.
- **Việc cần làm:** Thêm vào `build.gradle.kts`:
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-validation`
  - `io.jsonwebtoken:jjwt-api`, `jjwt-impl` (runtime), `jjwt-jackson` (runtime) — phiên bản `0.12.x`
  - (Đã có sẵn: `spring-boot-starter-webmvc`, `lombok`, `mssql-jdbc`, `devtools`)
- **File liên quan:** `build.gradle.kts`
- **Lưu ý:** Bạn đang dùng **Spring Boot 4.x** nên starter tên là `...-webmvc` chứ không phải `...-web`. Giữ nguyên quy ước này cho các starter mới (vẫn dùng tên chuẩn cho jpa/security/validation).

### Task 1.2 — Cấu trúc thư mục & quy ước package
- **Mục tiêu:** Tổ chức code theo tầng, dễ tìm, dễ giải thích.
- **Quy ước package gốc:** `csdlpt.sitemain`
- **Cấu trúc đề xuất:**

```
src/main/java/csdlpt/sitemain
├── SitemainApplication.java
├── config/            # SecurityConfig, JpaConfig, CorsConfig...
├── common/            # ApiResponse, ErrorResponse, PageResponse, constant, enum
├── exception/         # GlobalExceptionHandler + các custom exception
├── security/          # JwtService, JwtAuthenticationFilter, CustomUserDetails...
├── domain/            # @Entity (mapping bảng SQL Server)
│   └── entity/
├── repository/        # interface JpaRepository
├── dto/
│   ├── request/       # RegisterRequest, LoginRequest...
│   └── response/      # ProductResponse, AuthResponse...
├── service/           # interface + impl (AuthService, ProductService...)
│   └── impl/
├── controller/        # REST controller
└── util/              # tiện ích nhỏ (nếu cần)
```

- **Quy ước đặt tên:** Entity = tên class tiếng Anh hoặc giữ nguyên tiếng Việt không dấu (chọn 1 chuẩn, dùng nhất quán). Khuyến nghị: **giữ tên domain tiếng Việt không dấu** cho dễ map báo cáo (`NguoiDung`, `SanPhamCore`, `DanhMuc`), nhưng đặt tên DTO/Service/Controller theo tiếng Anh (`ProductResponse`, `AuthService`).

### Task 1.3 — Chuyển sang `application.yml` + cấu hình kết nối
- **Mục tiêu:** Cấu hình DB, profile, encoding, timezone tập trung 1 nơi.
- **Việc cần làm:**
  - Đổi `application.properties` → `application.yml` (hoặc giữ properties, tùy bạn — yml dễ đọc hơn cho báo cáo).
  - Cấu hình datasource SQL Server, JPA (`ddl-auto: none` vì DB đã có sẵn), `show-sql: true` cho dev.
  - Tách `application-dev.yml`.
- **File liên quan:** `src/main/resources/application.yml`, `application-dev.yml`
- **Khung cấu hình (chưa phải code, chỉ key cần có):**

```yaml
spring:
  application: { name: sitemain }
  profiles: { active: dev }
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=SiteMain;encrypt=true;trustServerCertificate=true
    username: sa
    password: <đặt qua biến môi trường>
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
  jpa:
    hibernate: { ddl-auto: none }      # DB đã tạo sẵn, KHÔNG để Hibernate sửa schema
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.SQLServerDialect
        jdbc: { time_zone: Asia/Ho_Chi_Minh }
jwt:
  secret: <chuỗi bí mật >= 32 ký tự>
  expiration-ms: 86400000              # 1 ngày
server:
  port: 8080
```

- **Lưu ý:**
  - Encoding: file Java + yml để UTF-8; SQL Server dùng `NVARCHAR` cho dữ liệu tiếng Việt.
  - `ddl-auto: none` là **bắt buộc** — schema do nhóm CSDL thiết kế, backend chỉ map.
  - Không commit mật khẩu thật → dùng biến môi trường / `application-dev.yml` (đã gitignore).

### ✅ Checklist Phase 1
- [x] Thêm đủ dependency (jpa, security, validation, jwt)
- [x] Tạo cây package theo tầng
- [x] Tạo `application.yml` + `application-dev.yml`
- [x] Kết nối SQL Server chạy `./gradlew bootRun` không lỗi
- [x] Log/timezone/encoding hoạt động đúng (chữ tiếng Việt không lỗi font)

---

## PHASE 2 — Thiết kế domain / entity

> **Mục tiêu phase:** Map đúng 8 bảng cần dùng thành entity JPA, đúng kiểu khóa chính và quan hệ.

### Task 2.1 — Thứ tự làm entity (theo độ phụ thuộc)
Làm từ bảng "gốc" (không phụ thuộc) → bảng phụ thuộc:
1. `KhuVuc`
2. `DanhMuc` (tự tham chiếu cha-con)
3. `ThuongHieu`
4. `DanhMuc_ThuongHieu` (bảng nối, map qua `@ManyToMany`)
5. `SanPhamCore`
6. `SanPhamDetail`
7. `NguoiDung`
8. `UserGlobalIndex`

### Task 2.2 — Quy ước mapping & khóa chính
- **Mục tiêu:** Map đúng kiểu dữ liệu SQL Server.
- **Lưu ý khóa chính:**
  - Khóa **VARCHAR/NVARCHAR** (vd `MaKhuVuc`, `MaSP`): khai báo `String id`, **KHÔNG** dùng `@GeneratedValue` (mã do nghiệp vụ/nhóm CSDL sinh). Nếu mã tự sinh phía DB, để app tự set trước khi save.
  - Khóa **UNIQUEIDENTIFIER** (nếu `NguoiDung`/`UserGlobalIndex` dùng GUID): map sang `java.util.UUID` + `@JdbcTypeCode(SqlTypes.UUID)` hoặc `String`. Nếu DB tự sinh `NEWID()` → `@GeneratedValue(strategy = GenerationType.UUID)` hoặc lấy về sau insert.
  - Mọi entity: `@Entity` + `@Table(name = "<TênBảng>")`, từng cột `@Column(name="<TênCột>")` map đúng tên DB.
  - Dùng Lombok `@Getter @Setter @NoArgsConstructor` (tránh `@Data` trên entity để khỏi lỗi `equals/hashCode` với quan hệ lazy).

### Task 2.3 — Mapping quan hệ
- **Mục tiêu:** Khai báo đúng quan hệ giữa các bảng.

| Quan hệ | Loại | Khai báo gợi ý |
|---|---|---|
| `DanhMuc` → `DanhMuc` cha | tự tham chiếu N-1 | `@ManyToOne` `danhMucCha` + `@JoinColumn(name="MaDanhMucCha")` |
| `SanPhamCore` → `DanhMuc` | N-1 | `@ManyToOne` + `@JoinColumn(name="MaDanhMuc")` |
| `SanPhamCore` → `ThuongHieu` | N-1 | `@ManyToOne` + `@JoinColumn(name="MaThuongHieu")` |
| `SanPhamDetail` → `SanPhamCore` | 1-1 | `@OneToOne` + `@JoinColumn(name="MaSP")` (hoặc shared PK) |
| `NguoiDung` → `KhuVuc` | N-1 | `@ManyToOne` + `@JoinColumn(name="MaKV")` |
| `UserGlobalIndex` → `KhuVuc` | N-1 | `@ManyToOne` + `@JoinColumn(name="MaKhuVuc")` |
| `DanhMucThuongHieu` | bảng nối N-N | Giai đoạn hiện tại map qua `@ManyToMany @JoinTable`, chưa tạo entity riêng |

- **Lưu ý quan trọng:** Đặt **tất cả `@ManyToOne` là `FetchType.LAZY`** để tránh query thừa; lấy dữ liệu kèm bằng JPQL `join fetch` hoặc DTO projection khi cần.

### Task 2.4 — Đặt tên class/repository
- **Mục tiêu:** Nhất quán, dễ đoán.
- **Quy ước:**
  - Entity: `KhuVuc`, `DanhMuc`, `ThuongHieu`, `SanPhamCore`, `SanPhamDetail`, `NguoiDung`, `UserGlobalIndex`
  - Repository: `<Entity>Repository` (vd `SanPhamCoreRepository`)
  - Service: nghiệp vụ tiếng Anh — `ProductService`, `CategoryService`, `BrandService`, `AuthService`, `UserService`, `RegionService`

### ✅ Checklist Phase 2
- [x] 8 entity tạo xong, map đúng tên bảng/cột
- [x] Khóa chính VARCHAR / UNIQUEIDENTIFIER xử lý đúng
- [x] 7 quan hệ map đúng, tất cả `@ManyToOne` = LAZY
- [x] App khởi động không lỗi mapping (Hibernate validate được entity ↔ bảng)

---

## PHASE 3 — Cấu hình tầng dữ liệu (Repository)

> **Mục tiêu phase:** Có repository cho mọi bảng cần dùng, query cơ bản + phân trang sản phẩm.

### Task 3.1 — Tạo repository
- **Mục tiêu:** CRUD/đọc cơ bản qua Spring Data JPA.
- **Việc cần làm:** Tạo interface `extends JpaRepository<Entity, IdType>`:
  - `KhuVucRepository`, `DanhMucRepository`, `ThuongHieuRepository`, `SanPhamCoreRepository`, `SanPhamDetailRepository`, `NguoiDungRepository`, `UserGlobalIndexRepository`
- **File liên quan:** package `repository/`

### Task 3.2 — Query method cần có
- **Mục tiêu:** Đủ cho nghiệp vụ Phase 7.
- **Gợi ý method (derived query, chưa cần viết JPQL):**
  - `NguoiDungRepository.findByEmail(String email)`
  - `UserGlobalIndexRepository.existsByEmail(...)`, `existsBySoDienThoai(...)`, `findByEmail(...)`
  - `SanPhamCoreRepository`: `findByDanhMuc_MaDanhMuc(...)`, `findByThuongHieu_MaThuongHieu(...)`, có thể dùng `Page<...> findAll(Pageable)`; lọc nâng cao dùng `@Query` hoặc `Specification` nếu cần.
  - `SanPhamDetailRepository.findBySanPhamCore_MaSP(...)` (hoặc theo PK).

### Task 3.3 — Phân biệt 2 luồng dữ liệu
- **Mục tiêu:** Tách rõ trong báo cáo.
- **Luồng đọc sản phẩm (public, read-only):** SanPhamCore + SanPhamDetail + DanhMuc + ThuongHieu → ưu tiên **DTO projection / `join fetch`** cho nhẹ.
- **Luồng xác thực (NguoiDung + UserGlobalIndex):** cần entity đầy đủ (lấy mật khẩu hash, khu vực) → dùng entity bình thường.

### Task 3.4 — Quyết định DTO projection & phân trang
- **DTO projection:** **CÓ** cho danh sách sản phẩm (tránh trả nguyên entity + lazy lỗi). Có thể dùng interface projection hoặc constructor expression JPQL.
- **Phân trang:** **CÓ** cho `GET /api/products` — dùng `Pageable` + trả `PageResponse` chuẩn (xem Phase 4).

### ✅ Checklist Phase 3
- [x] 7 repository tạo xong
- [x] Query method check email/SĐT, tìm user theo email
- [x] Query sản phẩm có lọc danh mục/thương hiệu + phân trang
- [x] Thống nhất dùng DTO projection cho danh sách sản phẩm

---

## PHASE 4 — DTO & response chuẩn

> **Mục tiêu phase:** Định dạng request/response thống nhất, dễ demo Postman.

### Task 4.1 — Response wrapper chung
- **Mục tiêu:** Mọi API trả về cùng format → frontend & báo cáo dễ đọc.
- **Class cần tạo (`common/`):**
  - `ApiResponse<T>`: `{ success, message, data, timestamp }`
  - `ErrorResponse`: `{ success=false, message, errorCode, details[], timestamp }`
  - `PageResponse<T>`: `{ items[], page, size, totalElements, totalPages }`

### Task 4.2 — DTO request (`dto/request/`)
- `RegisterRequest`: hoTen, email, soDienThoai, matKhau, maKhuVuc (+ annotation validation)
- `LoginRequest`: email, matKhau

### Task 4.3 — DTO response (`dto/response/`)
- `AuthResponse`: token, tokenType="Bearer", userId, hoTen, email, maKhuVuc
- `ProductListItemResponse`: maSP, tenSP, giá, tenDanhMuc, tenThuongHieu, trangThai, ảnh (nếu có)
- `ProductDetailResponse`: toàn bộ field core + detail (mô tả, thông số...)
- `CategoryResponse`: maDanhMuc, tenDanhMuc, maDanhMucCha (đệ quy children nếu muốn cây)
- `BrandResponse`: maThuongHieu, tenThuongHieu
- `RegionResponse`: maKhuVuc, tenKhuVuc
- `RegisterResponse` (hoặc tái dùng `AuthResponse` nếu đăng ký xong auto-login)

### ✅ Checklist Phase 4
- [x] `ApiResponse`, `ErrorResponse`, `PageResponse` xong
- [x] DTO request có annotation validation
- [x] DTO response cho từng nhóm API
- [x] Quyết định: đăng ký xong có trả token luôn không

---

## PHASE 5 — Validation & Exception

> **Mục tiêu phase:** Báo lỗi rõ ràng, đúng HTTP status, không lộ stacktrace.

### Task 5.1 — Validation đầu vào
- **Mục tiêu:** Chặn dữ liệu sai từ tầng controller.
- **Annotation trên DTO request:**
  - Email: `@NotBlank @Email @Size(max=100)`
  - SĐT: `@Pattern(regexp = "^(0|\\+84)\\d{9,10}$")`
  - Mật khẩu: `@NotBlank @Size(min=6, max=72)`
  - Mã khu vực: `@NotBlank` + kiểm tra tồn tại ở tầng service (so với bảng `KhuVuc`)
- **Kích hoạt:** `@Valid` ở tham số controller.

### Task 5.2 — Custom exception
- **Class cần tạo (`exception/`):**
  - `DuplicateEmailException`
  - `DuplicatePhoneException`
  - `InvalidCredentialsException` (đăng nhập sai)
  - `ResourceNotFoundException` (sản phẩm/khu vực không tồn tại)
  - `InvalidRegionException`
  - `BusinessException` (cha chung — tùy chọn)

### Task 5.3 — GlobalExceptionHandler
- **Mục tiêu:** Bắt mọi lỗi → trả `ErrorResponse` thống nhất.
- **Class:** `GlobalExceptionHandler` (`@RestControllerAdvice`)
- **Map lỗi → status:**

| Exception | HTTP status |
|---|---|
| `MethodArgumentNotValidException` (validation) | 400 |
| `DuplicateEmailException` / `DuplicatePhoneException` | 409 Conflict |
| `InvalidCredentialsException` | 401 Unauthorized |
| `ResourceNotFoundException` | 404 |
| `Exception` (fallback) | 500 |

### ✅ Checklist Phase 5
- [x] DTO request gắn đủ annotation
- [x] 4–5 custom exception
- [x] `GlobalExceptionHandler` map đúng status + trả `ErrorResponse`
- [x] Test thử request sai và các case lỗi chính của handler

---

## PHASE 6 — Bảo mật cơ bản (Spring Security + JWT)

> **Mục tiêu phase:** API sản phẩm public; đăng ký/đăng nhập public; có cơ chế JWT để mở rộng sau.

### Task 6.1 — PasswordEncoder
- **Mục tiêu:** Hash mật khẩu.
- **Việc cần làm:** Bean `BCryptPasswordEncoder` trong `SecurityConfig`.

### Task 6.2 — JwtService
- **Mục tiêu:** Sinh & verify token.
- **Class:** `security/JwtService` — `generateToken(userId/email)`, `extractUsername(token)`, `isTokenValid(token)`. Dùng `jjwt`, đọc secret/expiration từ `application.yml`.

### Task 6.3 — JwtAuthenticationFilter
- **Class:** `security/JwtAuthenticationFilter extends OncePerRequestFilter` — đọc header `Authorization: Bearer ...`, validate, set `SecurityContext`.

### Task 6.4 — UserDetails
- **Class:** `CustomUserDetailsService` (load `NguoiDung` theo email, ưu tiên fetch `KhuVuc`) + `CustomUserDetails` (wrap entity).

### Task 6.5 — SecurityConfig
- **Class:** `config/SecurityConfig`
- **Cấu hình:**
  - `csrf disable`, `sessionManagement STATELESS`
  - **Public:** `POST /api/auth/**`, `GET /api/products/**`, `GET /api/categories/**`, `GET /api/brands/**`, `GET /api/regions/**`
  - **Cần auth:** mọi route còn lại (giai đoạn sau: giỏ hàng, đơn hàng)
  - Đăng ký `JwtAuthenticationFilter` trước `UsernamePasswordAuthenticationFilter`
  - Bật CORS cho frontend demo
  - 401 trả JSON `INVALID_CREDENTIALS`, 403 trả JSON `ACCESS_DENIED`

### ✅ Checklist Phase 6
- [x] `PasswordEncoder` bean
- [x] `JwtService` sinh/verify token
- [x] `JwtAuthenticationFilter` + `CustomUserDetailsService`
- [x] `SecurityConfig` phân loại public/protected đúng
- [x] Gọi API sản phẩm không cần token; route protected trả 401; route forbidden trả 403

---

## PHASE 7 — Service nghiệp vụ

> **Mục tiêu phase:** Hiện thực các luồng chính. Mỗi service: interface + impl.

### Task 7.1 — AuthService (luồng đăng ký & đăng nhập)
- **Đăng ký** (`@Transactional`):
  1. Nhận `RegisterRequest`
  2. Kiểm tra `UserGlobalIndex` xem email/SĐT đã tồn tại chưa → nếu có ném `DuplicateEmail/PhoneException`
  3. Kiểm tra `maKhuVuc` hợp lệ (tồn tại trong `KhuVuc`)
  4. Tạo `NguoiDung` (hash mật khẩu bằng `PasswordEncoder`)
  5. Tạo bản ghi `UserGlobalIndex` (email, SĐT, maKhuVuc, userId)
  6. Trả `AuthResponse` (token nếu auto-login) hoặc `RegisterResponse`
- **Đăng nhập:**
  1. Tìm `NguoiDung` theo email → không có thì `InvalidCredentialsException`
  2. So khớp mật khẩu (`passwordEncoder.matches`)
  3. Sinh JWT (`JwtService`)
  4. Trả `AuthResponse`

> **Nhấn mạnh báo cáo:** bước 2 đăng ký + bước 5 đăng ký là phần thể hiện **ràng buộc duy nhất toàn cục** qua `User_Global_Index`. Giai đoạn sau, khi có Site Bắc/Nam, bước check này vẫn chạy ở Site Main → đảm bảo không trùng email giữa các khu vực.

### Task 7.2 — UserService
- **Mục tiêu:** Tách thao tác CRUD/đọc `NguoiDung` ra khỏi AuthService (lấy profile...). Giai đoạn đầu tối giản bằng `getProfile(UUID)` trả `UserProfileResponse`.

### Task 7.3 — ProductService
- **Danh sách:** nhận filter (maDanhMuc?, maThuongHieu?, trangThai?) + `Pageable` → trả `PageResponse<ProductListItemResponse>`.
- **Chi tiết:** nhận `maSP` → lấy `SanPhamCore` + `SanPhamDetail` (join fetch) → `ProductDetailResponse`; không có thì `ResourceNotFoundException`.

### Task 7.4 — CategoryService / BrandService / RegionService
- `CategoryService.getAll()` → `List<CategoryResponse>` (phẳng hoặc cây cha-con)
- `BrandService.getAll()` → `List<BrandResponse>`
- `RegionService.getAll()` → `List<RegionResponse>` (phục vụ dropdown chọn khu vực khi đăng ký)

### ✅ Checklist Phase 7
- [x] `AuthService` đăng ký + đăng nhập đúng luồng, có `@Transactional`
- [x] `ProductService` list (phân trang/lọc) + detail
- [x] `CategoryService`, `BrandService`, `RegionService`
- [x] Service ném đúng custom exception ở các nhánh lỗi

---

## PHASE 8 — API endpoint

> **Mục tiêu phase:** Tập endpoint tối thiểu, rõ ràng, dễ test Postman. Tất cả bọc trong `ApiResponse`.

| # | Method | URL | Auth | Request body | Service | Response (data) |
|---|---|---|---|---|---|---|
| 1 | POST | `/api/auth/register` | ❌ | `RegisterRequest` | AuthService | `AuthResponse`/`RegisterResponse` |
| 2 | POST | `/api/auth/login` | ❌ | `LoginRequest` | AuthService | `AuthResponse` |
| 3 | GET | `/api/auth/check-email?email=` | ❌ | — | AuthService | `{ available: true/false }` |
| 4 | GET | `/api/auth/check-phone?phone=` | ❌ | — | AuthService | `{ available: true/false }` |
| 5 | GET | `/api/products?page=&size=&maDanhMuc=&maThuongHieu=` | ❌ | — | ProductService | `PageResponse<ProductListItemResponse>` |
| 6 | GET | `/api/products/{maSP}` | ❌ | — | ProductService | `ProductDetailResponse` |
| 7 | GET | `/api/categories` | ❌ | — | CategoryService | `List<CategoryResponse>` |
| 8 | GET | `/api/brands` | ❌ | — | BrandService | `List<BrandResponse>` |
| 9 | GET | `/api/regions` | ❌ | — | RegionService | `List<RegionResponse>` |

**Controller cần tạo (`controller/`):** `AuthController`, `ProductController`, `CategoryController`, `BrandController`, `RegionController`.

**Response mẫu — đăng nhập (200):**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGciOi...",
    "tokenType": "Bearer",
    "userId": "U001",
    "hoTen": "Nguyen Van A",
    "email": "a@example.com",
    "maKhuVuc": "KV01"
  },
  "timestamp": "2026-06-02T10:00:00"
}
```

**Response mẫu — lỗi trùng email (409):**
```json
{
  "success": false,
  "message": "Email đã được sử dụng",
  "errorCode": "DUPLICATE_EMAIL",
  "details": [],
  "timestamp": "2026-06-02T10:00:00"
}
```

### ✅ Checklist Phase 8
- [x] 5 controller, 9 endpoint hoạt động
- [x] Mọi response bọc `ApiResponse`/`PageResponse`
- [x] Endpoint public không yêu cầu token
- [x] Test nhanh bằng Postman/`.http`

---

## PHASE 9 — Dữ liệu mẫu & kiểm thử

> **Mục tiêu phase:** Có data demo, chạy đủ kịch bản happy-path + lỗi.

### Task 9.1 — Chuẩn bị dữ liệu mẫu (SQL Server)
- **Thứ tự insert (theo phụ thuộc khóa ngoại):**
  1. `KhuVuc` (vd KV01 = Bắc, KV02 = Nam, KV00 = Trung tâm)
  2. `DanhMuc` (vài danh mục, có 1–2 cặp cha-con)
  3. `ThuongHieu`
  4. `DanhMuc_ThuongHieu`
  5. `SanPham_Core` (5–10 sản phẩm, gắn danh mục + thương hiệu)
  6. `SanPham_Detail` (chi tiết cho từng sản phẩm)
  7. (NguoiDung/UserGlobalIndex: nên tạo qua API đăng ký để mật khẩu được hash đúng)
- **File liên quan:** `docs/sample-data.sql` hoặc `src/main/resources/data.sql` (chỉ bật khi cần seed).
- **Lưu ý:** dữ liệu tiếng Việt → cột `NVARCHAR`, chuỗi insert có tiền tố `N'...'`.

### Task 9.2 — Kịch bản test
- **Happy path:**
  - [ ] Đăng ký user mới → 200/201, có bản ghi ở `NguoiDung` + `UserGlobalIndex`
  - [ ] Đăng nhập đúng → nhận token
  - [ ] GET danh sách sản phẩm → có phân trang
  - [ ] GET chi tiết sản phẩm tồn tại → đủ core + detail
  - [ ] GET categories / brands / regions
- **Lỗi:**
  - [ ] Đăng ký trùng email → 409 `DUPLICATE_EMAIL`
  - [ ] Đăng ký trùng SĐT → 409 `DUPLICATE_PHONE`
  - [ ] Đăng nhập sai mật khẩu → 401
  - [ ] GET sản phẩm không tồn tại → 404
  - [ ] Đăng ký thiếu/sai định dạng field → 400 với danh sách lỗi
- **Công cụ:** Postman collection hoặc file `requests.http` (REST Client) lưu trong `docs/`.

### ✅ Checklist Phase 9
- [ ] Script dữ liệu mẫu + thứ tự insert
- [ ] Test happy-path đủ 5 nhóm
- [ ] Test lỗi đủ 5 kịch bản
- [ ] Lưu Postman/`.http` để demo bảo vệ

---

## PHASE 10 — Chuẩn bị giai đoạn sau (liên site)

> **Mục tiêu phase:** Định hướng kiến trúc, KHÔNG code ở giai đoạn này.

- **Site Main giữ vai trò:** xác thực (JWT), dữ liệu dùng chung (sản phẩm/danh mục/thương hiệu/khu vực), **chỉ mục toàn cục** (`UserGlobalIndex`), và **định tuyến** request tới site khu vực.
- **API sẽ chuyển sang site khu vực (Bắc/Nam):** giỏ hàng, đơn hàng, tồn kho, xuất/nhập kho — vì dữ liệu phân mảnh theo khu vực.
- **Cần chuẩn bị trước:**
  - [ ] JWT chứa `maKhuVuc` → site khu vực biết user thuộc đâu (đã set sẵn từ Phase 6/7).
  - [ ] Chuẩn hóa `ApiResponse` để các site dùng chung format.
  - [ ] Tách rõ "dữ liệu dùng chung" (Main) vs "dữ liệu phân mảnh" (khu vực) trong tài liệu thiết kế DB.
  - [ ] Định nghĩa cơ chế định tuyến: dựa vào `maKhuVuc` trong token để gọi đúng Site Bắc/Nam (giai đoạn sau: cấu hình URL từng site, có thể qua RestClient/Feign — KHÔNG cần bây giờ).
- **Nguyên tắc giữ:** Main không xử lý giao dịch phân tán ở giai đoạn này; khi mở rộng mới bàn 2PC / replication — tránh đưa vào sớm.

---

## Thứ tự triển khai khuyến nghị (làm tuần tự)

1. **Phase 1** — chạy được + nối DB ✅ (ưu tiên cao nhất)
2. **Phase 2 + 3** — entity + repository (xương sống dữ liệu)
3. **Phase 4 + 5** — DTO + exception (khung trả về)
4. **Phase 7 (Product/Category/Brand/Region) + Phase 8 (GET endpoints)** — làm **luồng đọc sản phẩm trước** vì public, dễ test, không phụ thuộc security
5. **Phase 6** — security + JWT
6. **Phase 7 (AuthService) + Phase 8 (auth endpoints)** — đăng ký/đăng nhập
7. **Phase 9** — seed data + test toàn bộ
8. **Phase 10** — chốt định hướng, ghi vào báo cáo

> Mẹo demo: làm xong bước 4 là đã có thể mở Postman gọi `GET /api/products` để thầy/cô thấy hệ thống "sống" sớm, tạo động lực và dễ kiểm thử dần.

---

## Tổng hợp class/file cần tạo (skeleton)

```
config/           SecurityConfig, CorsConfig(optional)
common/           ApiResponse, ErrorResponse, PageResponse
exception/        GlobalExceptionHandler, DuplicateEmailException,
                  DuplicatePhoneException, InvalidCredentialsException,
                  ResourceNotFoundException
security/         JwtService, JwtAuthenticationFilter,
                  CustomUserDetailsService, CustomUserDetails
domain/entity/    KhuVuc, DanhMuc, ThuongHieu,
                  SanPhamCore, SanPhamDetail, NguoiDung, UserGlobalIndex
repository/       KhuVucRepository, DanhMucRepository, ThuongHieuRepository,
                  SanPhamCoreRepository, SanPhamDetailRepository,
                  NguoiDungRepository, UserGlobalIndexRepository
dto/request/      RegisterRequest, LoginRequest
dto/response/     AuthResponse, RegisterResponse, ProductListItemResponse,
                  ProductDetailResponse, CategoryResponse, BrandResponse,
                  RegionResponse
service/ + impl/  AuthService, UserService, ProductService,
                  CategoryService, BrandService, RegionService
controller/       AuthController, ProductController, CategoryController,
                  BrandController, RegionController
```

---

*Tài liệu kế hoạch — chưa bao gồm code. Bước tiếp theo: bắt đầu Phase 1, Task 1.1.*
