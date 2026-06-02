# Thay đổi trên branch `localserver`

Tài liệu này ghi lại toàn bộ những thay đổi, bổ sung, và xóa bỏ trên nhánh `localserver` so với `main`. Mục tiêu: chạy backend trong môi trường phân tán (distributed), không có chức năng tự đăng ký/đăng nhập, chỉ validate JWT từ hệ thống xác thực bên ngoài, và bổ sung API giỏ hàng.

---

## 1. Những gì đã XÓA

### 1.1. Chức năng đăng ký / đăng nhập (`/api/auth/**`)

Lý do: hệ thống phân tán — việc cấp token do site xác thực trung tâm đảm nhiệm. Backend này chỉ nhận và decode token.

**File bị xóa:**

| File | Vị trí |
|------|--------|
| `AuthController.java` | `controller/` |
| `AuthService.java` | `service/` |
| `AuthServiceImpl.java` | `service/impl/` |
| `LoginRequest.java` | `dto/request/` |
| `RegisterRequest.java` | `dto/request/` |
| `AuthResponse.java` | `dto/response/` |
| `CheckAvailabilityResponse.java` | `dto/response/` |
| `UserGlobalIndex.java` | `domain/entity/` |
| `UserGlobalIndexRepository.java` | `repository/` |
| `AuthControllerTest.java` | `src/test/.../controller/` |
| `AuthServiceImplTest.java` | `src/test/.../service/` |

### 1.2. Thay đổi trong `SecurityConfig.java`

Xóa các public route của auth và bean `PasswordEncoder`:
- ~~`.requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()`~~
- ~~`.requestMatchers(HttpMethod.GET, "/api/auth/check-email", "/api/auth/check-phone").permitAll()`~~
- ~~`@Bean PasswordEncoder passwordEncoder()` (BCryptPasswordEncoder)~~

Public routes còn lại: Swagger UI, `/api/products/**`, `/api/categories/**`, `/api/brands/**`, `/api/regions/**` (chỉ GET).

---

## 2. Những gì đã GIỮ NGUYÊN (JWT decode chain)

Toàn bộ bộ decode/validate JWT **không thay đổi**:

| File | Vai trò |
|------|---------|
| `security/JwtService.java` | Parse, validate chữ ký JWT; extract claims |
| `security/JwtAuthenticationFilter.java` | Filter chạy trước mọi request; gắn `SecurityContext` |
| `security/CustomUserDetails.java` | Wrapper expose `getUserId()` (UUID), `getMaKhuVuc()` |
| `security/CustomUserDetailsService.java` | Load user từ DB theo `email` trong JWT claims |
| `domain/entity/NguoiDung.java` | Entity user |
| `repository/NguoiDungRepository.java` | `findByEmail`, `findByEmailFetchKhuVuc` |

---

## 3. Những gì đã THÊM MỚI

### 3.1. Entity: Giỏ hàng

#### `domain/entity/GioHang.java`

Ánh xạ bảng `GioHang` trong schema.

| Column | Java field | Kiểu |
|--------|-----------|------|
| `MaGioHang` (PK, uniqueidentifier) | `maGioHang` | `UUID` |
| `MaND` (uniqueidentifier) | `maND` | `UUID` |
| `NgayTao` (DATETIME2) | `ngayTao` | `LocalDateTime` |
| `NgayCapNhat` (DATETIME2) | `ngayCapNhat` | `LocalDateTime` |
| `TrangThai` (VARCHAR 30, default 'active') | `trangThai` | `String` |

Quan hệ: `@OneToMany(mappedBy="gioHang", cascade=ALL, orphanRemoval=true)` → `List<ChiTietGioHang> chiTietList`.

> Schema có **filtered unique index** `UX_GioHang_Active ON GioHang(MaND) WHERE TrangThai = 'active'` → mỗi user chỉ có **1** giỏ hàng active tại một thời điểm.

#### `domain/entity/ChiTietGioHang.java`

Ánh xạ bảng `ChiTietGioHang`.

| Column | Java field | Kiểu |
|--------|-----------|------|
| `MaCTGH` (PK, uniqueidentifier) | `maCTGH` | `UUID` |
| `MaGioHang` (FK → GioHang) | `gioHang` | `@ManyToOne GioHang` |
| `MaSP` (FK → SanPham_Core) | `sanPham` | `@ManyToOne SanPhamCore` |
| `SoLuong` (INT) | `soLuong` | `Integer` |
| `NgayThem` (DATETIME2) | `ngayThem` | `LocalDateTime` |

> **Không lưu giá** trong giỏ hàng. Giá (`giaBan`) được lấy từ `SanPham_Core.GiaBan` tại thời điểm gọi API để tính `thanhTien`.

---

### 3.2. Repository

#### `repository/GioHangRepository.java`

```java
// Lấy giỏ hàng active kèm toàn bộ items (LEFT JOIN FETCH tránh N+1)
Optional<GioHang> findActiveByMaNDWithItems(UUID maND);   // @Query JPQL với TrangThai = 'active'

// Dùng cho xoaGioHang (chỉ cần GioHang, không cần items)
Optional<GioHang> findByMaNDAndTrangThai(UUID maND, String trangThai);
```

#### `repository/ChiTietGioHangRepository.java`

Minimal — nghiệp vụ xóa/cập nhật item đi qua `GioHang` (cascade), không truy cập trực tiếp.

---

### 3.3. DTO

#### Request

| Class | Field | Ràng buộc |
|-------|-------|----------|
| `ThemVaoGioRequest` | `maSP` (String), `soLuong` (Integer) | `@NotBlank`, `@NotNull @Min(1)` |
| `CapNhatSoLuongRequest` | `soLuong` (Integer) | `@NotNull @Min(1)` |

#### Response

**`GioHangResponse`**
```java
record GioHangResponse(List<ChiTietGioHangResponse> chiTiet, int tongSoLuong, BigDecimal tongTien)
```

**`ChiTietGioHangResponse`**
```java
record ChiTietGioHangResponse(
    String maSP, String tenSP, String hinhAnh, String donViTinh,
    Integer soLuong, BigDecimal giaBan, BigDecimal thanhTien
)
```

---

### 3.4. Service

#### `service/GioHangService.java` (interface)

```java
GioHangResponse getGioHang(UUID maND);
GioHangResponse themVaoGio(UUID maND, ThemVaoGioRequest request);
GioHangResponse capNhatSoLuong(UUID maND, String maSP, CapNhatSoLuongRequest request);
void xoaSanPham(UUID maND, String maSP);
void xoaGioHang(UUID maND);
```

#### `service/impl/GioHangServiceImpl.java`

| Method | Logic chính |
|--------|-------------|
| `getGioHang` | Tìm giỏ active; nếu không có trả `emptyResponse()` |
| `themVaoGio` | Validate sản phẩm active → tìm/tạo giỏ → cộng dồn nếu đã có item, thêm mới nếu chưa |
| `capNhatSoLuong` | Tìm giỏ + item → ghi đè số lượng |
| `xoaSanPham` | Tìm giỏ → `chiTietList.removeIf(...)` (cascade xóa DB qua `orphanRemoval`) |
| `xoaGioHang` | Tìm giỏ → `chiTietList.clear()` (giữ bản ghi GioHang, xóa hết items) |
| `toResponse` | Map entity → DTO; tính `giaBan` từ `sanPham.getGiaBan()`, `thanhTien = giaBan × soLuong` |

---

### 3.5. Controller: `controller/GioHangController.java`

Base path: `/api/cart`. Tất cả phương thức đều `@AuthenticationPrincipal CustomUserDetails userDetails` để lấy `userId`.

| Method | Path | HTTP | Response |
|--------|------|------|----------|
| `getGioHang` | `/api/cart` | GET | `200 GioHangResponse` |
| `themVaoGio` | `/api/cart/items` | POST | `200 GioHangResponse` + message |
| `capNhatSoLuong` | `/api/cart/items/{maSP}` | PUT | `200 GioHangResponse` + message |
| `xoaSanPham` | `/api/cart/items/{maSP}` | DELETE | `200 null` + message |
| `xoaGioHang` | `/api/cart` | DELETE | `200 null` + message |

---

## 4. Ràng buộc quan trọng (database phân tán)

> **Quy tắc bất biến:** Tất cả truy cập dữ liệu **phải đi qua stored procedure** — không truy vấn bảng trực tiếp (kể cả JPQL/derived query). Đây là yêu cầu của kiến trúc phân tán.

Hiện tại các repository đang dùng JPQL/derived query (trạng thái tạm thời). Khi có đủ tên và signature của stored procedures, toàn bộ repository sẽ được chuyển sang `@NamedStoredProcedureQuery` + `@Procedure`.

**Repositories cần chuyển đổi:**

| Repository | Query cần chuyển |
|-----------|-----------------|
| `GioHangRepository` | `findActiveByMaNDWithItems`, `findByMaNDAndTrangThai` |
| `SanPhamCoreRepository` | `search`, `searchProjection`, `findDetailById` |
| `NguoiDungRepository` | `findByEmail`, `findByEmailFetchKhuVuc` |
| `DanhMucRepository` | `findByTrangThai`, `findByDanhMucChaIsNull`, `findByDanhMucCha_MaDanhMuc` |
| `ThuongHieuRepository` | `findByTrangThai` |
| `KhuVucRepository` | `findAll` |
| `SanPhamDetailRepository` | `findBySanPhamCore_MaSP` |

---

## 5. Checklist

**Auth removal**
- [x] Xóa AuthController, AuthService, AuthServiceImpl
- [x] Xóa LoginRequest, RegisterRequest, AuthResponse, CheckAvailabilityResponse
- [x] Xóa UserGlobalIndex entity + repository
- [x] Xóa test files auth
- [x] Cập nhật SecurityConfig (bỏ public auth routes, bỏ PasswordEncoder bean)

**Cart feature**
- [x] Entity `GioHang` (MaGioHang, MaND, NgayTao, NgayCapNhat, TrangThai, chiTietList)
- [x] Entity `ChiTietGioHang` (MaCTGH, FK MaGioHang, FK MaSP, SoLuong, NgayThem)
- [x] `GioHangRepository` với query active cart + JOIN FETCH
- [x] `ChiTietGioHangRepository` (minimal)
- [x] Request DTO: `ThemVaoGioRequest`, `CapNhatSoLuongRequest`
- [x] Response DTO: `GioHangResponse`, `ChiTietGioHangResponse`
- [x] `GioHangService` interface + `GioHangServiceImpl`
- [x] `GioHangController` (5 endpoints)

**Test**
- [x] `GioHangServiceImplTest` — 12 unit test với Mockito (service layer, không cần Spring context)
- [x] `GioHangControllerTest` — 13 WebMvcTest với MockMvc (controller + security layer)

**Docs**
- [x] Cập nhật `API_DOCUMENTATION.md` (xóa auth section, thêm cart section)
- [x] Tạo file này (`CHANGES_LOCALSERVER.md`)

**Pending**
- [ ] Chuyển toàn bộ repository sang stored procedure (chờ tên và signature SP)
