# API Giỏ hàng (`/api/cart`) — Tài liệu cho Frontend

Mô tả chi tiết shape request/response cho API giỏ hàng `/api/cart`. Trong triển khai FE Phase 11, các endpoint này được gọi trên **Site Bắc/Site Nam theo khu vực user** qua `getRegionalApiClient(region, token)`: `BAC -> :8081`, `NAM -> :8082`. Shape bám sát code `GioHangController.java`, `GioHangServiceImpl.java` và các DTO liên quan.

- **Base URL (dev FE dùng):** `http://localhost:8081` cho user Bắc, `http://localhost:8082` cho user Nam
- **Prefix:** `/api/cart`
- **Định dạng:** `application/json`, charset `UTF-8`
- **Múi giờ timestamp:** `Asia/Ho_Chi_Minh`
- **Tài liệu chung:** xem `API_DOCUMENTATION.md` (response envelope, bảng mã lỗi, phân trang).

> ⚠️ **Quan trọng — khác với `API_DOCUMENTATION.md` cũ:** `GioHangResponse` hiện **không** còn field `chiTiet` phẳng. Backend đã tách giỏ thành **`sanPhamHopLe`** (còn hàng) và **`sanPhamHetHang`** (hết hàng), và mỗi dòng có thêm `soLuongKhaDung`. Doc này là bản chính xác theo code mới nhất.

---

## 1. Xác thực

**Tất cả** endpoint giỏ hàng đều **bắt buộc** gửi Bearer token:

```
Authorization: Bearer <token>
```

`maND` (mã người dùng) được lấy từ claims trong JWT — **FE không gửi `maND`**. Token thiếu/sai/hết hạn → `401 INVALID_CREDENTIALS`.

> Lưu ý kiến trúc: FE Phase 11 đã dùng `getRegionalApiClient(region, token)`, không dùng `mainApiClient` cho cart. Nếu backend tạm thời chỉ bật cart ở Site Main thì cần bật/copy endpoint tương ứng ở Site Bắc/Nam để demo đúng phân tán.

---

## 2. Cấu trúc dữ liệu

### 2.1. `GioHangResponse` — payload trả về cho hầu hết thao tác

```jsonc
{
  "sanPhamHopLe": [          // các dòng còn hàng (soLuongKhaDung > 0)
    {
      "maSP": "SP001",
      "tenSP": "iPhone 15 128GB",
      "hinhAnh": "/images/products/SP001.jpg",
      "donViTinh": "Cái",
      "soLuong": 2,           // số lượng user để trong giỏ
      "giaBan": 19990000.00,  // giá hiện tại từ bảng sản phẩm (không cố định trong giỏ)
      "thanhTien": 39980000.00, // = giaBan * soLuong
      "soLuongKhaDung": 15    // tồn kho khả dụng hiện tại của sản phẩm
    }
  ],
  "sanPhamHetHang": [        // các dòng đã hết hàng (soLuongKhaDung == 0)
    {
      "maSP": "SP099",
      "tenSP": "Tai nghe XYZ",
      "hinhAnh": "/images/products/SP099.jpg",
      "donViTinh": "Cái",
      "soLuong": 1,
      "giaBan": 500000.00,
      "thanhTien": 500000.00,
      "soLuongKhaDung": 0
    }
  ],
  "tongSoLuong": 2,           // CHỈ tính sanPhamHopLe
  "tongTien": 39980000.00     // CHỈ tính sanPhamHopLe
}
```

**Field của `GioHangResponse`:**

| Field            | Kiểu                      | Ý nghĩa |
|------------------|---------------------------|---------|
| `sanPhamHopLe`   | `ChiTietGioHang[]`        | Các dòng còn hàng (`soLuongKhaDung > 0`). Dùng để thanh toán. |
| `sanPhamHetHang` | `ChiTietGioHang[]`        | Các dòng đã hết hàng (`soLuongKhaDung == 0`). FE nên hiển thị riêng, mờ đi, không cho chọn mua. |
| `tongSoLuong`    | `integer`                 | Tổng `soLuong` của **chỉ** `sanPhamHopLe`. |
| `tongTien`       | `number` (BigDecimal)     | Tổng `thanhTien` của **chỉ** `sanPhamHopLe`. |

**Field của `ChiTietGioHang`:**

| Field            | Kiểu      | Ý nghĩa |
|------------------|-----------|---------|
| `maSP`           | `string`  | Mã sản phẩm |
| `tenSP`          | `string`  | Tên sản phẩm |
| `hinhAnh`        | `string`  | Đường dẫn ảnh |
| `donViTinh`      | `string`  | Đơn vị tính (vd "Cái") |
| `soLuong`        | `integer` | Số lượng trong giỏ |
| `giaBan`         | `number`  | Giá hiện tại của sản phẩm tại thời điểm gọi API |
| `thanhTien`      | `number`  | `giaBan * soLuong` |
| `soLuongKhaDung` | `integer` | Tồn kho khả dụng (`tongSoLuongKhaDung`) hiện tại |

> 💡 **Phân loại do backend tự tính** dựa trên tồn kho khả dụng tại thời điểm gọi: `soLuongKhaDung > 0` → `sanPhamHopLe`, ngược lại → `sanPhamHetHang`. FE không cần tự phân loại.
>
> 💡 **Cảnh báo vượt tồn:** một dòng vẫn nằm trong `sanPhamHopLe` ngay cả khi `soLuong > soLuongKhaDung` (user đặt nhiều hơn tồn). FE nên so sánh `soLuong` với `soLuongKhaDung` để hiện cảnh báo "chỉ còn N sản phẩm".
>
> 💡 Vì Jackson cấu hình `non_null`, FE vẫn nên thủ phòng. Nhưng `sanPhamHopLe`/`sanPhamHetHang` luôn là mảng (rỗng `[]` chứ không null), `tongTien` rỗng = `0`.

### 2.2. Giỏ rỗng

Khi user chưa có giỏ (hoặc giỏ trống), `GET /api/cart` trả:

```jsonc
{ "sanPhamHopLe": [], "sanPhamHetHang": [], "tongSoLuong": 0, "tongTien": 0 }
```

---

## 3. Bảng mã lỗi liên quan

| HTTP | errorCode            | Khi nào gặp ở giỏ hàng |
|------|----------------------|------------------------|
| 400  | `VALIDATION_ERROR`   | Body sai: `maSP` trống, `soLuong` null hoặc `< 1`. Chi tiết trong `details[]`. |
| 401  | `INVALID_CREDENTIALS`| Thiếu/sai/hết hạn token. |
| 404  | `RESOURCE_NOT_FOUND` | Sản phẩm không tồn tại / ngừng kinh doanh; giỏ hàng không tồn tại; sản phẩm không có trong giỏ. |
| 500  | `INTERNAL_ERROR`     | Lỗi nội bộ. |

---

## 4. Chi tiết endpoint

### 4.1. `GET /api/cart` — Xem giỏ hàng

- **Auth:** ✅ Bearer token
- **Body:** Không
- **Thành công:** `200 OK`, `message = "OK"`, `data` = `GioHangResponse`
- **Ghi chú:** Chưa có giỏ → trả giỏ rỗng (mục 2.2), **không** báo lỗi.

---

### 4.2. `POST /api/cart/items` — Thêm sản phẩm vào giỏ

- **Auth:** ✅ Bearer token
- **Body** (`ThemVaoGioRequest`):

| Field     | Kiểu    | Bắt buộc | Ràng buộc |
|-----------|---------|----------|-----------|
| `maSP`    | string  | ✅       | Không được để trống; phải là sản phẩm đang kinh doanh (`trangThai = true`) |
| `soLuong` | integer | ✅       | ≥ 1 |

```jsonc
// Request
{ "maSP": "SP001", "soLuong": 2 }
```

- **Thành công:** `200 OK`, `message = "Đã thêm sản phẩm vào giỏ hàng"`, `data` = `GioHangResponse`
- **Hành vi:**
  - Nếu sản phẩm **đã có** trong giỏ → **cộng dồn** số lượng (`soLuong cũ + soLuong gửi lên`), không tạo dòng mới.
  - Nếu user **chưa có giỏ** → tự động tạo giỏ mới rồi thêm.
- **Lỗi:**
  - `400 VALIDATION_ERROR` — `maSP` trống hoặc `soLuong < 1`
  - `404 RESOURCE_NOT_FOUND` — sản phẩm không tồn tại hoặc đã ngừng kinh doanh

> ⚠️ Backend **không kiểm tra tồn kho** khi thêm — vẫn thêm được dù hết hàng. Trạng thái còn/hết phản ánh qua việc dòng đó rơi vào `sanPhamHopLe` hay `sanPhamHetHang` trong response.

---

### 4.3. `PUT /api/cart/items/{maSP}` — Cập nhật số lượng

- **Auth:** ✅ Bearer token
- **Path:** `maSP` — mã sản phẩm cần cập nhật
- **Body** (`CapNhatSoLuongRequest`):

| Field     | Kiểu    | Bắt buộc | Ràng buộc |
|-----------|---------|----------|-----------|
| `soLuong` | integer | ✅       | ≥ 1 |

```jsonc
// PUT /api/cart/items/SP001
{ "soLuong": 5 }
```

- **Thành công:** `200 OK`, `message = "Đã cập nhật số lượng"`, `data` = `GioHangResponse`
- **Hành vi:** **Ghi đè** số lượng về đúng giá trị gửi lên (không cộng dồn). Muốn đưa về 0 → dùng `DELETE` (vì `soLuong ≥ 1`).
- **Lỗi:**
  - `400 VALIDATION_ERROR` — `soLuong < 1` hoặc null
  - `404 RESOURCE_NOT_FOUND` — giỏ hàng không tồn tại hoặc sản phẩm không có trong giỏ

---

### 4.4. `DELETE /api/cart/items/{maSP}` — Xóa một sản phẩm khỏi giỏ

- **Auth:** ✅ Bearer token
- **Path:** `maSP` — mã sản phẩm cần xóa
- **Body:** Không
- **Thành công:** `200 OK`, `message = "Đã xóa sản phẩm khỏi giỏ hàng"`, `data = null`
- **Hành vi:** Xóa toàn bộ dòng sản phẩm khỏi giỏ, bất kể số lượng.
- **Lỗi:** `404 RESOURCE_NOT_FOUND` — giỏ hàng không tồn tại hoặc sản phẩm không có trong giỏ

> Vì `data = null` (Jackson `non_null`), response thành công sẽ **không có** field `data`. FE dựa vào HTTP 200 + `success: true`, sau đó gọi lại `GET /api/cart` (hoặc invalidate query) để lấy giỏ mới.

---

### 4.5. `DELETE /api/cart` — Xóa toàn bộ giỏ hàng

- **Auth:** ✅ Bearer token
- **Body:** Không
- **Thành công:** `200 OK`, `message = "Đã xóa toàn bộ giỏ hàng"`, `data = null`
- **Hành vi:** Xóa tất cả dòng trong giỏ. Nếu user **không có giỏ đang hoạt động** → no-op, vẫn trả `200` (không báo lỗi).

---

## 5. Tổng hợp

| Method | Path                     | Auth | Mô tả                     | Trả về (data)      |
|--------|--------------------------|------|---------------------------|--------------------|
| GET    | `/api/cart`              | ✅   | Xem giỏ hàng              | `GioHangResponse`  |
| POST   | `/api/cart/items`        | ✅   | Thêm sản phẩm (cộng dồn)  | `GioHangResponse`  |
| PUT    | `/api/cart/items/{maSP}` | ✅   | Cập nhật số lượng (ghi đè)| `GioHangResponse`  |
| DELETE | `/api/cart/items/{maSP}` | ✅   | Xóa một sản phẩm          | `null`             |
| DELETE | `/api/cart`              | ✅   | Xóa toàn bộ giỏ           | `null`             |

---

## 6. Type TypeScript cho FE

```ts
export interface ChiTietGioHang {
  maSP: string;
  tenSP: string;
  hinhAnh: string;
  donViTinh: string;
  soLuong: number;        // số lượng trong giỏ
  giaBan: number;
  thanhTien: number;      // giaBan * soLuong
  soLuongKhaDung: number; // tồn kho khả dụng hiện tại
}

export interface GioHangResponse {
  sanPhamHopLe: ChiTietGioHang[];   // còn hàng (soLuongKhaDung > 0)
  sanPhamHetHang: ChiTietGioHang[]; // hết hàng (soLuongKhaDung === 0)
  tongSoLuong: number;              // chỉ tính sanPhamHopLe
  tongTien: number;                 // chỉ tính sanPhamHopLe
}

export interface ThemVaoGioRequest {
  maSP: string;
  soLuong: number; // >= 1
}

export interface CapNhatSoLuongRequest {
  soLuong: number; // >= 1
}
```

### Gợi ý gọi API (qua `apiFetch`, đã unwrap `ApiResponse.data`)

```ts
// Xem giỏ
const gio = await apiFetch<GioHangResponse>("/api/cart");

// Thêm vào giỏ
await apiFetch<GioHangResponse>("/api/cart/items", {
  method: "POST",
  body: { maSP: "SP001", soLuong: 2 } satisfies ThemVaoGioRequest,
});

// Cập nhật số lượng
await apiFetch<GioHangResponse>(`/api/cart/items/${maSP}`, {
  method: "PUT",
  body: { soLuong: 5 } satisfies CapNhatSoLuongRequest,
});

// Xóa một dòng (data = null)
await apiFetch<void>(`/api/cart/items/${maSP}`, { method: "DELETE" });

// Xóa toàn bộ giỏ (data = null)
await apiFetch<void>("/api/cart", { method: "DELETE" });
```

### Lưu ý UX cho FE

- Sau `POST` / `PUT` / `DELETE`, response của 3 thao tác đầu đã chứa `GioHangResponse` mới → dùng luôn để cập nhật state; riêng 2 `DELETE` trả `null` → gọi lại `GET /api/cart` hoặc invalidate query cache.
- Hiển thị `sanPhamHetHang` thành mục riêng (mờ, nút "Xóa"), không cho thanh toán; chỉ `sanPhamHopLe` mới đưa vào tổng tiền.
- Với dòng `soLuong > soLuongKhaDung`: cảnh báo và đề xuất giảm số lượng về `soLuongKhaDung`.
- `giaBan` lấy theo giá hiện tại mỗi lần gọi → giá trong giỏ có thể đổi giữa các lần xem.
