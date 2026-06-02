# PHASE 4 — DTO & RESPONSE CHUẨN (chi tiết)

> **Phạm vi:** DTO request/response + wrapper chuẩn cho API Site Main, đồng bộ với entity (Phase 2) và repository (Phase 3).
> **Mục tiêu phase:** Mọi API trả về **một format thống nhất**, dễ demo Postman, dễ giải thích trong báo cáo, không bao giờ lộ entity (đặc biệt `matKhau`).
> **Tài liệu chỉ mô tả skeleton (field + annotation) — KHÔNG viết logic mapping đầy đủ.**

> **Tiến độ cập nhật 2026-06-02:**
> - Đã tạo `ApiResponse`, `ErrorResponse`, `PageResponse`
> - Đã tạo request DTO (`RegisterRequest`, `LoginRequest`) với annotation validation cơ bản
> - Đã tạo response DTO cho auth, product, category, brand, region, availability
> - Quyết định hiện tại: giữ `AuthResponse` làm response chính cho luồng auto-login; `RegisterResponse` vẫn có sẵn nếu muốn tách luồng sau này
> - `thongSoKyThuat` đang trả dạng `String` JSON; `CategoryResponse` đang dùng danh sách phẳng
> - Jackson đang cấu hình `non_null`; thời gian `java.time` dùng output ISO mặc định của Jackson/Spring Boot 4

---

## 0. Nguyên tắc & quyết định nền

| # | Quyết định | Lý do |
|---|---|---|
| 1 | **Không trả entity ra controller** — luôn qua DTO | Tránh lazy-loading lỗi, tránh lộ `matKhau`, tách tầng rõ |
| 2 | **Response DTO dùng `record` (Java 21)** | Bất biến, gọn, không cần Lombok; map 1 dòng |
| 3 | **Request DTO dùng `record` hoặc class + Lombok** | Cần annotation validation; record vẫn gắn được `@NotBlank`... |
| 4 | **Map thủ công trong service** (không MapStruct) | Đồ án nhỏ → tránh over-engineering; mapping rõ ràng, dễ giải thích |
| 5 | **Mọi response bọc `ApiResponse<T>`**; danh sách phân trang dùng `PageResponse<T>` đặt trong `data` | Frontend & người chấm đọc 1 kiểu duy nhất |
| 6 | **`userId` trả dạng `String`** (từ `UUID.toString()`) | Đồng nhất JSON, tránh lệ thuộc kiểu GUID phía client |
| 7 | **`trangThai` (Boolean) trả `true/false`**; có thể kèm nhãn nếu cần | Khớp `NumericBooleanConverter` ở entity |

---

## 1. Wrapper chuẩn (Task 4.1) — package `common/`

### 1.1 `ApiResponse<T>` — vỏ chung cho MỌI response thành công
| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `success` | boolean | luôn `true` ở response thành công |
| `message` | String | thông điệp ngắn (vd "Đăng nhập thành công") |
| `data` | T | payload (DTO / List / PageResponse) |
| `timestamp` | LocalDateTime / String (ISO) | thời điểm phản hồi |

- Gợi ý factory: `ApiResponse.ok(data)`, `ApiResponse.ok(message, data)`.
- Khuyến nghị `record ApiResponse<T>(boolean success, String message, T data, LocalDateTime timestamp)` + static method.

### 1.2 `ErrorResponse` — vỏ chung cho MỌI lỗi (dùng ở Phase 5)
| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `success` | boolean | luôn `false` |
| `message` | String | mô tả lỗi cho người dùng |
| `errorCode` | String | mã lỗi máy đọc được (vd `DUPLICATE_EMAIL`) |
| `details` | List\<String\> | danh sách lỗi field (validation) — rỗng nếu không có |
| `timestamp` | LocalDateTime / String | thời điểm |

> Thống nhất `errorCode` dạng SNAKE_CASE in hoa: `DUPLICATE_EMAIL`, `DUPLICATE_PHONE`, `INVALID_CREDENTIALS`, `RESOURCE_NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`.

### 1.3 `PageResponse<T>` — chuẩn hóa kết quả phân trang
| Field | Kiểu | Nguồn từ `Page<T>` |
|---|---|---|
| `items` | List\<T\> | `page.getContent()` |
| `page` | int | `page.getNumber()` (0-based) |
| `size` | int | `page.getSize()` |
| `totalElements` | long | `page.getTotalElements()` |
| `totalPages` | int | `page.getTotalPages()` |
| `last` | boolean | `page.isLast()` (tùy chọn, tiện cho FE) |

- Gợi ý factory: `PageResponse.from(Page<T> page)` → đỡ lặp code ở mỗi service.
- **Cách dùng:** `ApiResponse.ok(PageResponse.from(productPage))` → `data` chứa object phân trang.

---

## 2. DTO request (Task 4.2) — package `dto/request/`

> Annotation validation chi tiết thuộc **Phase 5**; ở đây liệt kê field + ràng buộc tóm tắt để chốt hình dạng.

### 2.1 `RegisterRequest`
| Field | Kiểu | Bắt buộc | Ràng buộc (Phase 5) | Map tới entity |
|---|---|---|---|---|
| `hoTen` | String | ✅ | `@NotBlank @Size(max=100)` | NguoiDung.hoTen |
| `email` | String | ✅ | `@NotBlank @Email @Size(max=100)` | NguoiDung.email + UGI.email |
| `soDienThoai` | String | ❌* | `@Pattern(0/+84...)` | NguoiDung.soDienThoai + UGI.soDienThoai |
| `matKhau` | String | ✅ | `@NotBlank @Size(min=6,max=72)` | → hash → NguoiDung.matKhau |
| `maKhuVuc` | String | ✅ | `@NotBlank` + tồn tại trong KhuVuc (service) | NguoiDung.MaKV + UGI.MaKhuVuc |
| `diaChi` | String | ❌ | `@Size(max=300)` | NguoiDung.diaChi |
| `ngaySinh` | LocalDate/LocalDateTime | ❌ | `@Past` | NguoiDung.ngaySinh |
| `gioiTinh` | String | ❌ | mặc định "Nam" | NguoiDung.gioiTinh |
| `cccd` | String | ❌ | `@Pattern("\\d{12}")` | NguoiDung.cccd |

> *SĐT: DB cho NULL nhưng UNIQUE. Nếu coi SĐT là bắt buộc khi đăng ký thì thêm `@NotBlank`. Quyết định: **bắt buộc** cho đơn giản demo (check trùng có ý nghĩa). `vaiTro` KHÔNG nhận từ client — luôn gán `USER` ở service.

### 2.2 `LoginRequest`
| Field | Kiểu | Ràng buộc |
|---|---|---|
| `email` | String | `@NotBlank @Email` |
| `matKhau` | String | `@NotBlank` |

---

## 3. DTO response (Task 4.3) — package `dto/response/`

### 3.1 `AuthResponse` — trả khi đăng nhập (và đăng ký nếu auto-login)
| Field | Kiểu | Nguồn |
|---|---|---|
| `token` | String | JwtService |
| `tokenType` | String | hằng `"Bearer"` |
| `expiresIn` | long | giây còn hiệu lực (tùy chọn) |
| `userId` | String | `nguoiDung.maND.toString()` |
| `hoTen` | String | NguoiDung.hoTen |
| `email` | String | NguoiDung.email |
| `maKhuVuc` | String | NguoiDung.khuVuc.maKhuVuc |
| `vaiTro` | String | NguoiDung.vaiTro.name() |

### 3.2 `RegisterResponse` — nếu KHÔNG auto-login
| Field | Kiểu | Ghi chú |
|---|---|---|
| `userId` | String | maND |
| `hoTen` | String | |
| `email` | String | |
| `maKhuVuc` | String | |

> **Quyết định khuyến nghị:** đăng ký xong **trả luôn `AuthResponse`** (auto-login) → demo mượt, đỡ 1 bước gọi login. Nếu muốn tách rõ luồng thì dùng `RegisterResponse`.

### 3.3 `ProductListItemResponse` — 1 item trong danh sách sản phẩm
| Field | Kiểu | Nguồn |
|---|---|---|
| `maSP` | String | SanPhamCore.maSP |
| `tenSP` | String | tenSP |
| `giaBan` | BigDecimal | giaBan |
| `donViTinh` | String | donViTinh |
| `hinhAnh` | String | hinhAnh |
| `trangThai` | Boolean | trangThai |
| `tenDanhMuc` | String | danhMuc.tenDanhMuc |
| `tenThuongHieu` | String | thuongHieu.tenThuongHieu |

> Khớp đúng với `ProductListItemView` (projection) ở Phase 3 → service map view → response (hoặc dùng thẳng view làm `T` trong `PageResponse`).

### 3.4 `ProductDetailResponse` — chi tiết 1 sản phẩm
| Field | Kiểu | Nguồn |
|---|---|---|
| `maSP` | String | Core.maSP |
| `tenSP` | String | Core.tenSP |
| `giaBan` | BigDecimal | Core.giaBan |
| `donViTinh` | String | Core.donViTinh |
| `hinhAnh` | String | Core.hinhAnh |
| `trangThai` | Boolean | Core.trangThai |
| `ngayTao` | LocalDateTime | Core.ngayTao |
| `maDanhMuc` / `tenDanhMuc` | String | Core.danhMuc.* |
| `maThuongHieu` / `tenThuongHieu` | String | Core.thuongHieu.* |
| `moTa` | String | SanPhamDetail.moTa |
| `thongSoKyThuat` | String **hoặc** Object/Map | SanPhamDetail.thongSoKyThuat (JSON) |

> **`thongSoKyThuat`:** DDL lưu JSON (NVARCHAR(MAX) + CHECK ISJSON). Giai đoạn đầu **trả nguyên chuỗi JSON (`String`)** cho gọn; nếu muốn FE nhận object thì service parse bằng `ObjectMapper.readTree(...)` → `JsonNode`/`Map`. Khuyến nghị: trả `String`, để FE tự parse — đỡ phức tạp.

### 3.5 `CategoryResponse` — danh mục
| Field | Kiểu | Nguồn |
|---|---|---|
| `maDanhMuc` | String | maDanhMuc |
| `tenDanhMuc` | String | tenDanhMuc |
| `maDanhMucCha` | String | danhMucCha?.maDanhMuc (null nếu gốc) |
| `moTa` | String | moTa |
| `trangThai` | Boolean | trangThai |
| `children` | List\<CategoryResponse\> | (TÙY CHỌN) nếu trả dạng cây |

> Giai đoạn đầu: trả **danh sách phẳng** (bỏ `children`). Khi cần cây, service gom theo `maDanhMucCha`. Tránh để `children` rỗng gây nặng JSON nếu chưa dùng.

### 3.6 `BrandResponse`
| Field | Kiểu | Nguồn |
|---|---|---|
| `maThuongHieu` | String | maThuongHieu |
| `tenThuongHieu` | String | tenThuongHieu |
| `trangThai` | Boolean | trangThai |

### 3.7 `RegionResponse`
| Field | Kiểu | Nguồn |
|---|---|---|
| `maKhuVuc` | String | maKhuVuc |
| `tenKhuVuc` | String | tenKhuVuc |

### 3.8 `CheckAvailabilityResponse` — cho check-email / check-phone
| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `available` | boolean | `true` = chưa dùng (đăng ký được) |

---

## 4. Ví dụ JSON (để chốt format với FE / Postman)

**Danh sách sản phẩm (200):**
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "items": [
      { "maSP": "SP001", "tenSP": "Áo thun", "giaBan": 150000.00,
        "donViTinh": "Cái", "hinhAnh": "sp001.jpg", "trangThai": true,
        "tenDanhMuc": "Áo", "tenThuongHieu": "Local Brand" }
    ],
    "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "last": true
  },
  "timestamp": "2026-06-02T10:00:00"
}
```

**Đăng nhập (200):** xem `KE_HOACH_TASK_SITE_MAIN.md` (Phase 8).

**Lỗi validation (400):**
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errorCode": "VALIDATION_ERROR",
  "details": ["email: không đúng định dạng", "matKhau: tối thiểu 6 ký tự"],
  "timestamp": "2026-06-02T10:00:00"
}
```

---

## 5. Quy ước & cấu trúc file

- **Đặt tên:** request = `<Hành động>Request`; response = `<Đối tượng>Response` / `<Đối tượng>...Response`.
- **Tiền tệ:** dùng `BigDecimal`, để FE format hiển thị; backend không tự thêm dấu phân cách.
- **Thời gian:** `LocalDateTime`/`LocalDate`; cấu hình Jackson trả ISO-8601 (`spring.jackson.serialization.write-dates-as-timestamps=false` nếu cần).
- **null:** có thể bật `spring.jackson.default-property-inclusion=non_null` để JSON gọn (ẩn field null) — tùy chọn.

```
common/
├── ApiResponse.java
├── ErrorResponse.java
└── PageResponse.java
dto/
├── request/
│   ├── RegisterRequest.java
│   └── LoginRequest.java
└── response/
    ├── AuthResponse.java
    ├── RegisterResponse.java          (nếu không auto-login)
    ├── ProductListItemResponse.java   (hoặc dùng projection view)
    ├── ProductDetailResponse.java
    ├── CategoryResponse.java
    ├── BrandResponse.java
    ├── RegionResponse.java
    └── CheckAvailabilityResponse.java
```

---

## ✅ Checklist Phase 4

**Wrapper**
- [x] `ApiResponse<T>` + factory `ok(...)`
- [x] `ErrorResponse` (đồng bộ errorCode với Phase 5)
- [x] `PageResponse<T>` + `from(Page<T>)`

**Request**
- [x] `RegisterRequest` (chốt: SĐT bắt buộc; `vaiTro` KHÔNG nhận từ client)
- [x] `LoginRequest`

**Response**
- [x] `AuthResponse` (userId = UUID→String, có maKhuVuc + vaiTro)
- [x] `ProductListItemResponse` khớp projection Phase 3
- [x] `ProductDetailResponse` (chốt: thongSoKyThuat trả String JSON)
- [x] `CategoryResponse` (chốt: phẳng), `BrandResponse`, `RegionResponse`
- [x] `CheckAvailabilityResponse`

**Chốt quyết định**
- [x] Đăng ký auto-login (trả `AuthResponse`) hay tách (`RegisterResponse`)?
- [x] Cấu hình Jackson: ISO date mặc định, ẩn null
- [x] Không có field nhạy cảm (`matKhau`) trong bất kỳ response nào

---

*Hết Phase 4. Bước tiếp theo: Phase 5 — Validation & Exception (đồng bộ `errorCode` đã định ở 1.2).*
