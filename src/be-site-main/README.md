# be-site-main — Site trung tâm (Coordinator)

Backend Spring Boot cho **site trung tâm** của hệ thống bán hàng đa kho phân tán. Đây là site
điều phối: quản lý danh mục dùng chung, xác thực người dùng, các chức năng quản trị (admin) và
**tổng hợp báo cáo thống kê xuyên site** thông qua *linked server* của SQL Server.

> Xem README tổng ở thư mục gốc dự án để biết bức tranh toàn hệ thống (3 site + frontend).

---

## Vai trò trong hệ thống phân tán

- Có **CSDL SQL Server riêng** (`store_management`) chứa **bản nhân bản** của danh mục dùng chung
  (`SanPham_Core`, `SanPham_Detail`, `DanhMuc`, `ThuongHieu`, `KhuVuc`) và bảng **chỉ mục toàn
  cục** `User_Global_Index` (tra email/SĐT → khu vực để định tuyến đăng nhập về đúng trạm).
- Các báo cáo thống kê (`ThongKeRepository`) chạy **distributed query**: từ DB trung tâm gọi sang
  linked server `[SITE_BAC]`, `[SITE_NAM]` rồi `UNION ALL` để gộp doanh thu / đơn hàng / tồn kho
  toàn hệ thống.

---

## Công nghệ

Java 21 · Spring Boot 4.x · Spring Data JPA · Spring Security (JWT) · springdoc-openapi ·
SQL Server (mssql-jdbc) · Gradle (Kotlin DSL) · Lombok.

---

## Cấu trúc package

```
src/main/java/csdlpt/sitemain/
├── config/        # SecurityConfig, OpenApiConfig (Swagger), CORS
├── common/        # ApiResponse<T>, ErrorResponse, PageResponse, ErrorCodes, converter
├── exception/     # GlobalExceptionHandler + các exception nghiệp vụ
├── security/      # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── domain/
│   ├── entity/    # NguoiDung, SanPhamCore, SanPhamDetail, DanhMuc, ThuongHieu,
│   │              # KhuVuc, UserGlobalIndex
│   ├── enums/     # VaiTro, TrangThaiDonHang, TrangThaiThanhToan
│   └── converter/ # BooleanToTinyIntConverter (boolean ↔ TINYINT)
├── dto/
│   ├── request/   # LoginRequest, RegisterRequest, ProductUpsertRequest, ThongKeDoanhThuFilter...
│   ├── response/  # AuthResponse, ProductDetailResponse, DoanhThu*Response...
│   └── projection/# ProductListItemView
├── repository/    # JPA repo + ThongKeRepository (JdbcTemplate/SimpleJdbcCall — distributed query)
├── service/       # AuthService, BrandService, CategoryService, DonHangService, ThongKeService...
│   └── impl/
└── controller/    # REST controller (xem bảng API)
```

---

## API

| Nhóm | Endpoint | Mô tả |
|---|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/check-email`, `GET /api/auth/check-phone` | Đăng ký / đăng nhập (JWT) |
| User | `GET/PUT /api/users/me`, `PUT /api/users/me/mat-khau` | Hồ sơ & đổi mật khẩu |
| Catalog (công khai) | `GET /api/products`, `/api/products/{maSP}`, `/api/products/{maSP}/ton-kho`, `/api/categories`, `/api/brands`, `/api/regions` | Đọc danh mục |
| Admin · Danh mục | `POST/PUT/DELETE /api/products`, `/api/categories`, `/api/brands` | CRUD |
| Admin · Người dùng | `GET /api/admin/users`, `PUT /api/admin/users/{id}`, `PATCH .../vai-tro`, `PATCH .../khoiphuc`, `DELETE .../{id}` | Quản lý người dùng |
| Admin · Đơn hàng | `GET /api/admin/orders`, `GET .../{id}`, `PATCH .../{id}/trang-thai` | Theo dõi & cập nhật đơn |
| Admin · Thống kê | `GET /api/admin/thong-ke/doanh-thu`, `/doanh-thu-theo-thang`, `/san-pham-ban-chay`, `/don-hang-nhieu-kho` | **Báo cáo tổng hợp xuyên site** |

Mọi response bọc trong `ApiResponse<T>` (thành công) hoặc `ErrorResponse` (lỗi, mã trong
`ErrorCodes`). Swagger UI: `http://localhost:8080/swagger-ui.html`.

---

## Chạy local

```bash
# Sao chép & điền cấu hình dev (nếu có file mẫu)
# cp src/main/resources/application-dev.example.yml src/main/resources/application-dev.yml

./gradlew bootRun          # Windows: .\gradlew.bat bootRun
./gradlew build            # build + test
./gradlew test             # chỉ test
```

Biến môi trường (hoặc đặt trong `application-dev.yml`):

| Biến | Ý nghĩa | Mặc định |
|---|---|---|
| `DB_URL` | JDBC URL SQL Server | `jdbc:sqlserver://localhost:1433;databaseName=store_management;encrypt=true;trustServerCertificate=true` |
| `DB_USERNAME` / `DB_PASSWORD` | Tài khoản CSDL | `sa` / *(rỗng)* |
| `JWT_SECRET` | Khóa JWT (≥ 32 ký tự) | *(đổi trước khi chạy thật)* |
| `JWT_EXPIRATION_MS` | Thời hạn token | `31536000000` (1 năm, dev) |
| `SERVER_PORT` | Cổng | `8080` |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Origin được phép | `*` |

> JPA đặt `ddl-auto: none` — **schema phải tạo trước**. Dùng
> `../be-site-tram/src/main/resources/schema.sql` và cấu hình linked server `[SITE_BAC]`/`[SITE_NAM]`
> trước khi gọi các API thống kê.

### Docker

```bash
docker compose -f docker-compose.backend.yml up -d --build
```

---

## Bảo mật

- `JwtAuthenticationFilter` chạy trước mọi request.
- Công khai: Swagger/OpenAPI, `/error`, `/api/auth/**`, các `GET` danh mục.
- Còn lại yêu cầu Bearer token; `@EnableMethodSecurity` phân quyền theo `VaiTro`
  (`ADMIN` / `WAREHOUSE_STAFF` / `USER`).
