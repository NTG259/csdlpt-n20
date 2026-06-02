# SiteMain API — Tài liệu cho Frontend

Tài liệu mô tả cấu trúc chung và chi tiết toàn bộ API hiện có của hệ thống SiteMain.

- **Base URL (dev):** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Định dạng:** `application/json`, charset `UTF-8`
- **Múi giờ timestamp:** `Asia/Ho_Chi_Minh`

> **Cập nhật (branch `localserver`):** Các endpoint đăng ký / đăng nhập (`/api/auth/**`) đã bị xóa khỏi backend. Backend chỉ còn bộ **decode & validate JWT** — token phải do hệ thống xác thực bên ngoài cấp. Đã thêm API quản lý **giỏ hàng** (`/api/cart`).

---

## 1. Cấu trúc response chung

Mọi response đều được bọc trong một cấu trúc thống nhất. FE chỉ cần xử lý 2 dạng: **thành công** và **lỗi**.

### 1.1. Response thành công (`ApiResponse<T>`)

```jsonc
{
  "success": true,
  "message": "OK",                  // hoặc thông điệp riêng, ví dụ "Đã thêm sản phẩm vào giỏ hàng"
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
    "soLuong: Số lượng phải ít nhất là 1"
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
| `INVALID_CREDENTIALS`| 401         | Token thiếu, sai định dạng, hoặc hết hạn             | Gọi API cần auth mà không có token hợp lệ |
| `ACCESS_DENIED`      | 403         | Không đủ quyền truy cập                              | Token hợp lệ nhưng không đủ vai trò |
| `RESOURCE_NOT_FOUND` | 404         | Không tìm thấy tài nguyên                            | Sản phẩm không tồn tại; sản phẩm không có trong giỏ; giỏ hàng không tồn tại |
| `INTERNAL_ERROR`     | 500         | Lỗi nội bộ                                           | Lỗi không lường trước; FE hiển thị "thử lại sau" |

---

## 3. Xác thực (Authentication)

Hệ thống dùng **JWT Bearer token**, stateless (không session/cookie). Token được cấp bởi hệ thống bên ngoài — backend chỉ validate chữ ký và decode claims.

### 3.1. Luồng sử dụng

1. FE lấy `token` từ hệ thống xác thực (site auth / SSO).
2. Với mọi request cần xác thực, đính kèm header:
   ```
   Authorization: Bearer <token>
   ```
3. Token hết hạn → API trả `401 INVALID_CREDENTIALS` → FE điều hướng về trang lấy token mới.

### 3.2. Endpoint công khai (KHÔNG cần token)

- `GET /api/products/**`, `GET /api/categories/**`, `GET /api/brands/**`, `GET /api/regions/**`
- Swagger UI & `/v3/api-docs`

> Mọi endpoint khác — bao gồm toàn bộ `/api/cart/**` — đều **bắt buộc** có Bearer token hợp lệ.

### 3.3. Test trên Swagger

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

`PageResponse` nằm trong `data` của `ApiResponse`.

---

## 5. Chi tiết các endpoint

### 5.1. Sản phẩm

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

### 5.2. Danh mục / Thương hiệu / Khu vực (dữ liệu tham chiếu)

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
| `maKhuVuc`  | string | Mã khu vực |
| `tenKhuVuc` | string | Tên khu vực |

---

### 5.3. Giỏ hàng (`/api/cart`)

> Tất cả endpoint giỏ hàng đều **yêu cầu Bearer token**. `maND` được lấy từ claims của JWT — FE không cần gửi.

**`GioHangResponse` (data trả về cho các thao tác thành công):**

```jsonc
{
  "chiTiet": [
    {
      "maSP": "SP001",
      "tenSP": "iPhone 15 128GB",
      "hinhAnh": "/images/products/SP001.jpg",
      "donViTinh": "Cái",
      "soLuong": 2,
      "giaBan": 19990000.00,
      "thanhTien": 39980000.00
    }
  ],
  "tongSoLuong": 2,
  "tongTien": 39980000.00
}
```

> `giaBan` là giá hiện tại lấy từ bảng sản phẩm tại thời điểm gọi API (không lưu cố định trong giỏ).

---

#### `GET /api/cart` — Xem giỏ hàng

- **Auth:** ✅ Bearer token
- **Body:** Không
- **Thành công:** `200 OK`, `data` = `GioHangResponse`
- **Ghi chú:** Nếu user chưa có giỏ hàng, trả về giỏ rỗng `{ chiTiet: [], tongSoLuong: 0, tongTien: 0 }`.

---

#### `POST /api/cart/items` — Thêm sản phẩm vào giỏ

- **Auth:** ✅ Bearer token
- **Body** (`ThemVaoGioRequest`):

| Field     | Kiểu    | Bắt buộc | Ràng buộc |
|-----------|---------|----------|-----------|
| `maSP`    | string  | ✅       | Không được để trống; phải là sản phẩm đang kinh doanh (`trangThai = true`) |
| `soLuong` | integer | ✅       | ≥ 1 |

- **Thành công:** `200 OK`, `message = "Đã thêm sản phẩm vào giỏ hàng"`, `data` = `GioHangResponse`
- **Lỗi:**
  - `400 VALIDATION_ERROR` — `maSP` trống hoặc `soLuong < 1`
  - `404 RESOURCE_NOT_FOUND` — sản phẩm không tồn tại hoặc đã ngừng kinh doanh
- **Hành vi:** Nếu sản phẩm đã có trong giỏ, **cộng dồn** số lượng (không tạo dòng mới).

```jsonc
// Request
{ "maSP": "SP001", "soLuong": 2 }
```

---

#### `PUT /api/cart/items/{maSP}` — Cập nhật số lượng

- **Auth:** ✅ Bearer token
- **Path:** `maSP` — mã sản phẩm cần cập nhật
- **Body** (`CapNhatSoLuongRequest`):

| Field     | Kiểu    | Bắt buộc | Ràng buộc |
|-----------|---------|----------|-----------|
| `soLuong` | integer | ✅       | ≥ 1 |

- **Thành công:** `200 OK`, `message = "Đã cập nhật số lượng"`, `data` = `GioHangResponse`
- **Lỗi:**
  - `400 VALIDATION_ERROR` — `soLuong < 1`
  - `404 RESOURCE_NOT_FOUND` — giỏ hàng không tồn tại hoặc sản phẩm không có trong giỏ
- **Hành vi:** Đặt số lượng về đúng giá trị gửi lên (ghi đè, không cộng dồn).

```jsonc
// PUT /api/cart/items/SP001
// Request
{ "soLuong": 5 }
```

---

#### `DELETE /api/cart/items/{maSP}` — Xóa một sản phẩm khỏi giỏ

- **Auth:** ✅ Bearer token
- **Path:** `maSP` — mã sản phẩm cần xóa
- **Body:** Không
- **Thành công:** `200 OK`, `message = "Đã xóa sản phẩm khỏi giỏ hàng"`, `data = null`
- **Lỗi:** `404 RESOURCE_NOT_FOUND` — giỏ hàng không tồn tại hoặc sản phẩm không có trong giỏ

---

#### `DELETE /api/cart` — Xóa toàn bộ giỏ hàng

- **Auth:** ✅ Bearer token
- **Body:** Không
- **Thành công:** `200 OK`, `message = "Đã xóa toàn bộ giỏ hàng"`, `data = null`
- **Ghi chú:** Nếu user không có giỏ hàng đang hoạt động, không báo lỗi (no-op).

---

## 6. Tổng hợp endpoint

| Method | Path                       | Auth | Mô tả                                      | Trả về (data)               |
|--------|----------------------------|------|--------------------------------------------|-----------------------------|
| GET    | `/api/products`            | ❌   | Danh sách sản phẩm (có lọc, phân trang)    | `PageResponse<ProductListItemResponse>` |
| GET    | `/api/products/{maSP}`     | ❌   | Chi tiết sản phẩm                          | `ProductDetailResponse`     |
| GET    | `/api/categories`          | ❌   | Danh sách danh mục                         | `CategoryResponse[]`        |
| GET    | `/api/brands`              | ❌   | Danh sách thương hiệu                      | `BrandResponse[]`           |
| GET    | `/api/regions`             | ❌   | Danh sách khu vực                          | `RegionResponse[]`          |
| GET    | `/api/cart`                | ✅   | Xem giỏ hàng                              | `GioHangResponse`           |
| POST   | `/api/cart/items`          | ✅   | Thêm sản phẩm vào giỏ                     | `GioHangResponse`           |
| PUT    | `/api/cart/items/{maSP}`   | ✅   | Cập nhật số lượng                          | `GioHangResponse`           |
| DELETE | `/api/cart/items/{maSP}`   | ✅   | Xóa một sản phẩm khỏi giỏ                 | `null`                      |
| DELETE | `/api/cart`                | ✅   | Xóa toàn bộ giỏ hàng                      | `null`                      |

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
    | "RESOURCE_NOT_FOUND" | "INTERNAL_ERROR";
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

interface ProductListItem {
  maSP: string; tenSP: string; giaBan: number; donViTinh: string;
  hinhAnh: string; trangThai: boolean; tenDanhMuc: string; tenThuongHieu: string;
}

interface ChiTietGioHang {
  maSP: string;
  tenSP: string;
  hinhAnh: string;
  donViTinh: string;
  soLuong: number;
  giaBan: number;
  thanhTien: number;
}

interface GioHangResponse {
  chiTiet: ChiTietGioHang[];
  tongSoLuong: number;
  tongTien: number;
}

interface ThemVaoGioRequest {
  maSP: string;
  soLuong: number; // >= 1
}

interface CapNhatSoLuongRequest {
  soLuong: number; // >= 1
}
```

> Tài liệu này phản ánh trạng thái API tại thời điểm cập nhật. Nguồn chính xác nhất luôn là Swagger UI (`/swagger-ui.html`) sinh trực tiếp từ code.
