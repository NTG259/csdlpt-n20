# Hệ thống bán hàng đa kho phân tán — CSDL Phân Tán (Nhóm 20)

Bài tập lớn môn **Cơ sở dữ liệu phân tán**: xây dựng một hệ thống thương mại điện tử bán hàng
nhiều kho, dữ liệu được **phân mảnh ngang theo khu vực địa lý** (Miền Bắc / Miền Nam) và
đặt trên các site SQL Server khác nhau. Mỗi miền tự xử lý đơn hàng, tồn kho, xuất/nhập kho
của mình; site trung tâm chịu trách nhiệm danh mục dùng chung, quản trị và **tổng hợp dữ
liệu xuyên site** thông qua *linked server*.

> Tài liệu báo cáo đầy đủ: `BÁO CÁO BTL N20 bản chính.docx` / `.pdf` ở thư mục gốc.

---

## 1. Tổng quan kiến trúc

```
                          ┌──────────────────────────────┐
                          │        Frontend (Next.js)     │
                          │   src/fe  — port 3000          │
                          └───────────────┬───────────────┘
              MAIN_API_URL │   NORTH_API_URL │ SOUTH_API_URL
        ┌──────────────────┘                 │            └──────────────────┐
        ▼                                     ▼                               ▼
┌─────────────────┐              ┌─────────────────────┐         ┌─────────────────────┐
│   be-site-main   │             │  be-site-tram (Bắc) │         │  be-site-tram (Nam) │
│  Site trung tâm  │             │   Trạm Miền Bắc      │         │   Trạm Miền Nam      │
│  - Danh mục      │             │  - Giỏ hàng          │         │  - Giỏ hàng          │
│  - Quản trị      │             │  - Đặt hàng          │         │  - Đặt hàng          │
│  - Thống kê toàn │             │  - Kho / xuất / nhập │         │  - Kho / xuất / nhập │
│    hệ thống      │             └──────────┬──────────┘         └──────────┬──────────┘
└────────┬────────┘                         │                              │
         ▼                                   ▼                              ▼
┌──────────────────┐              ┌──────────────┐               ┌──────────────┐
│   SQL Server     │              │ SQL Server   │               │ SQL Server   │
│   store_mgmt     │              │ store_mgmt   │               │ store_mgmt   │
│  (site trung tâm:│◄──┐          │ (mảnh Bắc)   │               │ (mảnh Nam)   │
│   danh mục nhân  │   │          └──────▲───────┘               └──────▲───────┘
│   bản + Global   │   │ Linked Server   │                              │
│   Index)         │   └─────────────────┴──────────────────────────────┘
└──────────────────┘     [SITE_BAC] / [SITE_NAM]  (distributed query — thống kê)
```

- **`be-site-main`** — *Site trung tâm (coordinator)*. Có **CSDL SQL Server riêng** (`store_management`)
  chứa bản nhân bản của danh mục dùng chung và `User_Global_Index`. Phục vụ danh mục dùng chung
  (sản phẩm, danh mục, thương hiệu, khu vực), xác thực/đăng ký người dùng, các API quản trị
  (admin) và các **báo cáo thống kê tổng hợp toàn hệ thống**. Truy vấn thống kê dùng *distributed
  query* của SQL Server: từ DB trung tâm gọi sang linked server `[SITE_BAC]`, `[SITE_NAM]` rồi
  `UNION ALL` kết quả.
- **`be-site-tram`** — *Site trạm (regional)*. Cùng một codebase được triển khai **2 lần**, mỗi
  bản kết nối tới một CSDL miền. Xử lý nghiệp vụ cục bộ theo miền: giỏ hàng, đặt hàng, tồn kho,
  phiếu xuất/nhập kho. Logic phức tạp (chọn kho tối ưu, trừ tồn, giao hàng liên kho) nằm trong
  các **stored procedure** SQL Server.
- **`fe`** — *Frontend Next.js 16 (App Router)*. Tự định tuyến request: gọi `MAIN_API` cho danh
  mục/quản trị, và gọi `NORTH_API`/`SOUTH_API` tương ứng với khu vực của người dùng cho giỏ
  hàng/đơn hàng/kho.

### Chiến lược phân tán dữ liệu

| Bảng | Chiến lược | Ghi chú |
|---|---|---|
| `SanPham_Core`, `SanPham_Detail`, `DanhMuc`, `ThuongHieu`, `KhuVuc` | **Nhân bản toàn phần** | Danh mục dùng chung, giống nhau ở mọi site |
| `NguoiDung` | **Phân mảnh ngang** theo `MaKV` (Bắc/Nam) | Mỗi người dùng thuộc về một miền |
| `User_Global_Index` | **Chỉ mục toàn cục** | Bảng tra cứu email/SĐT → khu vực, đảm bảo định tuyến đăng nhập về đúng site |
| `DonHang`, `ChiTietDonHang` | **Phân mảnh ngang / dẫn xuất** theo miền xử lý | Đơn nằm tại site của miền |
| `Kho`, `TonKho`, `PhieuXuatKho`, `PhieuNhapKho`, ... | **Phân mảnh ngang** theo kho/miền | Tồn kho cục bộ từng site |

Trong `schema.sql`, các khóa ngoại liên-site **được cố tình bỏ** (thay bằng khóa logic) và có
ghi chú rõ ràng (`-- Không tạo FK vì ... có thể nằm ở site khác`), trong khi FK nội-site vẫn
được giữ. Đây là điểm cốt lõi của thiết kế phân mảnh.

---

## 2. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 4.x, Spring Data JPA, Spring Security (JWT), Gradle (Kotlin DSL) |
| Cơ sở dữ liệu | Microsoft SQL Server (mỗi site một instance) + Linked Server |
| API docs | springdoc-openapi (Swagger UI) |
| Frontend | Next.js 16 (App Router), React 19, TypeScript, TanStack Query, React Hook Form + Zod, Tailwind CSS v4, shadcn/ui |
| Đóng gói | Docker + docker-compose (riêng cho từng dịch vụ) |

---

## 3. Cấu trúc thư mục

```
csdlpt-n20/
├── BÁO CÁO BTL N20 bản chính.docx / .pdf   # Báo cáo bài tập lớn
├── Dữ liệu mẫu/                            # Dữ liệu mẫu để seed CSDL
│   ├── 1. Khu vực - 2 bản ghi.txt
│   ├── 2. Người dùng - 1000 bản ghi.txt
│   ├── 3. Kho - 10 bản ghi.txt
│   ├── 4. Danh mục - Thương hiệu.txt
│   ├── 5. Sản phẩm - 20 bản ghi.txt
│   ├── 6. Tồn kho - 200 bản ghi.txt
│   └── Chạy theo số thứ tự.txt
└── src/
    ├── be-site-main/    # Backend site trung tâm
    ├── be-site-tram/    # Backend site trạm (deploy 2 lần: Bắc & Nam)
    └── fe/              # Frontend Next.js
```

### 3.1. Backend — cấu trúc package (cả 2 site)

```
src/main/java/csdlpt/sitemain/
├── config/        # SecurityConfig, OpenApiConfig (Swagger), CORS
├── common/        # ApiResponse<T>, ErrorResponse, PageResponse, ErrorCodes, converter
├── exception/     # GlobalExceptionHandler + các exception nghiệp vụ
├── security/      # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── domain/
│   ├── entity/    # JPA entity (đặt tên tiếng Việt: NguoiDung, SanPhamCore, DonHang...)
│   ├── enums/     # VaiTro, TrangThaiDonHang, TrangThaiThanhToan
│   └── converter/ # BooleanToTinyIntConverter (boolean ↔ TINYINT của SQL Server)
├── dto/
│   ├── request/   # DTO đầu vào
│   ├── response/  # DTO đầu ra
│   └── projection/# Projection tối ưu truy vấn
├── repository/    # Spring Data JPA repo + JdbcTemplate/SimpleJdbcCall cho SP & distributed query
├── service/       # Interface nghiệp vụ
│   └── impl/      # Cài đặt
└── controller/    # REST controller
```

### 3.2. Frontend — cấu trúc (`src/fe/src`)

```
app/                       # Next.js App Router
├── (auth)/login, register
├── products, products/[maSP], cart, orders
├── admin/                 # brands, categories, products, orders,
│                          # doanh-thu-theo-thang, san-pham-ban-chay, don-hang-nhieu-kho
└── warehouse/             # ton-kho, phieu-nhap, phieu-xuat, giao-khach
features/                  # Logic theo miền nghiệp vụ (auth, cart, orders, products, admin, warehouse)
components/ (ui, shared)   # shadcn/ui + component dùng chung
lib/
├── main-api-client.ts     # Gọi site trung tâm (NEXT_PUBLIC_MAIN_API_URL)
├── regional-api-client.ts # Gọi site trạm theo khu vực (NORTH/SOUTH_API_URL)
├── api-fetch.ts           # Wrapper fetch + xử lý lỗi/token
└── auth-storage.ts, auth-events.ts, query-client.ts, format.ts
constants/ , types/
```

---

## 4. Mô hình dữ liệu (lược đồ chính)

Lược đồ đầy đủ: [`src/be-site-tram/src/main/resources/schema.sql`](src/be-site-tram/src/main/resources/schema.sql).

| Bảng | Ý nghĩa |
|---|---|
| `KhuVuc` | Khu vực (`Bac`, `Nam`) |
| `Kho` | Kho hàng, thuộc một khu vực, có sức chứa |
| `DanhMuc` | Danh mục sản phẩm (tự tham chiếu cây cha-con) |
| `ThuongHieu` / `DanhMuc_ThuongHieu` | Thương hiệu và quan hệ N-N với danh mục |
| `SanPham_Core` / `SanPham_Detail` | Lõi sản phẩm + chi tiết (mô tả, thông số kỹ thuật dạng JSON) |
| `NguoiDung` | Người dùng (UUID PK, vai trò, khu vực, kho phụ trách) |
| `User_Global_Index` | Chỉ mục toàn cục email/SĐT → khu vực |
| `GioHang` / `ChiTietGioHang` | Giỏ hàng (mỗi user 1 giỏ `active`) |
| `DonHang` / `ChiTietDonHang` | Đơn hàng + dòng chi tiết (`ThanhTien` cột tính sẵn) |
| `PhieuXuatKho` / `ChiTietXuatKho` | Phiếu xuất (giao khách hoặc chuyển kho liên site) |
| `PhieuNhapKho` / `ChiTietPhieuNhap` | Phiếu nhập (nhập nội bộ giữa các kho) |
| `TonKho` | Tồn kho theo (Kho, SP); có `SoLuongDatHang` và `RowVer` (rowversion) để kiểm soát đồng thời |

**Vai trò người dùng** (`VaiTro`): `ADMIN`, `WAREHOUSE_STAFF`, `USER`.


---

## 5. Tổng hợp API

Tất cả endpoint trả về `ApiResponse<T>` khi thành công, `ErrorResponse` khi lỗi (mã lỗi tập
trung trong `ErrorCodes`). Swagger UI: `http://localhost:8080/swagger-ui.html`.

### 5.1. Site trung tâm (`be-site-main`)

| Nhóm | Endpoint | Mô tả |
|---|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/check-email`, `GET /api/auth/check-phone` | Đăng ký / đăng nhập (JWT) |
| User | `GET/PUT /api/users/me`, `PUT /api/users/me/mat-khau` | Hồ sơ & đổi mật khẩu |
| Catalog | `GET /api/products`, `/api/products/{maSP}`, `/api/products/{maSP}/ton-kho`, `/api/categories`, `/api/brands`, `/api/regions` | Đọc danh mục (công khai) |
| Admin · Sản phẩm | `POST/PUT/DELETE /api/products`, `/api/categories`, `/api/brands` | CRUD danh mục |
| Admin · Người dùng | `GET /api/admin/users`, `PUT /api/admin/users/{id}`, `PATCH .../vai-tro`, `PATCH .../khoiphuc`, `DELETE .../{id}` | Quản lý người dùng |
| Admin · Đơn hàng | `GET /api/admin/orders`, `GET .../{id}`, `PATCH .../{id}/trang-thai` | Theo dõi & cập nhật đơn |
| Admin · Thống kê | `GET /api/admin/thong-ke/doanh-thu`, `/doanh-thu-theo-thang`, `/san-pham-ban-chay`, `/don-hang-nhieu-kho` | **Báo cáo tổng hợp xuyên site** |

### 5.2. Site trạm (`be-site-tram`, mỗi miền)

| Nhóm | Endpoint | Mô tả |
|---|---|---|
| Catalog | `GET /api/products`, `/api/products/{maSP}`, `/api/products/{maSP}/ton-kho`, `/api/categories`, `/api/brands`, `/api/regions` | Đọc danh mục (cục bộ) |
| Giỏ hàng | `GET /api/cart`, `POST /api/cart/items`, `PUT/DELETE /api/cart/items/{maSP}`, `DELETE /api/cart` | Quản lý giỏ (yêu cầu đăng nhập) |
| Đơn hàng | `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/xac-nhan-nhan-hang` | Đặt & theo dõi đơn |
| Kho (staff) | `GET /api/warehouse/me`, `/dashboard`, `/ton-kho` | Bối cảnh & dashboard kho |
| Phiếu xuất | `GET /api/warehouse/phieu-xuat`, `.../{id}`, `POST .../xac-nhan-noi-bo`, `POST .../xac-nhan-giao-khach` | Xử lý phiếu xuất |
| Phiếu nhập | `GET /api/warehouse/phieu-nhap`, `.../{id}`, `POST .../xac-nhan` | Xử lý phiếu nhập |
| Giao khách | `GET /api/warehouse/orders/ready-to-ship`, `POST /api/warehouse/orders/{id}/tao-phieu-giao-khach` | Tạo phiếu giao khách |

---

## 6. Hướng dẫn cài đặt & chạy

### Yêu cầu
- JDK 21
- Microsoft SQL Server (tối thiểu 1 instance cho dev; lý tưởng là 3: trung tâm + Bắc + Nam)
- Node.js / Bun (frontend dùng `bun.lock`)
- (Tùy chọn) Docker & docker-compose

### 6.1. Chuẩn bị cơ sở dữ liệu

1. Tạo database `store_management` trên mỗi instance SQL Server.
2. Chạy lược đồ: [`src/be-site-tram/src/main/resources/schema.sql`](src/be-site-tram/src/main/resources/schema.sql).
3. Tạo stored procedure: chạy các script trong `src/be-site-tram/src/main/resources/static/db/proceduce/`
   và `procedure.sql`.
4. Cấu hình **Linked Server** `[SITE_BAC]` và `[SITE_NAM]` trên instance trung tâm để các truy
   vấn thống kê (`ThongKeRepository`) hoạt động.
5. Nạp dữ liệu mẫu từ thư mục `Dữ liệu mẫu/` theo thứ tự đánh số (Khu vực → Người dùng → Kho →
   Danh mục/Thương hiệu → Sản phẩm → Tồn kho).

> JPA được đặt `ddl-auto: none/validate` — **schema phải tạo thủ công trước**, Hibernate không
> tự sinh bảng.

### 6.2. Chạy backend

Mỗi site là một ứng dụng Spring Boot độc lập. Sao chép
`src/main/resources/application-dev.example.yml` → `application-dev.yml` rồi điền giá trị, hoặc
truyền qua biến môi trường:

| Biến | Ý nghĩa |
|---|---|
| `DB_URL` | JDBC URL SQL Server, ví dụ `jdbc:sqlserver://localhost:1433;databaseName=store_management;encrypt=true;trustServerCertificate=true` |
| `DB_USERNAME` / `DB_PASSWORD` | Tài khoản CSDL |
| `JWT_SECRET` | Khóa JWT (≥ 32 ký tự) |
| `JWT_EXPIRATION_MS` | Thời hạn token (mặc định 24h ở trạm, 1 năm ở site-main dev) |
| `SERVER_PORT` | Cổng (mặc định `8080`) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Origin được phép (mặc định `*`) |

```bash
# Site trung tâm
cd src/be-site-main
./gradlew bootRun        # Windows: .\gradlew.bat bootRun

# Site trạm (mở 2 process, mỗi process trỏ DB_URL & SERVER_PORT khác nhau cho Bắc / Nam)
cd src/be-site-tram
./gradlew bootRun
```

Lệnh Gradle hữu ích: `./gradlew build` (kèm test), `./gradlew test`, `./gradlew check`.

### 6.3. Chạy frontend

```bash
cd src/fe
cp .env.frontend.example .env.local   # rồi điền các URL backend
bun install                            # hoặc npm install
bun run dev                            # http://localhost:3000
```

Biến môi trường frontend:

| Biến | Ý nghĩa |
|---|---|
| `NEXT_PUBLIC_MAIN_API_URL` | URL site trung tâm (danh mục, quản trị, thống kê) |
| `NEXT_PUBLIC_NORTH_API_URL` | URL site trạm Miền Bắc |
| `NEXT_PUBLIC_SOUTH_API_URL` | URL site trạm Miền Nam |

### 6.4. Chạy bằng Docker

Mỗi dịch vụ có `Dockerfile` và `docker-compose` riêng:

```bash
# Backend (chạy trong từng thư mục site, truyền .env tương ứng)
cd src/be-site-main && docker compose -f docker-compose.backend.yml up -d --build
cd src/be-site-tram && docker compose -f docker-compose.backend.yml up -d --build

# Frontend
cd src/fe && docker compose up -d --build
```

---

## 7. Bảo mật

- Xác thực bằng **JWT Bearer token**; `JwtAuthenticationFilter` chạy trước mọi request.
- Route công khai: Swagger/OpenAPI, `/error`, các `GET` danh mục (`/api/products`,
  `/api/categories`, `/api/brands`, `/api/regions`) và `/api/auth/**` (ở site trung tâm).
- Mọi route còn lại (giỏ hàng, đơn hàng, kho, admin) yêu cầu token hợp lệ.
- `@EnableMethodSecurity` bật sẵn để phân quyền theo `VaiTro` (`ADMIN` / `WAREHOUSE_STAFF` /
  `USER`).
- CORS mặc định cho phép `localhost:3000`, `5173`, `4173` (cấu hình qua
  `CORS_ALLOWED_ORIGIN_PATTERNS`).

---

## 8. Quy ước

- **Đặt tên domain bằng tiếng Việt** (NguoiDung, SanPham, DonHang, TonKho...) để bám sát đề bài.
- UUID dùng `@JdbcTypeCode(SqlTypes.UUID)`; `boolean` ↔ SQL Server `TINYINT` qua
  `BooleanToTinyIntConverter`; quan hệ `@ManyToOne`/`@OneToOne` dùng `LAZY` fetch.
- Jackson bỏ field `null`, múi giờ `Asia/Ho_Chi_Minh`, encoding UTF-8 toàn cục.

---

*Bài tập lớn môn Cơ sở dữ liệu phân tán — Nhóm 20.*
