# SiteMain API — Tài liệu cho Frontend

Tài liệu mô tả cấu trúc chung và chi tiết toàn bộ API hiện có của hệ thống SiteMain.

- **Base URL (dev):** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Định dạng:** `application/json`, charset `UTF-8`
- **Múi giờ timestamp:** `Asia/Ho_Chi_Minh`

---

## 1. Cấu trúc response chung

Mọi response đều được bọc trong một cấu trúc thống nhất. FE chỉ cần xử lý 2 dạng: **thành công** và **lỗi**.

### 1.1. Response thành công (`ApiResponse<T>`)

```jsonc
{
  "success": true,
  "message": "OK",                  // hoặc thông điệp riêng, ví dụ "Đăng nhập thành công"
  "data": { /* T — payload thực tế, có thể là object, mảng, hoặc trang phân trang */ },
  "timestamp": "2026-06-02T03:19:34.5337247"
}
```

> Lưu ý: các field có giá trị `null` **bị lược bỏ** khỏi JSON (cấu hình `non_null`). FE không nên giả định mọi field luôn xuất hiện.

### 1.2. Response lỗi (`ErrorResponse`)

```jsonc
{
  "success": false,
  "message": "Dữ liệu đầu vào không hợp lệ",   // thông điệp hiển thị được cho người dùng
  "errorCode": "VALIDATION_ERROR",             // mã lỗi máy đọc — dùng để rẽ nhánh xử lý
  "details": [                                  // danh sách lỗi chi tiết (chủ yếu cho validation)
    "email: Email không đúng định dạng",
    "matKhau: Mật khẩu phải từ 6 đến 72 ký tự"
  ],
  "timestamp": "2026-06-02T03:18:24.0843673"
}
```

**Quy ước cho FE:**
- Dựa vào **HTTP status** để biết nhóm kết quả; dựa vào **`errorCode`** để xử lý logic cụ thể.
- `message` luôn an toàn để hiển thị cho người dùng (đã được Việt hoá).
- `details` rỗng `[]` với hầu hết lỗi nghiệp vụ; chỉ đầy đủ khi `errorCode = VALIDATION_ERROR`.

### 1.3. Phân biệt success/error

FE nên kiểm tra theo thứ tự: HTTP status code → field `success`. Khi `success = true` luôn có `data`; khi `false` luôn có `errorCode`.

---

## 2. Bảng mã lỗi (`errorCode`)

| errorCode            | HTTP status | Ý nghĩa                                              | Khi nào gặp |
|----------------------|-------------|-----------------------------------------------------|-------------|
| `VALIDATION_ERROR`   | 400         | Dữ liệu đầu vào không hợp lệ                         | Sai định dạng body / query param; xem `details` |
| `INVALID_CREDENTIALS`| 401         | Sai thông tin đăng nhập hoặc thiếu/het hạn token    | Login sai; gọi API cần auth mà không có token hợp lệ |
| `ACCESS_DENIED`      | 403         | Không đủ quyền truy cập                              | Token hợp lệ nhưng không đủ vai trò |
| `RESOURCE_NOT_FOUND` | 404         | Không tìm thấy tài nguyên                            | Ví dụ chi tiết sản phẩm với mã không tồn tại |
| `DUPLICATE_EMAIL`    | 409         | Email đã tồn tại                                     | Đăng ký với email đã dùng |
| `DUPLICATE_PHONE`    | 409         | Số điện thoại đã tồn tại                             | Đăng ký với SĐT đã dùng |
| `INVALID_REGION`     | 400/409     | Mã khu vực không hợp lệ                              | Đăng ký với `maKhuVuc` không tồn tại |
| `INTERNAL_ERROR`     | 500         | Lỗi nội bộ                                           | Lỗi không lường trước; FE hiển thị "thử lại sau" |

---

## 3. Xác thực (Authentication)

Hệ thống dùng **JWT Bearer token**, stateless (không session/cookie).

### 3.1. Luồng

1. FE gọi `POST /api/auth/login` (hoặc `register`) → nhận `token`.
2. Với mọi request cần xác thực, đính kèm header:
   ```
   Authorization: Bearer <token>
   ```
3. Token hết hạn (mặc định **1 năm** = `expiresIn: 31536000` giây) → API trả `401 INVALID_CREDENTIALS` → FE điều hướng về đăng nhập.

### 3.2. Endpoint công khai (KHÔNG cần token)

- `POST /api/auth/**` (register, login)
- `GET /api/auth/check-email`, `GET /api/auth/check-phone`
- `GET /api/products/**`, `GET /api/categories/**`, `GET /api/brands/**`, `GET /api/regions/**`
- Swagger UI & `/v3/api-docs`

> Hiện tại các endpoint `GET` danh mục/sản phẩm là công khai. Mọi endpoint khác (và mọi method ghi dữ liệu sau này) đều yêu cầu token.

### 3.3. Endpoint ADMIN

- `POST/PUT/DELETE /api/products/**`, `/api/categories/**`, `/api/brands/**` yêu cầu Bearer token và `vaiTro = ADMIN`.
- `GET /api/admin/**` yêu cầu Bearer token và `vaiTro = ADMIN`.
- Token thiếu/sai/hết hạn: `401 INVALID_CREDENTIALS`.
- Token hợp lệ nhưng không phải admin: `403 ACCESS_DENIED`.
- FE vẫn phải guard route `/admin/**` bằng `user.vaiTro === "ADMIN"`, nhưng backend là nguồn enforce cuối cùng.

### 3.4. Test trên Swagger

Bấm nút **Authorize** (ổ khoá) ở Swagger UI → dán `token` (không cần chữ "Bearer") → mọi request thử nghiệm sẽ tự thêm header.

---

## 4. Phân trang & sắp xếp

Các endpoint trả danh sách lớn (hiện tại: `GET /api/products`) dùng phân trang chuẩn Spring.

### 4.1. Query params

| Param  | Kiểu    | Mặc định        | Mô tả |
|--------|---------|-----------------|-------|
| `page` | integer | `0`             | Số trang, **bắt đầu từ 0** |
| `size` | integer | `10`            | Số phần tử mỗi trang |
| `sort` | string  | `ngayTao,desc`  | `<tên_field>,<asc\|desc>`. Có thể lặp lại để sắp xếp nhiều cấp |

Ví dụ: `GET /api/products?page=0&size=20&sort=tenSP,asc`

### 4.2. Cấu trúc trang (`PageResponse<T>`)

```jsonc
{
  "items": [ /* mảng phần tử của trang hiện tại */ ],
  "page": 0,             // trang hiện tại (0-based)
  "size": 10,            // kích thước trang yêu cầu
  "totalElements": 20,   // tổng số phần tử trên mọi trang
  "totalPages": 2,       // tổng số trang
  "last": false          // true nếu đây là trang cuối
}
```

`PageResponse` nằm trong `data` của `ApiResponse`, nên cấu trúc đầy đủ là:
```jsonc
{ "success": true, "message": "OK", "data": { "items": [...], "page": 0, ... }, "timestamp": "..." }
```

---

## 5. Chi tiết các endpoint

### 5.1. Auth

#### `POST /api/auth/register` — Đăng ký tài khoản
- **Auth:** Không
- **Body** (`RegisterRequest`):

| Field        | Kiểu       | Bắt buộc | Ràng buộc |
|--------------|------------|----------|-----------|
| `hoTen`      | string     | ✅       | ≤ 100 ký tự |
| `email`      | string     | ✅       | đúng định dạng email, ≤ 100 ký tự |
| `soDienThoai`| string     | ✅       | regex `^(0|\+84)\d{9,10}$`, ≤ 15 ký tự |
| `matKhau`    | string     | ✅       | 6–72 ký tự |
| `maKhuVuc`   | string     | ✅       | ≤ 10 ký tự, phải tồn tại trong hệ thống |
| `diaChi`     | string     | ❌       | ≤ 300 ký tự |
| `ngaySinh`   | date       | ❌       | định dạng `YYYY-MM-DD`, phải trong quá khứ |
| `gioiTinh`   | string     | ❌       | ≤ 10 ký tự (vd: "Nam", "Nữ") |
| `cccd`       | string     | ❌       | đúng 12 chữ số |

- **Thành công:** `201 Created`, `message = "Đăng ký thành công"`, `data` = `AuthResponse` (mục 5.1 dưới).
- **Lỗi thường gặp:** `400 VALIDATION_ERROR`, `409 DUPLICATE_EMAIL`, `409 DUPLICATE_PHONE`, `INVALID_REGION`.

```jsonc
// Request
{
  "hoTen": "Nguyen Van A",
  "email": "nguyenvana@example.com",
  "soDienThoai": "0123456789",
  "matKhau": "matkhau123",
  "maKhuVuc": "KV01",
  "diaChi": "123 Duong Demo, Ha Noi",
  "ngaySinh": "2000-01-01",
  "gioiTinh": "Nam",
  "cccd": "123456789012"
}
```

#### `POST /api/auth/login` — Đăng nhập
- **Auth:** Không
- **Body** (`LoginRequest`): `email` (bắt buộc, email hợp lệ), `matKhau` (bắt buộc, 6–72 ký tự).
- **Thành công:** `200 OK`, `message = "Đăng nhập thành công"`, `data` = `AuthResponse`.
- **Lỗi:** `401 INVALID_CREDENTIALS` (sai email/mật khẩu), `400 VALIDATION_ERROR`.

**`AuthResponse` (data của register & login):**

| Field       | Kiểu   | Mô tả |
|-------------|--------|-------|
| `token`     | string | JWT — đính kèm vào header `Authorization` |
| `tokenType` | string | Loại token, thường `"Bearer"` |
| `expiresIn` | number | Thời gian sống của token (giây, theo backend hiện tại) |
| `userId`    | string | Mã người dùng |
| `hoTen`     | string | Họ tên |
| `email`     | string | Email |
| `maKhuVuc`  | string | Mã khu vực |
| `vaiTro`    | string | Vai trò (role): `USER` \| `WAREHOUSE_STAFF` \| `ADMIN`. Mặc định khi đăng ký là `USER` |

```jsonc
// data
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 31536000,
  "userId": "KH001",
  "hoTen": "Nguyen Van A",
  "email": "nguyenvana@example.com",
  "maKhuVuc": "KV01",
  "vaiTro": "USER"
}
```

#### `GET /api/auth/check-email?email=...` — Kiểm tra email khả dụng
- **Auth:** Không · **Query:** `email` (bắt buộc, email hợp lệ)
- **Thành công:** `200 OK`, `data = { "available": true }` (`true` = chưa ai dùng, có thể đăng ký).

#### `GET /api/auth/check-phone?phone=...` — Kiểm tra SĐT khả dụng
- **Auth:** Không · **Query:** `phone` (bắt buộc, regex `^(0|\+84)\d{9,10}$`)
- **Thành công:** `200 OK`, `data = { "available": true }`.

---

### 5.2. Sản phẩm

#### `GET /api/products` — Danh sách sản phẩm (phân trang)
- **Auth:** Không
- **Query params:**

| Param          | Kiểu    | Bắt buộc | Mô tả |
|----------------|---------|----------|-------|
| `maDanhMuc`    | string  | ❌       | Lọc theo mã danh mục |
| `maThuongHieu` | string  | ❌       | Lọc theo mã thương hiệu |
| `trangThai`    | boolean | ❌       | Lọc theo trạng thái (`true`/`false`) |
| `page`         | integer | ❌       | Trang, mặc định `0` |
| `size`         | integer | ❌       | Kích thước trang, mặc định `10` |
| `sort`         | string  | ❌       | Mặc định `ngayTao,desc` |

- **Thành công:** `200 OK`, `data` = `PageResponse<ProductListItemResponse>`.

**`ProductListItemResponse`:**

| Field           | Kiểu             | Mô tả |
|-----------------|------------------|-------|
| `maSP`          | string           | Mã sản phẩm |
| `tenSP`         | string           | Tên sản phẩm |
| `giaBan`        | number (decimal) | Giá bán |
| `donViTinh`     | string           | Đơn vị tính |
| `hinhAnh`       | string           | Đường dẫn ảnh |
| `trangThai`     | boolean          | Trạng thái kinh doanh |
| `tenDanhMuc`    | string           | Tên danh mục |
| `tenThuongHieu` | string           | Tên thương hiệu |

```jsonc
// GET /api/products?page=0&size=2
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [
      {
        "maSP": "SP001", "tenSP": "iPhone 15 128GB", "giaBan": 19990000.00,
        "donViTinh": "Cái", "hinhAnh": "/images/products/SP001.jpg",
        "trangThai": true, "tenDanhMuc": "Điện thoại", "tenThuongHieu": "Apple"
      }
    ],
    "page": 0, "size": 2, "totalElements": 20, "totalPages": 10, "last": false
  },
  "timestamp": "2026-06-02T03:19:34.5337247"
}
```

#### `GET /api/products/{maSP}` — Chi tiết sản phẩm
- **Auth:** Không · **Path:** `maSP` — mã sản phẩm
- **Thành công:** `200 OK`, `data` = `ProductDetailResponse`.
- **Lỗi:** `404 RESOURCE_NOT_FOUND` nếu mã không tồn tại.

**`ProductDetailResponse`:**

| Field             | Kiểu             | Mô tả |
|-------------------|------------------|-------|
| `maSP`            | string           | Mã sản phẩm |
| `tenSP`           | string           | Tên sản phẩm |
| `giaBan`          | number (decimal) | Giá bán |
| `donViTinh`       | string           | Đơn vị tính |
| `hinhAnh`         | string           | Đường dẫn ảnh |
| `trangThai`       | boolean          | Trạng thái |
| `ngayTao`         | datetime         | Ngày tạo |
| `maDanhMuc`       | string           | Mã danh mục |
| `tenDanhMuc`      | string           | Tên danh mục |
| `maThuongHieu`    | string           | Mã thương hiệu |
| `tenThuongHieu`   | string           | Tên thương hiệu |
| `moTa`            | string \| null   | Mô tả (có thể null) |
| `thongSoKyThuat`  | string \| null   | Thông số kỹ thuật (có thể null) |

---

### 5.3. Danh mục / Thương hiệu / Khu vực (dữ liệu tham chiếu)

Ba endpoint này trả về **toàn bộ danh sách** (không phân trang), thường dùng để đổ dữ liệu cho dropdown/filter.

#### `GET /api/categories` — Danh sách danh mục
- **Auth:** Không · **Thành công:** `200 OK`, `data` = mảng `CategoryResponse`.

| Field         | Kiểu           | Mô tả |
|---------------|----------------|-------|
| `maDanhMuc`   | string         | Mã danh mục |
| `tenDanhMuc`  | string         | Tên danh mục |
| `maDanhMucCha`| string \| null | Mã danh mục cha (null nếu là gốc) |
| `moTa`        | string \| null | Mô tả |
| `trangThai`   | boolean        | Trạng thái |

#### `GET /api/brands` — Danh sách thương hiệu
- **Auth:** Không · **Thành công:** `200 OK`, `data` = mảng `BrandResponse`.

| Field           | Kiểu    | Mô tả |
|-----------------|---------|-------|
| `maThuongHieu`  | string  | Mã thương hiệu |
| `tenThuongHieu` | string  | Tên thương hiệu |
| `trangThai`     | boolean | Trạng thái |

#### `GET /api/regions` — Danh sách khu vực
- **Auth:** Không · **Thành công:** `200 OK`, `data` = mảng `RegionResponse`.

| Field       | Kiểu   | Mô tả |
|-------------|--------|-------|
| `maKhuVuc`  | string | Mã khu vực (dùng cho `maKhuVuc` khi đăng ký) |
| `tenKhuVuc` | string | Tên khu vực |

---

### 5.4. Admin — quản trị catalog & thống kê

Các endpoint trong mục này chỉ dành cho `ADMIN`. FE phải gửi header:

```http
Authorization: Bearer <token>
```

#### Request body dùng chung

**`ProductUpsertRequest`** dùng cho tạo và sửa sản phẩm. Khi `PUT`, backend yêu cầu `maSP` trong body trùng với `{maSP}` trên URL.

| Field            | Kiểu    | Bắt buộc | Ràng buộc |
|------------------|---------|----------|-----------|
| `maSP`           | string  | ✅       | ≤ 20 ký tự |
| `tenSP`          | string  | ✅       | ≤ 255 ký tự |
| `maDanhMuc`      | string  | ✅       | Phải tồn tại |
| `maThuongHieu`   | string  | ✅       | Phải tồn tại |
| `giaBan`         | number  | ✅       | > 0 |
| `donViTinh`      | string  | ✅       | ≤ 20 ký tự |
| `hinhAnh`        | string  | ❌       | ≤ 500 ký tự |
| `trangThai`      | boolean | ✅       | `true`/`false` |
| `moTa`           | string  | ❌       | Có thể bỏ trống |
| `thongSoKyThuat` | string  | ❌       | Có thể bỏ trống |

**`CategoryUpsertRequest`**

| Field           | Kiểu    | Bắt buộc | Ràng buộc |
|-----------------|---------|----------|-----------|
| `maDanhMuc`     | string  | ✅       | ≤ 20 ký tự |
| `tenDanhMuc`    | string  | ✅       | ≤ 100 ký tự |
| `maDanhMucCha`  | string  | ❌       | Nếu có phải tồn tại, không được trùng chính nó |
| `moTa`          | string  | ❌       | ≤ 500 ký tự |
| `trangThai`     | boolean | ✅       | `true`/`false` |

**`BrandUpsertRequest`**

| Field            | Kiểu    | Bắt buộc | Ràng buộc |
|------------------|---------|----------|-----------|
| `maThuongHieu`   | string  | ✅       | ≤ 20 ký tự |
| `tenThuongHieu`  | string  | ✅       | ≤ 100 ký tự, không trùng |
| `trangThai`      | boolean | ✅       | `true`/`false` |

#### CRUD sản phẩm

| Method | Path | Auth | Body | Thành công |
|--------|------|------|------|------------|
| POST | `/api/products` | ADMIN | `ProductUpsertRequest` | `201 Created`, `data = ProductDetailResponse` |
| PUT | `/api/products/{maSP}` | ADMIN | `ProductUpsertRequest` | `200 OK`, `data = ProductDetailResponse` |
| DELETE | `/api/products/{maSP}` | ADMIN | Không | `200 OK`, `data = null` |

```jsonc
// POST /api/products
{
  "maSP": "SP999",
  "tenSP": "Laptop Demo",
  "maDanhMuc": "DM01",
  "maThuongHieu": "TH01",
  "giaBan": 19990000,
  "donViTinh": "Cái",
  "hinhAnh": "/images/products/SP999.jpg",
  "trangThai": true,
  "moTa": "Mô tả sản phẩm",
  "thongSoKyThuat": "{\"cpu\":\"i7\"}"
}
```

`DELETE /api/products/{maSP}` là soft-delete: backend đặt `trangThai=false`, không xóa cứng để tránh lỗi khóa ngoại.

#### CRUD danh mục

| Method | Path | Auth | Body | Thành công |
|--------|------|------|------|------------|
| POST | `/api/categories` | ADMIN | `CategoryUpsertRequest` | `201 Created`, `data = CategoryResponse` |
| PUT | `/api/categories/{maDanhMuc}` | ADMIN | `CategoryUpsertRequest` | `200 OK`, `data = CategoryResponse` |
| DELETE | `/api/categories/{maDanhMuc}` | ADMIN | Không | `200 OK`, `data = null` |

`DELETE /api/categories/{maDanhMuc}` là soft-delete: backend đặt `trangThai=false`.

#### CRUD thương hiệu

| Method | Path | Auth | Body | Thành công |
|--------|------|------|------|------------|
| POST | `/api/brands` | ADMIN | `BrandUpsertRequest` | `201 Created`, `data = BrandResponse` |
| PUT | `/api/brands/{maThuongHieu}` | ADMIN | `BrandUpsertRequest` | `200 OK`, `data = BrandResponse` |
| DELETE | `/api/brands/{maThuongHieu}` | ADMIN | Không | `200 OK`, `data = null` |

`DELETE /api/brands/{maThuongHieu}` là soft-delete: backend đặt `trangThai=false`.

#### Thống kê doanh thu toàn hệ thống

`GET /api/admin/thong-ke/doanh-thu`

- **Auth:** ADMIN
- **Query optional:** `tuNgay`, `denNgay` định dạng `YYYY-MM-DD`; `maKho`; `maKhuVuc`; `maSP`; `chiTinhDaXuat`.
- **Mặc định:** nếu không truyền `chiTinhDaXuat`, backend tính như `true`.
- **Nguồn dữ liệu:** stored procedure `dbo.sp_ThongKeDoanhThu_ToanHeThong`, gom dữ liệu từ `SITE_BAC` và `SITE_NAM`.

| Query | Kiểu | Mô tả |
|-------|------|-------|
| `tuNgay` | date | Từ ngày, lọc `NgayTao >= tuNgay` |
| `denNgay` | date | Đến ngày, inclusive theo procedure |
| `maKho` | string | Lọc một kho |
| `maKhuVuc` | string | Lọc khu vực |
| `maSP` | string | Lọc một sản phẩm |
| `chiTinhDaXuat` | boolean | `true` = chỉ phiếu đã xuất, `false` = gồm doanh thu dự kiến |

```jsonc
// GET /api/admin/thong-ke/doanh-thu?tuNgay=2026-06-01&denNgay=2026-06-02&chiTinhDaXuat=true
{
  "success": true,
  "message": "OK",
  "data": {
    "theoKho": [
      {
        "siteXuat": "BAC",
        "maKhuVuc": "KV01",
        "maKhoXuat": "KB01",
        "tenKho": "Kho Hà Nội",
        "soDonHang": 2,
        "soPhieuXuat": 2,
        "tongSoLuongXuat": 5,
        "doanhThu": 1500000
      }
    ],
    "theoVung": [
      {
        "maKhuVuc": "KV01",
        "soDonHang": 2,
        "soPhieuXuat": 2,
        "soKhoThamGiaXuat": 1,
        "tongSoLuongXuat": 5,
        "doanhThu": 1500000
      }
    ],
    "toanHeThong": {
      "tongSoDonHang": 2,
      "tongSoPhieuXuat": 2,
      "tongSoKhoThamGiaXuat": 1,
      "tongSoLuongXuat": 5,
      "tongDoanhThu": 1500000
    }
  },
  "timestamp": "2026-06-02T17:40:00"
}
```

Khi không có dữ liệu: `theoKho = []`, `theoVung = []`, các số trong `toanHeThong` là `0`.

**Lỗi thường gặp cho admin API:**

| HTTP | errorCode | Trường hợp |
|------|-----------|------------|
| 400 | `VALIDATION_ERROR` | Body thiếu/sai field; query ngày sai định dạng; mã path/body không khớp khi update |
| 401 | `INVALID_CREDENTIALS` | Thiếu token hoặc token hết hạn |
| 403 | `ACCESS_DENIED` | Token hợp lệ nhưng không phải ADMIN |
| 404 | `RESOURCE_NOT_FOUND` | Không tìm thấy product/category/brand hoặc mã tham chiếu |
| 500 | `INTERNAL_ERROR` | Lỗi DB/linked server khi thống kê |

---

## 6. Tổng hợp endpoint

| Method | Path                       | Auth | Trả về (data)                         |
|--------|----------------------------|------|---------------------------------------|
| POST   | `/api/auth/register`       | ❌   | `AuthResponse` (201) |
| POST   | `/api/auth/login`          | ❌   | `AuthResponse` |
| GET    | `/api/auth/check-email`    | ❌   | `{ available }` |
| GET    | `/api/auth/check-phone`    | ❌   | `{ available }` |
| GET    | `/api/products`            | ❌   | `PageResponse<ProductListItemResponse>` |
| GET    | `/api/products/{maSP}`     | ❌   | `ProductDetailResponse` |
| GET    | `/api/categories`          | ❌   | `CategoryResponse[]` |
| GET    | `/api/brands`              | ❌   | `BrandResponse[]` |
| GET    | `/api/regions`             | ❌   | `RegionResponse[]` |
| POST   | `/api/products`            | ADMIN | `ProductDetailResponse` (201) |
| PUT    | `/api/products/{maSP}`     | ADMIN | `ProductDetailResponse` |
| DELETE | `/api/products/{maSP}`     | ADMIN | `null` |
| POST   | `/api/categories`          | ADMIN | `CategoryResponse` (201) |
| PUT    | `/api/categories/{maDanhMuc}` | ADMIN | `CategoryResponse` |
| DELETE | `/api/categories/{maDanhMuc}` | ADMIN | `null` |
| POST   | `/api/brands`              | ADMIN | `BrandResponse` (201) |
| PUT    | `/api/brands/{maThuongHieu}` | ADMIN | `BrandResponse` |
| DELETE | `/api/brands/{maThuongHieu}` | ADMIN | `null` |
| GET    | `/api/admin/thong-ke/doanh-thu` | ADMIN | `ThongKeDoanhThuResponse` |

---

## 7. Gợi ý type cho FE (TypeScript)

```ts
// Wrapper chung
interface ApiResponse<T> {
  success: true;
  message: string;
  data: T;
  timestamp: string;
}

interface ErrorResponse {
  success: false;
  message: string;
  errorCode:
    | "VALIDATION_ERROR" | "INVALID_CREDENTIALS" | "ACCESS_DENIED"
    | "RESOURCE_NOT_FOUND" | "DUPLICATE_EMAIL" | "DUPLICATE_PHONE"
    | "INVALID_REGION" | "INTERNAL_ERROR";
  details: string[];
  timestamp: string;
}

interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

interface AuthResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  hoTen: string;
  email: string;
  maKhuVuc: string;
  vaiTro: string;
}

interface ProductListItem {
  maSP: string; tenSP: string; giaBan: number; donViTinh: string;
  hinhAnh: string; trangThai: boolean; tenDanhMuc: string; tenThuongHieu: string;
}

interface ProductUpsert {
  maSP: string;
  tenSP: string;
  maDanhMuc: string;
  maThuongHieu: string;
  giaBan: number;
  donViTinh: string;
  hinhAnh?: string;
  trangThai: boolean;
  moTa?: string;
  thongSoKyThuat?: string;
}

interface CategoryUpsert {
  maDanhMuc: string;
  tenDanhMuc: string;
  maDanhMucCha?: string;
  moTa?: string;
  trangThai: boolean;
}

interface BrandUpsert {
  maThuongHieu: string;
  tenThuongHieu: string;
  trangThai: boolean;
}

interface DoanhThuTheoKho {
  siteXuat: string;
  maKhuVuc: string;
  maKhoXuat: string;
  tenKho: string;
  soDonHang: number;
  soPhieuXuat: number;
  tongSoLuongXuat: number;
  doanhThu: number;
}

interface DoanhThuTheoVung {
  maKhuVuc: string;
  soDonHang: number;
  soPhieuXuat: number;
  soKhoThamGiaXuat: number;
  tongSoLuongXuat: number;
  doanhThu: number;
}

interface DoanhThuToanHeThong {
  tongSoDonHang: number;
  tongSoPhieuXuat: number;
  tongSoKhoThamGiaXuat: number;
  tongSoLuongXuat: number;
  tongDoanhThu: number;
}

interface ThongKeDoanhThuResponse {
  theoKho: DoanhThuTheoKho[];
  theoVung: DoanhThuTheoVung[];
  toanHeThong: DoanhThuToanHeThong;
}
```

> Tài liệu này phản ánh trạng thái API tại thời điểm cập nhật. Nguồn chính xác nhất luôn là Swagger UI (`/swagger-ui.html`) sinh trực tiếp từ code.
