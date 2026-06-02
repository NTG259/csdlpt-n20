# API Quản lý Đơn hàng (Admin)

Base URL: `/api/admin/orders`

Yêu cầu `ROLE_ADMIN` + JWT Bearer token.

> **Kiến trúc đa site:** Dữ liệu đơn hàng nằm trên hai linked server riêng biệt:
> - `SITE_BAC` — Khu vực miền Bắc
> - `SITE_NAM` — Khu vực miền Nam
>
> Các endpoint đọc (`GET`) có thể truy vấn cả hai site cùng lúc.
> Các endpoint ghi (`PATCH`) yêu cầu chỉ định `siteNguon` để cập nhật đúng server.

---

## Mục lục

1. [GET /api/admin/orders](#1-get-apiadminorders) — Danh sách đơn hàng (có filter + phân trang)
2. [GET /api/admin/orders/{maDonHang}](#2-get-apiadminordersmaddonhang) — Chi tiết đơn hàng
3. [PATCH /api/admin/orders/{maDonHang}/trang-thai](#3-patch-apiadminordersmadddonhangtrang-thai) — Cập nhật trạng thái

---

## Bảng trạng thái

### Trạng thái đơn hàng (`trangThaiDH`)

| Giá trị | Ý nghĩa |
|---|---|
| `pending` | Chờ xác nhận |
| `processing` | Đang xử lý / đóng gói |
| `shipping` | Đang vận chuyển |
| `completed` | Đã giao thành công |
| `cancelled` | Đã huỷ |

### Trạng thái thanh toán (`trangThaiTT`)

| Giá trị | Ý nghĩa |
|---|---|
| `waiting_cod` | Chờ thu tiền COD |
| `paid` | Đã thanh toán |
| `failed` | Thanh toán thất bại |
| `cancelled` | Đã huỷ thanh toán |

---

## 1. GET /api/admin/orders

Lấy danh sách đơn hàng từ SITE_BAC và SITE_NAM, hỗ trợ filter và phân trang.

### Request

```
GET /api/admin/orders?siteNguon=SITE_BAC&trangThaiDH=pending&page=0&size=20
Authorization: Bearer <token>
```

### Query Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `siteNguon` | `string` | Không | `SITE_BAC` hoặc `SITE_NAM`. Bỏ qua = trả cả hai |
| `trangThaiDH` | `string` | Không | Lọc theo trạng thái đơn hàng |
| `trangThaiTT` | `string` | Không | Lọc theo trạng thái thanh toán |
| `tuNgay` | `date` | Không | Từ ngày (`yyyy-MM-dd`), theo `NgayDat` |
| `denNgay` | `date` | Không | Đến ngày (`yyyy-MM-dd`), bao gồm cả ngày này |
| `page` | `int` | Không | Trang, bắt đầu từ `0`. Mặc định `0` |
| `size` | `int` | Không | Bản ghi mỗi trang, 1–100. Mặc định `20` |

### Response `200 OK`

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [
      {
        "siteNguon": "SITE_BAC",
        "maDonHang": "550e8400-e29b-41d4-a716-446655440001",
        "maND": "550e8400-e29b-41d4-a716-446655440000",
        "ngayDat": "2025-06-01T14:30:00",
        "hoTenNguoiNhan": "Nguyễn Văn A",
        "soDienThoaiNhan": "0912345678",
        "maKhuVucXuLi": "KV_BAC",
        "tongTien": 1500000.00,
        "phuongThucTT": "COD",
        "trangThaiTT": "waiting_cod",
        "trangThaiDH": "pending"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "last": false
  },
  "timestamp": "2025-06-03T10:00:00"
}
```

---

## 2. GET /api/admin/orders/{maDonHang}

Lấy chi tiết đầy đủ của một đơn hàng, bao gồm danh sách sản phẩm.

`siteNguon` **bắt buộc** để định tuyến đúng server.

### Request

```
GET /api/admin/orders/550e8400-e29b-41d4-a716-446655440001?siteNguon=SITE_BAC
Authorization: Bearer <token>
```

### Query Parameters

| Tham số | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `siteNguon` | `string` | **Có** | `SITE_BAC` hoặc `SITE_NAM` |

### Response `200 OK`

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "siteNguon": "SITE_BAC",
    "maDonHang": "550e8400-e29b-41d4-a716-446655440001",
    "maND": "550e8400-e29b-41d4-a716-446655440000",
    "ngayDat": "2025-06-01T14:30:00",
    "hoTenNguoiNhan": "Nguyễn Văn A",
    "soDienThoaiNhan": "0912345678",
    "diaChiGiao": "123 Phố Huế, Hoàn Kiếm, Hà Nội",
    "maKhuVucXuLi": "KV_BAC",
    "tongTien": 1500000.00,
    "phuongThucTT": "COD",
    "trangThaiTT": "waiting_cod",
    "trangThaiDH": "pending",
    "ghiChu": "Giao trong giờ hành chính",
    "chiTiet": [
      {
        "maCTDH": "aa1e8400-e29b-41d4-a716-446655440010",
        "maSP": "SP001",
        "tenSP": "Laptop Dell XPS 15",
        "soLuong": 1,
        "donGia": 1200000.00,
        "thanhTien": 1200000.00
      },
      {
        "maCTDH": "bb2e8400-e29b-41d4-a716-446655440011",
        "maSP": "SP042",
        "tenSP": "Chuột không dây Logitech",
        "soLuong": 2,
        "donGia": 150000.00,
        "thanhTien": 300000.00
      }
    ]
  },
  "timestamp": "2025-06-03T10:00:00"
}
```

### Lỗi

| HTTP | errorCode | Khi nào |
|---|---|---|
| `400` | `VALIDATION_ERROR` | `siteNguon` không hợp lệ hoặc thiếu |
| `404` | `RESOURCE_NOT_FOUND` | Không tìm thấy đơn hàng trên site chỉ định |

---

## 3. PATCH /api/admin/orders/{maDonHang}/trang-thai

Cập nhật trạng thái đơn hàng (`trangThaiDH`) và/hoặc trạng thái thanh toán (`trangThaiTT`).

Cần cung cấp ít nhất một trong hai trường. Trả về thông tin đơn hàng sau khi cập nhật.

### Request

```
PATCH /api/admin/orders/550e8400-e29b-41d4-a716-446655440001/trang-thai
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "siteNguon": "SITE_BAC",
  "trangThaiDH": "processing",
  "trangThaiTT": null
}
```

### Request Body

| Trường | Kiểu | Bắt buộc | Mô tả |
|---|---|---|---|
| `siteNguon` | `string` | **Có** | `SITE_BAC` hoặc `SITE_NAM` |
| `trangThaiDH` | `string` | Không* | Trạng thái đơn hàng mới |
| `trangThaiTT` | `string` | Không* | Trạng thái thanh toán mới |

*Ít nhất một trong `trangThaiDH` hoặc `trangThaiTT` phải được cung cấp (không null).

**Giá trị hợp lệ:** Xem [bảng trạng thái](#bảng-trạng-thái) ở trên. Có thể gửi bằng chữ thường (`pending`) hoặc chữ hoa (`PENDING`).

### Ví dụ — Xác nhận đơn và đánh dấu đã thanh toán

```json
{
  "siteNguon": "SITE_NAM",
  "trangThaiDH": "processing",
  "trangThaiTT": "paid"
}
```

### Ví dụ — Chỉ cập nhật trạng thái đơn hàng

```json
{
  "siteNguon": "SITE_BAC",
  "trangThaiDH": "completed"
}
```

### Response `200 OK` — thông tin đơn hàng sau khi cập nhật

Trả về cấu trúc giống `GET /{maDonHang}`.

### Lỗi

| HTTP | errorCode | Khi nào |
|---|---|---|
| `400` | `VALIDATION_ERROR` | `siteNguon` sai, cả hai trạng thái đều null, hoặc giá trị trạng thái không hợp lệ |
| `404` | `RESOURCE_NOT_FOUND` | Không tìm thấy đơn hàng trên site chỉ định |

---

## Cấu trúc `ApiResponse<T>` chung

```json
{
  "success": true,
  "message": "OK",
  "data": "<T>",
  "timestamp": "2025-06-03T10:00:00"
}
```

Khi lỗi:

```json
{
  "success": false,
  "message": "Mô tả lỗi",
  "errorCode": "ERROR_CODE",
  "details": [],
  "timestamp": "2025-06-03T10:00:00"
}
```
