# ADMIN API — Thiết kế chi tiết (Backend Site Main)

> Thiết kế backend cho các API phục vụ trang **Admin** ([`FRONTEND_PHASE14_ADMIN.md`](FRONTEND_PHASE14_ADMIN.md)): **CRUD Sản phẩm / Danh mục / Thương hiệu** và **Thống kê doanh thu toàn hệ thống**.
>
> Phần **thống kê gọi stored procedure** `dbo.sp_ThongKeDoanhThu_ToanHeThong` (đã tạo sẵn trong DB `store_management`, file `src/main/resources/db/proceduce.sql`) — proc dùng linked server `SITE_BAC` / `SITE_NAM` để gộp doanh thu từ cả 2 site.

Tất cả tuân thủ convention sẵn có: envelope `ApiResponse<T>` / `ErrorResponse`, mã lỗi `ErrorCodes`, layered `controller → service(+impl) → repository`, tên domain tiếng Việt, Jackson `non_null`, timezone `Asia/Ho_Chi_Minh`.

---

## 0. Nguyên tắc chung

- **Prefix:** CRUD dùng lại tài nguyên hiện có (`/api/products`, `/api/categories`, `/api/brands`); thống kê đặt dưới `/api/admin/thong-ke/**`.
- **Phân quyền:** mọi endpoint **ghi** (POST/PUT/DELETE) và **toàn bộ** `/api/admin/**` yêu cầu Bearer token + role `ADMIN`. Các `GET` catalog vẫn công khai như cũ.
- **Trả về:** tạo/sửa trả entity vừa ghi; xoá trả `null` (`ApiResponse` với `data` bị lược do `non_null`).
- **Lỗi:** `VALIDATION_ERROR` (400, kèm `details[]`), `DUPLICATE_*`/`RESOURCE_NOT_FOUND` cho nghiệp vụ, `ACCESS_DENIED` (403), `INVALID_CREDENTIALS` (401), `INTERNAL_ERROR` (500, gồm lỗi linked server).

---

## 1. Bảo mật & phân quyền

`SecurityConfig` hiện `permitAll` cho `GET /api/products|categories|brands|regions/**`. Cần bổ sung **method-level cho ghi + nhóm admin**.

```java
// trong authorizeHttpRequests(...)
.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**",
        "/api/brands/**", "/api/regions/**").permitAll()
// Ghi danh mục/thương hiệu/sản phẩm + thống kê → chỉ ADMIN
.requestMatchers(HttpMethod.POST,   "/api/products/**", "/api/categories/**", "/api/brands/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.PUT,    "/api/products/**", "/api/categories/**", "/api/brands/**").hasRole("ADMIN")
.requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**", "/api/brands/**").hasRole("ADMIN")
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

- **`hasRole("ADMIN")`** kỳ vọng authority `ROLE_ADMIN`. Đảm bảo `CustomUserDetailsService` map `VaiTro.ADMIN` → `new SimpleGrantedAuthority("ROLE_ADMIN")` (hoặc dùng `.hasAuthority("ADMIN")` nếu không có tiền tố). **Kiểm tra điểm này trước khi code** để 403 hoạt động đúng.
- Token không đủ quyền → Spring trả 403; `GlobalExceptionHandler` map `AccessDeniedException` → `ACCESS_DENIED`.

---

## 2. Quản lý Sản phẩm (CRUD)

Sản phẩm tách 2 bảng: `SanPham_Core` (giá, danh mục, thương hiệu, trạng thái…) và `SanPham_Detail` (mô tả, thông số). Service ghi cả hai trong một transaction.

### 2.1. DTO request (`dto/request/`)

```java
// ProductUpsertRequest.java — dùng chung cho tạo & sửa (maSP đến từ path khi sửa)
public record ProductUpsertRequest(
        @NotBlank @Size(max = 20) String maSP,        // bỏ qua khi PUT (lấy từ path)
        @NotBlank @Size(max = 255) String tenSP,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal giaBan,
        @NotBlank @Size(max = 50) String donViTinh,
        @Size(max = 500) String hinhAnh,
        @NotNull Boolean trangThai,
        @NotBlank String maDanhMuc,
        @NotBlank String maThuongHieu,
        String moTa,
        String thongSoKyThuat
) {}
```

> Khi tạo: validate `maSP` chưa tồn tại (→ `DUPLICATE_*` / `VALIDATION_ERROR`), `maDanhMuc`/`maThuongHieu` tồn tại (→ 404 nếu sai). Khi sửa (PUT): bỏ qua `maSP` trong body, dùng path.

### 2.2. Endpoint (thêm vào `ProductController`)

| Method | Path | Body | Trả về |
|---|---|---|---|
| POST | `/api/products` | `ProductUpsertRequest` | `ProductDetailResponse` |
| PUT | `/api/products/{maSP}` | `ProductUpsertRequest` | `ProductDetailResponse` |
| DELETE | `/api/products/{maSP}` | — | `null` |

```java
@PostMapping
public ResponseEntity<ApiResponse<ProductDetailResponse>> create(@Valid @RequestBody ProductUpsertRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Đã tạo sản phẩm", productService.create(req)));
}

@PutMapping("/{maSP}")
public ResponseEntity<ApiResponse<ProductDetailResponse>> update(
        @PathVariable String maSP, @Valid @RequestBody ProductUpsertRequest req) {
    return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật sản phẩm", productService.update(maSP, req)));
}

@DeleteMapping("/{maSP}")
public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String maSP) {
    productService.delete(maSP);
    return ResponseEntity.ok(ApiResponse.ok("Đã xoá sản phẩm", null));
}
```

### 2.3. Service (`ProductService` + impl)
- `create`: kiểm tra trùng `maSP`; load `DanhMuc`/`ThuongHieu` (404 nếu không có); lưu `SanPhamCore` + `SanPhamDetail`; set `ngayTao`. `@Transactional`.
- `update`: load core (404), cập nhật trường, upsert detail.
- `delete`: chọn **một** chính sách — **xoá cứng** (`deleteById`, có thể vướng FK nếu sản phẩm đã nằm trong giỏ/đơn) **hoặc khuyến nghị soft-delete** (`trangThai=false`). Doc này khuyến nghị **soft-delete** để an toàn FK; nếu xoá cứng, bắt `DataIntegrityViolationException` → trả lỗi nghiệp vụ "Sản phẩm đang được sử dụng".

> **Ghi chú nhất quán:** trang client lọc `trangThai=true`; admin gọi GET **không truyền** `trangThai` để thấy cả hàng ngừng bán → cần `ProductService.getProducts` cho phép `trangThai=null` (đã hỗ trợ).

---

## 3. Quản lý Danh mục (CRUD)

### 3.1. DTO request
```java
public record CategoryUpsertRequest(
        @NotBlank @Size(max = 20) String maDanhMuc,   // bỏ qua khi PUT
        @NotBlank @Size(max = 100) String tenDanhMuc,
        @Size(max = 20) String maDanhMucCha,          // optional; phải tồn tại nếu có & khác chính nó
        String moTa,
        @NotNull Boolean trangThai
) {}
```

### 3.2. Endpoint (`CategoryController`)
| Method | Path | Body | Trả về |
|---|---|---|---|
| POST | `/api/categories` | `CategoryUpsertRequest` | `CategoryResponse` |
| PUT | `/api/categories/{maDanhMuc}` | `CategoryUpsertRequest` | `CategoryResponse` |
| DELETE | `/api/categories/{maDanhMuc}` | — | `null` |

### 3.3. Quy tắc
- `maDanhMucCha`: nếu có → phải tồn tại; khi sửa không cho trỏ về chính nó (chống vòng). 
- Xoá danh mục **đang có sản phẩm** hoặc **là cha của danh mục khác** → trả lỗi nghiệp vụ (ví dụ exception mới `ResourceInUseException` → map `VALIDATION_ERROR`/409, tùy chọn). Khuyến nghị soft-delete.

---

## 4. Quản lý Thương hiệu (CRUD)

```java
public record BrandUpsertRequest(
        @NotBlank @Size(max = 20) String maThuongHieu,  // bỏ qua khi PUT
        @NotBlank @Size(max = 100) String tenThuongHieu,
        @NotNull Boolean trangThai
) {}
```

| Method | Path | Body | Trả về |
|---|---|---|---|
| POST | `/api/brands` | `BrandUpsertRequest` | `BrandResponse` |
| PUT | `/api/brands/{maThuongHieu}` | `BrandUpsertRequest` | `BrandResponse` |
| DELETE | `/api/brands/{maThuongHieu}` | — | `null` |

Quy tắc xoá tương tự danh mục (chặn/soft-delete khi còn sản phẩm tham chiếu).

---

## 5. ⭐ Thống kê Doanh thu toàn hệ thống (stored procedure)

Đây là phần trọng tâm, gọi `dbo.sp_ThongKeDoanhThu_ToanHeThong`.

### 5.1. Stored procedure — tham số & kết quả

**Tham số** (đều optional, lọc động):

| Tham số | Kiểu SQL | Ý nghĩa |
|---|---|---|
| `@TuNgay` | DATETIME2 | Từ ngày (lọc `PhieuXuatKho.NgayTao >=`) |
| `@DenNgay` | DATETIME2 | Đến ngày (inclusive — proc tự `DATEADD(DAY,1,...)`) |
| `@MaKho` | VARCHAR(20) | Lọc theo 1 kho |
| `@MaKhuVuc` | VARCHAR(10) | Lọc theo khu vực (mã DB: `Bac`/`Nam`) |
| `@MaSP` | VARCHAR(20) | Lọc theo 1 sản phẩm |
| `@ChiTinhDaXuat` | BIT (mặc định 1) | `1`=chỉ phiếu `TrangThaiXuat='exported'`; `0`=tính cả chưa xuất (doanh thu dự kiến) |

**Proc trả về 3 result set, theo thứ tự:**

1. **Doanh thu theo từng kho:** `SiteXuat, MaKhuVuc, MaKhoXuat, TenKho, SoDonHang, SoPhieuXuat, TongSoLuongXuat, DoanhThu`
2. **Doanh thu theo vùng:** `MaKhuVuc, SoDonHang, SoPhieuXuat, SoKhoThamGiaXuat, TongSoLuongXuat, DoanhThu`
3. **Doanh thu toàn hệ thống** (1 dòng): `TongSoDonHang, TongSoPhieuXuat, TongSoKhoThamGiaXuat, TongSoLuongXuat, TongDoanhThu`

> ⚠️ `SUM(...)` trên tập rỗng trả **NULL** → result set 3 luôn có 1 dòng nhưng các cột có thể NULL khi không có dữ liệu. **Phải coalesce về 0** khi map.
> ⚠️ `OPENQUERY`/linked server lỗi → ném `DataAccessException` → API `500 INTERNAL_ERROR`.

### 5.2. Endpoint

**`GET /api/admin/thong-ke/doanh-thu`** — Auth: 🔐 ADMIN.

Query params (tất cả optional): `tuNgay`, `denNgay` (ISO `yyyy-MM-dd`), `maKho`, `maKhuVuc`, `maSP`, `chiTinhDaXuat` (default `true`).

```jsonc
// GET /api/admin/thong-ke/doanh-thu?tuNgay=2026-01-01&denNgay=2026-06-02&chiTinhDaXuat=true
{
  "success": true, "message": "OK",
  "data": {
    "theoKho": [
      { "site": "BAC", "maKhuVuc": "Bac", "maKho": "K_BAC_01", "tenKho": "Kho Hà Nội",
        "soDonHang": 12, "soPhieuXuat": 12, "tongSoLuongXuat": 80, "doanhThu": 250000000.00 }
    ],
    "theoVung": [
      { "maKhuVuc": "Bac", "soDonHang": 20, "soPhieuXuat": 21, "soKhoThamGiaXuat": 2,
        "tongSoLuongXuat": 140, "doanhThu": 410000000.00 },
      { "maKhuVuc": "Nam", "soDonHang": 15, "soPhieuXuat": 15, "soKhoThamGiaXuat": 1,
        "tongSoLuongXuat": 90,  "doanhThu": 300000000.00 }
    ],
    "toanHeThong": {
      "tongSoDonHang": 35, "tongSoPhieuXuat": 36, "tongSoKhoThamGiaXuat": 3,
      "tongSoLuongXuat": 230, "tongDoanhThu": 710000000.00
    }
  },
  "timestamp": "2026-06-02T17:40:00"
}
```

Không có dữ liệu trong khoảng lọc → `theoKho: []`, `theoVung: []`, `toanHeThong` với các số = 0.

### 5.3. DTO response (`dto/response/`)

```java
public record DoanhThuTheoKhoResponse(
        String site, String maKhuVuc, String maKho, String tenKho,
        int soDonHang, int soPhieuXuat, long tongSoLuongXuat, BigDecimal doanhThu) {}

public record DoanhThuTheoVungResponse(
        String maKhuVuc, int soDonHang, int soPhieuXuat, int soKhoThamGiaXuat,
        long tongSoLuongXuat, BigDecimal doanhThu) {}

public record DoanhThuToanHeThongResponse(
        int tongSoDonHang, int tongSoPhieuXuat, int tongSoKhoThamGiaXuat,
        long tongSoLuongXuat, BigDecimal tongDoanhThu) {}

public record ThongKeDoanhThuResponse(
        List<DoanhThuTheoKhoResponse> theoKho,
        List<DoanhThuTheoVungResponse> theoVung,
        DoanhThuToanHeThongResponse toanHeThong) {}
```

### 5.4. Repository — gọi proc nhiều result set bằng `SimpleJdbcCall`

`SimpleJdbcCall.returningResultSet(...)` map **lần lượt** từng result set theo thứ tự khai báo (trùng thứ tự `SELECT` trong proc). Vì proc dùng **bảng tạm**, JDBC metadata dễ lỗi → tắt metadata và **khai báo tham số tường minh**.

```java
// repository/ThongKeRepository.java
@Repository
public class ThongKeRepository {

    private final SimpleJdbcCall call;

    public ThongKeRepository(JdbcTemplate jdbcTemplate) {
        this.call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_ThongKeDoanhThu_ToanHeThong")
                .withoutProcedureColumnMetaDataAccess()      // proc có temp table → tự khai báo
                .declareParameters(
                        new SqlParameter("TuNgay", Types.TIMESTAMP),
                        new SqlParameter("DenNgay", Types.TIMESTAMP),
                        new SqlParameter("MaKho", Types.VARCHAR),
                        new SqlParameter("MaKhuVuc", Types.VARCHAR),
                        new SqlParameter("MaSP", Types.VARCHAR),
                        new SqlParameter("ChiTinhDaXuat", Types.BIT))
                .returningResultSet("theoKho", (rs, i) -> new DoanhThuTheoKhoResponse(
                        rs.getString("SiteXuat"), rs.getString("MaKhuVuc"),
                        rs.getString("MaKhoXuat"), rs.getString("TenKho"),
                        rs.getInt("SoDonHang"), rs.getInt("SoPhieuXuat"),
                        rs.getLong("TongSoLuongXuat"), nz(rs.getBigDecimal("DoanhThu"))))
                .returningResultSet("theoVung", (rs, i) -> new DoanhThuTheoVungResponse(
                        rs.getString("MaKhuVuc"), rs.getInt("SoDonHang"),
                        rs.getInt("SoPhieuXuat"), rs.getInt("SoKhoThamGiaXuat"),
                        rs.getLong("TongSoLuongXuat"), nz(rs.getBigDecimal("DoanhThu"))))
                .returningResultSet("toanHeThong", (rs, i) -> new DoanhThuToanHeThongResponse(
                        rs.getInt("TongSoDonHang"), rs.getInt("TongSoPhieuXuat"),
                        rs.getInt("TongSoKhoThamGiaXuat"), rs.getLong("TongSoLuongXuat"),
                        nz(rs.getBigDecimal("TongDoanhThu"))));
    }

    @SuppressWarnings("unchecked")
    public ThongKeDoanhThuResponse thongKeDoanhThu(ThongKeFilter f) {
        var params = new MapSqlParameterSource()
                .addValue("TuNgay", f.tuNgay())   // LocalDateTime hoặc null
                .addValue("DenNgay", f.denNgay())
                .addValue("MaKho", f.maKho())
                .addValue("MaKhuVuc", f.maKhuVuc())
                .addValue("MaSP", f.maSP())
                .addValue("ChiTinhDaXuat", f.chiTinhDaXuat());

        Map<String, Object> out = call.execute(params);

        var theoKho = (List<DoanhThuTheoKhoResponse>) out.getOrDefault("theoKho", List.of());
        var theoVung = (List<DoanhThuTheoVungResponse>) out.getOrDefault("theoVung", List.of());
        var thtList = (List<DoanhThuToanHeThongResponse>) out.getOrDefault("toanHeThong", List.of());
        var toanHeThong = thtList.isEmpty()
                ? new DoanhThuToanHeThongResponse(0, 0, 0, 0L, BigDecimal.ZERO)
                : thtList.get(0);

        return new ThongKeDoanhThuResponse(theoKho, theoVung, toanHeThong);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
```

> `rs.getInt`/`getLong` trả `0` khi cột NULL — phù hợp cho count/sum số nguyên. `BigDecimal` cần `nz(...)` vì `getBigDecimal` trả `null`.
> `ThongKeFilter` là một record gói tham số (xem 5.5). Truyền `null` cho các tham số không lọc — proc xử lý `IS NULL`.

### 5.5. Service

```java
// record gói filter
public record ThongKeFilter(
        LocalDateTime tuNgay, LocalDateTime denNgay,
        String maKho, String maKhuVuc, String maSP, boolean chiTinhDaXuat) {}

public interface ThongKeService {
    ThongKeDoanhThuResponse thongKeDoanhThu(ThongKeFilter filter);
}
```

Impl chỉ ủy quyền cho `ThongKeRepository.thongKeDoanhThu(filter)`. Chuyển `tuNgay`/`denNgay` từ `LocalDate` (query param) sang `LocalDateTime` (đầu ngày) nếu cần, hoặc nhận thẳng `LocalDate` và để `null` khi không truyền.

### 5.6. Controller (`AdminThongKeController` mới)

```java
@RestController
@RequestMapping("/api/admin/thong-ke")
@SecurityRequirement(name = "bearerAuth")
public class AdminThongKeController {

    private final ThongKeService thongKeService;
    // ctor...

    @GetMapping("/doanh-thu")
    public ResponseEntity<ApiResponse<ThongKeDoanhThuResponse>> doanhThu(
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate denNgay,
            @RequestParam(required = false) String maKho,
            @RequestParam(required = false) String maKhuVuc,
            @RequestParam(required = false) String maSP,
            @RequestParam(required = false, defaultValue = "true") boolean chiTinhDaXuat) {

        var filter = new ThongKeFilter(
                tuNgay == null ? null : tuNgay.atStartOfDay(),
                denNgay == null ? null : denNgay.atStartOfDay(),
                maKho, maKhuVuc, maSP, chiTinhDaXuat);
        return ResponseEntity.ok(ApiResponse.ok(thongKeService.thongKeDoanhThu(filter)));
    }
}
```

### 5.7. Xử lý lỗi
- Linked server `SITE_BAC`/`SITE_NAM` down → `DataAccessException` → `GlobalExceptionHandler` nhánh chung → `500 INTERNAL_ERROR`. *Tùy chọn:* exception riêng `ThongKeUnavailableException` để message thân thiện.
- Tham số ngày sai định dạng → Spring ném `MethodArgumentTypeMismatchException` → đảm bảo handler map về `VALIDATION_ERROR` (400).

---

## 6. (Tùy chọn) Thống kê đếm nhanh

Nếu dashboard cần các thẻ đếm đơn giản (không doanh thu), thêm `GET /api/admin/thong-ke/tong-quan` dùng `count()` của các repository hiện có (`SanPhamCoreRepository`, `DanhMucRepository`, `ThuongHieuRepository`, `NguoiDungRepository`) — không cần linked server, rẻ. Response: `{ tongSanPham, sanPhamDangBan, sanPhamNgungBan, tongDanhMuc, tongThuongHieu, tongNguoiDung }`. Để **tách biệt** với thống kê doanh thu (nặng, cross-site).

---

## 7. Tổng hợp endpoint

| Method | Path | Auth | data |
|---|---|---|---|
| POST | `/api/products` | ADMIN | `ProductDetailResponse` |
| PUT | `/api/products/{maSP}` | ADMIN | `ProductDetailResponse` |
| DELETE | `/api/products/{maSP}` | ADMIN | `null` |
| POST | `/api/categories` | ADMIN | `CategoryResponse` |
| PUT | `/api/categories/{maDanhMuc}` | ADMIN | `CategoryResponse` |
| DELETE | `/api/categories/{maDanhMuc}` | ADMIN | `null` |
| POST | `/api/brands` | ADMIN | `BrandResponse` |
| PUT | `/api/brands/{maThuongHieu}` | ADMIN | `BrandResponse` |
| DELETE | `/api/brands/{maThuongHieu}` | ADMIN | `null` |
| GET | `/api/admin/thong-ke/doanh-thu` | ADMIN | `ThongKeDoanhThuResponse` |
| GET | `/api/admin/thong-ke/tong-quan` *(tùy chọn)* | ADMIN | `ThongKeTongQuanResponse` |

---

## 8. Checklist triển khai

- [x] `SecurityConfig`: phân quyền ghi + `/api/admin/**` cho ROLE_ADMIN; xác nhận `CustomUserDetailsService` cấp `ROLE_ADMIN`.
- [x] Product: `ProductUpsertRequest`, 3 endpoint, service ghi core+detail (transaction), chính sách xoá (khuyến nghị soft-delete).
- [x] Category / Brand: request DTO + 3 endpoint + quy tắc xoá/`maDanhMucCha`.
- [x] Thống kê: DTO (3 record + wrapper), `ThongKeFilter`, `ThongKeRepository` (`SimpleJdbcCall` 3 result set, coalesce NULL), `ThongKeService`, `AdminThongKeController`.
- [x] `GlobalExceptionHandler`: phủ `AccessDeniedException`→403, `MethodArgumentTypeMismatchException`→400, `DataAccessException`→500.
- [ ] Swagger `@Operation`/`@ApiResponses` cho endpoint mới.
- [x] Test: controller (`@WebMvcTest`, có/không quyền ADMIN) cho CRUD catalog và thống kê. Integration test thủ công vẫn cần DB linked server thật.
- [x] Đồng bộ lại hợp đồng giả định trong [`FRONTEND_PHASE14_ADMIN.md`](FRONTEND_PHASE14_ADMIN.md) §1.4 — **thay** thống kê đếm bằng thống kê **doanh thu** ở trên (xem mục 9).

---

## 9. Đồng bộ với Frontend (PHASE 14)

Hợp đồng thống kê thực tế **khác** phần giả định ban đầu trong PHASE14 §1.4 (vốn là đếm số lượng). FE cần cập nhật:

```ts
// types/admin.ts — thống kê doanh thu
export interface DoanhThuTheoKho {
  site: string; maKhuVuc: string; maKho: string; tenKho: string;
  soDonHang: number; soPhieuXuat: number; tongSoLuongXuat: number; doanhThu: number;
}
export interface DoanhThuTheoVung {
  maKhuVuc: string; soDonHang: number; soPhieuXuat: number;
  soKhoThamGiaXuat: number; tongSoLuongXuat: number; doanhThu: number;
}
export interface DoanhThuToanHeThong {
  tongSoDonHang: number; tongSoPhieuXuat: number; tongSoKhoThamGiaXuat: number;
  tongSoLuongXuat: number; tongDoanhThu: number;
}
export interface ThongKeDoanhThu {
  theoKho: DoanhThuTheoKho[];
  theoVung: DoanhThuTheoVung[];
  toanHeThong: DoanhThuToanHeThong;
}
```

- **Endpoint FE:** `MAIN_ENDPOINTS.ADMIN_STATS_REVENUE = "/api/admin/thong-ke/doanh-thu"`, gọi qua `mainApiClient.get(..., { token, query: { tuNgay, denNgay, maKhuVuc, chiTinhDaXuat } })`.
- **Dashboard `/admin`:** thẻ lớn = `toanHeThong.tongDoanhThu` (+ số đơn, số lượng xuất); biểu đồ cột = `theoVung` (Bắc vs Nam); bảng chi tiết = `theoKho`. Thêm **bộ lọc khoảng ngày** + toggle `chiTinhDaXuat` (Đã xuất / Gồm dự kiến).
- **Lưu ý hiệu năng:** đây là truy vấn cross-site → đặt `staleTime` hợp lý, chỉ refetch khi đổi filter (đưa filter vào `queryKey`).
