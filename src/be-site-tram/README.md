# be-site-tram — Site trạm theo khu vực (Regional)

Backend Spring Boot cho **site trạm** của hệ thống bán hàng đa kho phân tán. Cùng một codebase
được **triển khai 2 lần** — một bản cho Miền Bắc, một bản cho Miền Nam — mỗi bản kết nối tới CSDL
SQL Server của miền đó. Trạm xử lý toàn bộ nghiệp vụ cục bộ: giỏ hàng, đặt hàng, tồn kho, phiếu
xuất/nhập kho.

> Xem README tổng ở thư mục gốc dự án để biết bức tranh toàn hệ thống (3 site + frontend).

---

## Vai trò trong hệ thống phân tán

- Mỗi trạm sở hữu **mảnh dữ liệu cục bộ**: `NguoiDung` (theo `MaKV`), `DonHang`, `ChiTietDonHang`,
  `Kho`, `TonKho`, `PhieuXuatKho`/`ChiTietXuatKho`, `PhieuNhapKho`/`ChiTietPhieuNhap`.
- Danh mục dùng chung (`SanPham_Core`, `DanhMuc`, `ThuongHieu`, `KhuVuc`) là **bản nhân bản**,
  giống nhau ở mọi site.
- Nghiệp vụ phức tạp đặt trong **stored procedure** SQL Server (xem mục dưới): chọn kho tối ưu,
  trừ tồn khi đặt hàng, tạo & xác nhận phiếu xuất (nội bộ / giao khách), nhập-chuyển hàng liên kho.
- Khóa ngoại **liên-site bị bỏ** (thay bằng khóa logic) trong `schema.sql`; FK nội-site được giữ.

---

## Công nghệ

Java 21 · Spring Boot 4.x · Spring Data JPA · Spring Security (JWT) · springdoc-openapi ·
SQL Server (mssql-jdbc, stored procedure) · Gradle (Kotlin DSL) · Lombok.

---

## Cấu trúc package

```
src/main/java/csdlpt/sitemain/
├── config/        # SecurityConfig, OpenApiConfig (Swagger), CORS
├── common/        # ApiResponse<T>, ErrorResponse, PageResponse, ErrorCodes, converter
├── exception/     # GlobalExceptionHandler, SqlServerErrorTranslator + exception nghiệp vụ
├── security/      # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── domain/
│   ├── entity/    # NguoiDung, SanPham*, DanhMuc, ThuongHieu, KhuVuc,
│   │              # GioHang, ChiTietGioHang, DonHang, ChiTietDonHang
│   └── enums/     # VaiTro
├── dto/ (request, response, projection)
├── repository/    # JPA repo + OrderStoredProcedureDao, WarehouseStoredProcedureDao,
│                  # WarehouseQueryRepository (gọi stored procedure)
├── service/       # GioHangService, OrderService, ProductService, RegionService...
│   └── impl/
└── controller/    # REST controller (xem bảng API)
```

### Tài nguyên SQL (`src/main/resources`)

```
schema.sql                       # Lược đồ đầy đủ (18 bảng) — chạy trước khi khởi động
procedure.sql                    # sp_KiemTraTonKho_ToanHeThong...
static/db/proceduce/
├── chonkhotoiuu.sql             # sp_ChonKhoNhan_ToiUu — chọn kho gom hàng tối ưu
├── taodon.sql                   # tạo đơn + trừ tồn (SoLuongDatHang)
├── taophieuxuat*.sql            # tạo phiếu xuất (nội bộ / giao khách / JSON)
├── xacnhanxuat*.sql             # xác nhận xuất kho
└── taophieumhapnoibo.sql, xacnhapxuatnoibo.sql  # nhập/chuyển hàng nội bộ
```

---

## API

| Nhóm | Endpoint | Mô tả |
|---|---|---|
| Catalog | `GET /api/products`, `/api/products/{maSP}`, `/api/products/{maSP}/ton-kho`, `/api/categories`, `/api/brands`, `/api/regions` | Đọc danh mục cục bộ |
| Giỏ hàng | `GET /api/cart`, `POST /api/cart/items`, `PUT/DELETE /api/cart/items/{maSP}`, `DELETE /api/cart` | Quản lý giỏ (cần đăng nhập) |
| Đơn hàng | `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/xac-nhan-nhan-hang` | Đặt & theo dõi đơn |
| Kho (staff) | `GET /api/warehouse/me`, `/dashboard`, `/ton-kho` | Bối cảnh & dashboard kho |
| Phiếu xuất | `GET /api/warehouse/phieu-xuat`, `.../{id}`, `POST .../xac-nhan-noi-bo`, `POST .../xac-nhan-giao-khach` | Xử lý phiếu xuất |
| Phiếu nhập | `GET /api/warehouse/phieu-nhap`, `.../{id}`, `POST .../xac-nhan` | Xử lý phiếu nhập |
| Giao khách | `GET /api/warehouse/orders/ready-to-ship`, `POST /api/warehouse/orders/{id}/tao-phieu-giao-khach` | Tạo phiếu giao khách |

Mọi response bọc trong `ApiResponse<T>` / `ErrorResponse`. Swagger UI: `http://localhost:8080/swagger-ui.html`.

---

## Chạy local

```bash
# cp src/main/resources/application-dev.example.yml src/main/resources/application-dev.yml

./gradlew bootRun          # Windows: .\gradlew.bat bootRun
./gradlew build            # build + test
./gradlew test             # chỉ test
./gradlew check            # lint + test
```

Để chạy **cả hai trạm** trên một máy, mở 2 tiến trình với `DB_URL` và `SERVER_PORT` khác nhau
(ví dụ Bắc → port 8081, Nam → port 8082).

Biến môi trường:

| Biến | Ý nghĩa | Mặc định |
|---|---|---|
| `DB_URL` | JDBC URL SQL Server của miền | — |
| `DB_USERNAME` / `DB_PASSWORD` | Tài khoản CSDL | — |
| `JWT_SECRET` | Khóa JWT (≥ 32 ký tự) | — |
| `JWT_EXPIRATION_MS` | Thời hạn token | `86400000` (24h) |
| `SERVER_PORT` | Cổng | `8080` |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Origin được phép | `*` |

> **Chuẩn bị CSDL trước khi chạy**: tạo `store_management`, chạy `schema.sql`, rồi nạp toàn bộ
> stored procedure trong `procedure.sql` và `static/db/proceduce/`. JPA không tự sinh bảng.

### Docker

```bash
docker compose -f docker-compose.backend.yml up -d --build
```

---

## Bảo mật

- `JwtAuthenticationFilter` chạy trước mọi request.
- Công khai: Swagger/OpenAPI, `/error`, các `GET` danh mục.
- Giỏ hàng, đơn hàng, kho yêu cầu Bearer token; `@EnableMethodSecurity` phân quyền theo `VaiTro`
  (`USER` cho mua hàng, `WAREHOUSE_STAFF` cho nghiệp vụ kho).
