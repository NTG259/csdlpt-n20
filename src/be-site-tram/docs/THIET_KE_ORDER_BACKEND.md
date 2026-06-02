# Thiết kế chi tiết — Tầng Java cho tính năng Đặt hàng (Order Controller + Service)

> **Phạm vi:** tài liệu này mô tả cách hiện thực **tầng Java (Spring Boot)** cho luồng đặt hàng đã thiết kế ở [THIET_KE_TAO_DON_HANG.md](THIET_KE_TAO_DON_HANG.md). Nó **không** thay đổi thiết kế SQL/SP — chỉ định nghĩa controller, service, DTO, entity nhóm B, lớp gọi Stored Procedure (TVP), và ánh xạ lỗi, **bám đúng convention hiện có** của codebase (`ApiResponse`, `BusinessException` → `GlobalExceptionHandler`, `@AuthenticationPrincipal CustomUserDetails`, record DTO, đặt tên tiếng Việt).
>
> **Lưu ý vị trí chạy:** tính năng đặt hàng chạy ở **site khu vực** (Bắc `:8081` / Nam `:8082`), **không** ở Site Main. Trong workspace hiện chỉ có Site Main; doc này là bản thiết kế dùng lại **nguyên template** của Site Main cho service khu vực. Khi dựng site khu vực, copy hạ tầng (`common/`, `exception/`, `security/`, `config/`) rồi bổ sung các lớp dưới đây.
>
> **Căn cứ quyết định:** tôn trọng 8 mục đã khóa ở [§12 THIET_KE_TAO_DON_HANG](THIET_KE_TAO_DON_HANG.md#12-quyết-định-đã-chốt). Đáng chú ý: items lấy **từ giỏ server** (mục 5); truyền **TVP trực tiếp** không né bằng JSON (mục 6); FE định tuyến theo `maKhuVuc` gọi thẳng site khu vực (mục 7); cùng tên SP `sp_DatHang_TuSiteBac_ModelB` ở cả 2 DB (mục 4).

---

## 1. Tổng quan luồng & phân vai tầng

Java đóng vai **điều phối mỏng** (theo [§6](THIET_KE_TAO_DON_HANG.md#6-dàn-xếp-orchestration--đã-hiện-thực-ở-sp-0)): không mở transaction riêng, không tính tồn kho — chỉ chuẩn bị đầu vào, gọi SP, đọc kết quả, map lỗi.

```
POST /api/orders  (Bearer, role USER)
   │  @AuthenticationPrincipal → maND (UUID), maKhuVuc ("Bac"/"Nam")
   ▼
OrderController.taoDonHang(userDetails, TaoDonHangRequest)
   ▼
OrderService.taoDonHang(maND, maKhuVuc, request)
   ├─ 1. Đọc giỏ server: ChiTietGioHangRepository.findActiveItems(maND)
   │      → rỗng ⇒ throw BusinessException(CART_EMPTY)
   ├─ 2. Dựng TVP @Items (OrderItemType) từ các dòng giỏ  (SQLServerDataTable)
   ├─ 3. EXEC sp_ChonKhoNhan_ToiUu(@MaKhuVucXuLi, @Items) → @MaKhoNhan
   ├─ 4. EXEC sp_DatHang_TuSiteBac_ModelB(... @Items, @MaDonHang OUTPUT)
   │      → result set (MaDonHang, TongTien, ThongBao); OUTPUT @MaDonHang
   ├─ 5. Đánh dấu giỏ đã đặt: GioHang.TrangThai='ordered'
   │      (làm trong SP nếu có; nếu không, update ở repo sau khi SP commit)
   └─ 6. Đọc lại đơn vừa tạo → map DonHangResponse (items, tổng tiền, trạng thái)
   ▼
ResponseEntity 201  ApiResponse<DonHangResponse>
```

**Ranh giới transaction:** SP `sp_DatHang_*` tự mở **một** `BEGIN TRANSACTION` bọc toàn bộ (kể cả nhánh `[LINK]`/MSDTC). Vì vậy **không** đặt `@Transactional` lên `OrderService.taoDonHang` bao quanh lời gọi SP — tránh lồng transaction Java vào transaction phân tán của SQL Server. Service chỉ là chuỗi lời gọi JDBC tuần tự; mỗi `SimpleJdbcCall` tự auto-commit phần của nó. (Bước 5 "đặt giỏ = ordered" tốt nhất **gộp trong SP** để cùng commit nguyên tử; nếu để Java làm sau, chấp nhận khe nhỏ — xem [§7](#7-điểm-cần-quyết-trước-khi-code).)

---

## 2. Hạ tầng cần bổ sung (build & cấu hình)

### 2.1 `build.gradle.kts` — nâng `mssql-jdbc` lên compile scope
Hiện tại driver là `runtimeOnly` (chỉ JPA dùng). Để **truyền TVP** ta cần tham chiếu trực tiếp `com.microsoft.sqlserver.jdbc.SQLServerDataTable` và `microsoft.sql.Types.STRUCTURED` ở compile-time:

```kotlin
// THAY runtimeOnly("com.microsoft.sqlserver:mssql-jdbc") bằng:
implementation("com.microsoft.sqlserver:mssql-jdbc")
```

`spring-boot-starter-data-jpa` đã kéo `spring-jdbc` (có `JdbcTemplate`, `SimpleJdbcCall`) nên **không cần** thêm dependency JDBC khác.

### 2.2 Bean JDBC
`DataSource` đã do Spring Boot autoconfigure (từ `application-dev.yml`). Khai báo `JdbcTemplate` qua constructor injection của `DataSource` trong DAO — không cần `@Bean` thủ công.

### 2.3 Security — không cần sửa `SecurityConfig`
`anyRequest().authenticated()` đã bao `/api/orders` (USER có token hợp lệ là gọi được). Các endpoint Pha B (kho) siết role bằng `@PreAuthorize("hasRole('WAREHOUSE_STAFF')")` — `@EnableMethodSecurity` đã bật. **Không** thêm rule path mới.

---

## 3. Entity JPA nhóm B (đọc lại đơn / Pha B)

Toàn bộ **ghi** do SP lo. Java chỉ cần entity để **đọc lại** (map response) và cho các endpoint Pha B liệt kê/đọc phiếu. Map vào schema có sẵn (`ddl-auto: validate`), theo đúng style hiện tại: Lombok `@Getter/@Setter`, UUID dùng `@JdbcTypeCode(SqlTypes.UUID)`, boolean `TINYINT` qua `BooleanToTinyIntConverter`, quan hệ `LAZY`.

| Entity | Bảng | PK | Ghi chú |
|---|---|---|---|
| `DonHang` | `DonHang` | `MaDonHang` UUID | `trangThaiDH`, `trangThaiTT`, `tongTien`, `ngayDat`, `maKhuVucXuLi`, FK `maND` |
| `ChiTietDonHang` | `ChiTietDonHang` | `MaCTDH` UUID | FK `MaDonHang`, `maSP`, `soLuong`, `donGia` (giá chốt lúc đặt) |
| `PhieuXuatKho` | `PhieuXuatKho` | `MaPhieuXuat` UUID | `maDonHang?`, `maKhoXuat`, `maKhoNhan?`, `trangThaiXuat`, `trangThaiNhan?` |
| `ChiTietXuatKho` | `ChiTietXuatKho` | `MaCTXK` | FK phiếu xuất, `maSP`, `soLuongXuat` |
| `PhieuNhapKho` | `PhieuNhapKho` | `MaPhieuNhap` UUID | `maDonHang?`, `maKhoXuat`, `maKhoNhap`, `trangThaiNhap`, `maNhanVienNhap?`, `ngayNhap?` |
| `ChiTietPhieuNhap` | `ChiTietPhieuNhap` | `MaCTPN` | FK phiếu nhập, `maSP`, `soLuong` |

> **Tối thiểu hóa:** cho luồng `POST /api/orders`, chỉ **`DonHang` + `ChiTietDonHang`** là bắt buộc (để đọc lại đơn). Bốn entity phiếu chỉ cần khi làm endpoint Pha B — có thể tạo dần. `TonKho`/`Kho` **không** cần entity ở luồng đặt hàng (đã có `TonKhoService` đọc tồn nếu cần hiển thị).

Ví dụ entity rút gọn (đúng khuôn `NguoiDung`/`GioHang`):

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "DonHang")
public class DonHang {
    @Id @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaDonHang", columnDefinition = "uniqueidentifier")
    private UUID maDonHang;

    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "MaND", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID maND;

    @Column(name = "HoTenNguoiNhan", length = 100) private String hoTenNguoiNhan;
    @Column(name = "SoDienThoaiNhan", length = 15) private String soDienThoaiNhan;
    @Column(name = "DiaChiGiao", length = 300)     private String diaChiGiao;
    @Column(name = "MaKhuVucXuLi", length = 10)     private String maKhuVucXuLi;
    @Column(name = "TongTien")                      private BigDecimal tongTien;
    @Column(name = "PhuongThucTT", length = 50)     private String phuongThucTT;
    @Column(name = "TrangThaiTT", length = 30)      private String trangThaiTT;
    @Column(name = "TrangThaiDH", length = 30)      private String trangThaiDH;
    @Column(name = "GhiChu", length = 500)          private String ghiChu;
    @Column(name = "NgayDat")                       private LocalDateTime ngayDat;

    @OneToMany(mappedBy = "donHang", fetch = FetchType.LAZY)
    private List<ChiTietDonHang> chiTietList = new ArrayList<>();
}
```

> Tên cột chính xác phải khớp DDL thật của site khu vực — **đối chiếu schema** trước khi commit (Hibernate `validate` sẽ fail nếu lệch).

---

## 4. DTO

### 4.1 Request — `dto/request/TaoDonHangRequest.java`
Record + Bean Validation (giống `ThemVaoGioRequest`). **Không nhận `items`** (lấy từ giỏ server — [§7/§12 mục 5](THIET_KE_TAO_DON_HANG.md#7-hợp-đồng-api-site-khu-vực)).

```java
public record TaoDonHangRequest(
    @NotBlank(message = "Họ tên người nhận không được để trống")
    @Size(max = 100) String hoTenNguoiNhan,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại không hợp lệ")
    String soDienThoaiNhan,

    @NotBlank(message = "Địa chỉ giao không được để trống")
    @Size(max = 300) String diaChiGiao,

    @NotBlank @Pattern(regexp = "COD", message = "Hiện chỉ hỗ trợ COD")
    String phuongThucTT,

    @Size(max = 500) String ghiChu
) {}
```

### 4.2 Response — `dto/response/DonHangResponse.java` + `ChiTietDonHangResponse.java`
Record thuần (giống `GioHangResponse`). Jackson `non_null` sẽ tự bỏ field null.

```java
public record DonHangResponse(
    UUID maDonHang,
    String trangThaiDH,        // "processing"
    String trangThaiTT,        // "waiting_cod"
    BigDecimal tongTien,
    LocalDateTime ngayDat,
    String khuVucXuLi,         // "Bac" / "Nam"
    List<ChiTietDonHangResponse> items
) {}

public record ChiTietDonHangResponse(
    String maSP, String tenSP, int soLuong,
    BigDecimal donGia, BigDecimal thanhTien
) {}
```

---

## 5. Lớp gọi Stored Procedure (TVP) — `repository/OrderStoredProcedureDao`

Đây là phần kỹ thuật cốt lõi: **marshalling TVP `dbo.OrderItemType`** và đọc OUTPUT + result set. Dùng `SimpleJdbcCall` với `microsoft.sql.Types.STRUCTURED`.

```java
@Repository
public class OrderStoredProcedureDao {

    private final JdbcTemplate jdbcTemplate;

    public OrderStoredProcedureDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Dựng TVP từ các dòng giỏ. Cột PHẢI khớp dbo.OrderItemType (MaSP VARCHAR(20), SoLuong INT).
    private SQLServerDataTable buildItemsTvp(List<GioHangItem> items) throws SQLServerException {
        SQLServerDataTable tvp = new SQLServerDataTable();
        tvp.addColumnMetadata("MaSP", java.sql.Types.VARCHAR);
        tvp.addColumnMetadata("SoLuong", java.sql.Types.INTEGER);
        for (GioHangItem it : items) {
            tvp.addRow(it.maSP(), it.soLuong());
        }
        return tvp;
    }

    // Bước 3: chọn kho gom tối ưu
    public String chonKhoNhanToiUu(String maKhuVucXuLi, List<GioHangItem> items) {
        try {
            SQLServerDataTable tvp = buildItemsTvp(items);
            SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_ChonKhoNhan_ToiUu")
                .declareParameters(
                    new SqlParameter("MaKhuVucXuLi", Types.VARCHAR),
                    new SqlParameter("Items", microsoft.sql.Types.STRUCTURED),
                    new SqlOutParameter("MaKhoNhan", Types.VARCHAR));
            Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("MaKhuVucXuLi", maKhuVucXuLi)
                .addValue("Items", tvp));
            return (String) out.get("MaKhoNhan");
        } catch (SQLServerException e) {
            throw new DataAccessResourceFailureException("TVP build lỗi", e);
        }
    }

    // Bước 4: tạo đơn (master SP)
    public KetQuaDatHang datHang(DatHangParams p, List<GioHangItem> items) {
        try {
            SQLServerDataTable tvp = buildItemsTvp(items);
            SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_DatHang_TuSiteBac_ModelB")  // CÙNG TÊN ở cả 2 DB (§12 mục 4)
                .declareParameters(
                    new SqlParameter("MaND", Types.OTHER),         // UNIQUEIDENTIFIER ← UUID
                    new SqlParameter("HoTenNguoiNhan", Types.NVARCHAR),
                    new SqlParameter("SoDienThoaiNhan", Types.VARCHAR),
                    new SqlParameter("DiaChiGiao", Types.NVARCHAR),
                    new SqlParameter("MaKhuVucXuLi", Types.VARCHAR),
                    new SqlParameter("MaKhoNhan", Types.VARCHAR),
                    new SqlParameter("MaKhoUuTien", Types.VARCHAR),
                    new SqlParameter("PhuongThucTT", Types.VARCHAR),
                    new SqlParameter("Items", microsoft.sql.Types.STRUCTURED),
                    new SqlParameter("GhiChu", Types.NVARCHAR),
                    new SqlOutParameter("MaDonHang", Types.OTHER));
            Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("MaND", p.maND())
                .addValue("HoTenNguoiNhan", p.hoTen())
                .addValue("SoDienThoaiNhan", p.sdt())
                .addValue("DiaChiGiao", p.diaChi())
                .addValue("MaKhuVucXuLi", p.maKhuVuc())
                .addValue("MaKhoNhan", p.maKhoNhan())
                .addValue("MaKhoUuTien", null)
                .addValue("PhuongThucTT", "COD")
                .addValue("Items", tvp)
                .addValue("GhiChu", p.ghiChu()));
            // OUTPUT + result set (MaDonHang, TongTien, ThongBao)
            UUID maDon = (UUID) out.get("MaDonHang");
            return new KetQuaDatHang(maDon /*, TongTien/ThongBao nếu đọc resultSet*/);
        } catch (DataAccessException e) {
            throw e;  // SQLServerException (RAISERROR) bọc trong DataAccessException → translator ở §6
        }
    }
}
```

**Ghi chú kỹ thuật:**
- `microsoft.sql.Types.STRUCTURED` là hằng riêng của driver (không có trong `java.sql.Types`) — đây là lý do phải đưa `mssql-jdbc` lên `implementation` ([§2.1](#21-buildgradlekts--nâng-mssql-jdbc-lên-compile-scope)).
- `UUID` → tham số `UNIQUEIDENTIFIER`: dùng `Types.OTHER`, driver mssql tự nhận `java.util.UUID`. (Nếu driver phiên bản cũ không nhận, truyền `maND.toString()` với `Types.CHAR(36)`.)
- **Hai lần dựng TVP** (cho `sp_ChonKhoNhan_ToiUu` rồi `sp_DatHang`) từ **cùng** `List<GioHangItem>` — `SQLServerDataTable` không tái sử dụng được giữa hai call, dựng lại mỗi lần.
- Nếu thực sự muốn né TVP: gọi biến thể `*_Json` (OPENJSON) như đã nêu ở [§8 design](THIET_KE_TAO_DON_HANG.md#8-tích-hợp-java) — **nhưng đã chốt truyền TVP trực tiếp** ([§12 mục 6](THIET_KE_TAO_DON_HANG.md#12-quyết-định-đã-chốt)).

---

## 6. Ánh xạ lỗi RAISERROR → ErrorCodes

SP phát lỗi nghiệp vụ bằng `RAISERROR(N'...', 16, 1)` → mssql-jdbc ném `SQLServerException` (Spring bọc thành `DataAccessException`/`UncategorizedSQLException`). Service bắt và dịch sang `BusinessException` để `GlobalExceptionHandler` trả `ErrorResponse` chuẩn — **không lộ message thô**.

### 6.1 Bổ sung `common/ErrorCodes.java`
```java
public static final String CART_EMPTY        = "CART_EMPTY";
public static final String OUT_OF_STOCK      = "OUT_OF_STOCK";
public static final String PRODUCT_INVALID   = "PRODUCT_INVALID";
public static final String ORDER_NOT_FOUND   = "ORDER_NOT_FOUND";
public static final String INVALID_ORDER_STATE = "INVALID_ORDER_STATE";   // Pha B: sai trạng thái phiếu/đơn
public static final String SLIP_NOT_FOUND    = "SLIP_NOT_FOUND";          // Pha B: không thấy phiếu
public static final String PAYMENT_NOT_SUPPORTED = "PAYMENT_NOT_SUPPORTED"; // != COD
```

### 6.2 Bộ dịch — `exception/SqlServerErrorTranslator`
Khớp **mẫu chuỗi tiếng Việt** trong message của các SP (đã đọc nguyên văn ở các file `proceduce/*.sql`) → mã lỗi + HTTP status:

| Mẫu trong message SP | ErrorCode | HTTP |
|---|---|---|
| "không đủ hàng" / "Tồn kho … không đủ" / "đã giữ không đủ" | `OUT_OF_STOCK` | 409 |
| "không tồn tại" / "ngừng kinh doanh" / "sản phẩm … không hợp lệ" | `PRODUCT_INVALID` | 422 |
| "phải có ít nhất một sản phẩm" | `CART_EMPTY` | 400 |
| "chỉ hỗ trợ … COD" | `PAYMENT_NOT_SUPPORTED` | 400 |
| "Không tìm thấy phiếu" | `SLIP_NOT_FOUND` | 404 |
| "Chỉ được xác nhận … trạng thái" / "chưa exported" | `INVALID_ORDER_STATE` | 409 |
| *(còn lại)* | `INTERNAL_ERROR` | 500 (log nguyên văn) |

```java
public final class SqlServerErrorTranslator {
    public static BusinessException translate(DataAccessException ex) {
        String msg = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
        String low = msg == null ? "" : msg.toLowerCase(Locale.ROOT);
        if (low.contains("không đủ"))        return biz(ErrorCodes.OUT_OF_STOCK, HttpStatus.CONFLICT, "Sản phẩm không đủ hàng trong khu vực");
        if (low.contains("không tồn tại") || low.contains("ngừng kinh doanh"))
                                             return biz(ErrorCodes.PRODUCT_INVALID, HttpStatus.UNPROCESSABLE_ENTITY, "Sản phẩm không hợp lệ");
        if (low.contains("ít nhất một sản phẩm")) return biz(ErrorCodes.CART_EMPTY, HttpStatus.BAD_REQUEST, "Giỏ hàng trống");
        if (low.contains("cod"))             return biz(ErrorCodes.PAYMENT_NOT_SUPPORTED, HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ thanh toán COD");
        if (low.contains("không tìm thấy phiếu")) return biz(ErrorCodes.SLIP_NOT_FOUND, HttpStatus.NOT_FOUND, "Không tìm thấy phiếu");
        if (low.contains("chỉ được xác nhận") || low.contains("chưa exported"))
                                             return biz(ErrorCodes.INVALID_ORDER_STATE, HttpStatus.CONFLICT, "Phiếu không ở trạng thái cho phép thao tác");
        return biz(ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý đơn hàng");
    }
    private static BusinessException biz(String code, HttpStatus s, String m){ return new BusinessException(code, s, m); }
}
```

> Khớp theo **chuỗi message** là điểm dễ vỡ (đổi câu chữ trong SP là lệch). Đây là đánh đổi chấp nhận được cho đồ án; bền hơn thì cho mỗi `RAISERROR` một **`@ErrState` riêng** (số) và switch theo `SQLServerException.getSQLState()`/state — ghi nhận như cải tiến tương lai.

---

## 7. Điểm cần quyết trước khi code

1. **"Đặt giỏ = ordered" ở đâu?** Lý tưởng là **trong `sp_DatHang`** (cùng commit, nguyên tử). Nếu SP hiện chưa làm, hai lựa chọn: (a) thêm `UPDATE GioHang SET TrangThai='ordered'` vào SP — *khuyến nghị*; (b) để Java update sau khi SP trả về (`GioHangRepository`), chấp nhận khe nhỏ nếu app chết giữa chừng → đơn đã tạo nhưng giỏ chưa đóng (an toàn hơn so với ngược lại). **Cần xác nhận** SP hiện đã có bước này chưa.
2. **`@MaKhoUuTien`** hiện truyền `null` (để SP tự rải theo `sp_ChonKhoNhan_ToiUu`). Nếu sau này UI cho chọn kho ưu tiên thì thêm vào request.
3. **Đọc lại đơn để dựng response:** dùng `DonHang` entity + `findByIdWithItems` (join-fetch `ChiTietDonHang` + tên SP) — hoặc đọc luôn từ result set cuối của SP (`TongTien`, `ThongBao`) + 1 query items. Khuyến nghị **đọc lại qua JPA** cho gọn (đơn vừa commit, chắc chắn thấy).

---

## 8. Endpoint Pha B (nhân viên kho — `WAREHOUSE_STAFF`)

Mỗi SP xác nhận đã đọc nguyên văn → mỗi cái một endpoint mỏng. Tất cả `@PreAuthorize("hasRole('WAREHOUSE_STAFF')")`, gọi qua `WarehouseStoredProcedureDao` (cùng pattern `SimpleJdbcCall`, không TVP).

| Endpoint | SP gọi | Tham số | Ghi chú |
|---|---|---|---|
| `POST /api/warehouse/xuat-noi-bo/{maPhieuXuat}/xac-nhan` | `sp_XacNhanXuat_NoiBo` | `@MaPhieuXuat` | `waiting_export→exported`, trừ tồn kho nguồn |
| `POST /api/warehouse/nhap-noi-bo/{maPhieuNhap}/xac-nhan` | `sp_XacNhanNhap_NoiBo` | `@MaPhieuNhap`, `@MaNhanVienNhap`(=`userId` từ JWT) | cộng tồn kho gom; kiểm phiếu nguồn `exported` (có thể qua `[LINK]`→MSDTC) |
| `POST /api/warehouse/xuat-giao-khach/{maPhieuXuat}/xac-nhan` | `sp_XacNhanXuat_GiaoKhach` | `@MaPhieuXuat` | trừ tồn kho gom; `processing→shipping` |

- `@MaNhanVienNhap` **luôn lấy từ `userDetails.getUserId()`**, không nhận từ client (tránh giả mạo nhân viên).
- Response: `ApiResponse<XacNhanResponse>` với `{maPhieu, thongBao}` đọc từ result set cuối của SP.
- Endpoint **liệt kê phiếu chờ** (gợi ý, cho UI kho): `GET /api/warehouse/phieu-xuat?trangThai=waiting_export`, `GET /api/warehouse/phieu-nhap?trangThai=waiting_import` — đọc thuần JPA, lọc theo kho phụ trách của nhân viên (`NguoiDung.maKhoPhuTrach`).
- Lỗi SP (`RAISERROR`) đi qua **cùng** `SqlServerErrorTranslator` → `SLIP_NOT_FOUND` / `INVALID_ORDER_STATE`.

---

## 9. Controller — `OrderController`

Bám đúng khuôn `GioHangController` (constructor injection, `@AuthenticationPrincipal`, `ApiResponse.ok`, Swagger annotations).

```java
@Tag(name = "Đơn hàng", description = "Đặt hàng — yêu cầu Bearer token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    public OrderController(OrderService orderService){ this.orderService = orderService; }

    @Operation(summary = "Đặt hàng từ giỏ", description = "Tạo đơn từ giỏ hàng phía server của người dùng.")
    @PostMapping
    public ResponseEntity<ApiResponse<DonHangResponse>> taoDonHang(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TaoDonHangRequest request) {
        DonHangResponse data = orderService.taoDonHang(
                userDetails.getUserId(), userDetails.getMaKhuVuc(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đặt hàng thành công", data));
    }
}
```

`OrderService` (interface) + `OrderServiceImpl` (impl) — đặt logic [§1](#1-tổng-quan-luồng--phân-vai-tầng) trong impl; bắt `DataAccessException` quanh các lời gọi DAO và `throw SqlServerErrorTranslator.translate(ex)`. Validate "giỏ rỗng" **trước** khi gọi SP (`throw new BusinessException(CART_EMPTY, BAD_REQUEST, ...)`).

---

## 10. Kiểm thử

| Lớp | Cách test | Lưu ý |
|---|---|---|
| `OrderController` | `@WebMvcTest` + mock `OrderService` | giống `GioHangControllerTest`: kiểm 201, body envelope, 401 khi thiếu token, 400 khi validation fail |
| `OrderServiceImpl` | mock `OrderStoredProcedureDao` + `GioHangRepository` | kiểm: giỏ rỗng → `CART_EMPTY`; DAO ném `DataAccessException("…không đủ…")` → `OUT_OF_STOCK`; happy path map response đúng |
| `SqlServerErrorTranslator` | unit test thuần | bảng mẫu [§6.2](#62-bộ-dịch--exceptionsqlservererrortranslator): từng chuỗi → đúng code/HTTP |
| `OrderStoredProcedureDao` | **integration** (cần SQL Server thật + SP + TVP type) | không mock được TVP; chạy thủ công hoặc Testcontainers (ngoài phạm vi đồ án) |

Đăng ký test class mới vào danh sách trong [CLAUDE.md](../CLAUDE.md) (mục "Test classes that exist") khi tạo.

---

## 11. Checklist file cần tạo/sửa

**Sửa:**
- [ ] `build.gradle.kts` — `mssql-jdbc`: `runtimeOnly` → `implementation`
- [ ] `common/ErrorCodes.java` — thêm 7 mã ([§6.1](#61-bổ-sung-commonerrorcodesjava))
- [ ] (SP) xác nhận `sp_DatHang_*` có set `GioHang='ordered'` ([§7.1](#7-điểm-cần-quyết-trước-khi-code))

**Tạo — luồng đặt hàng (bắt buộc):**
- [ ] `domain/entity/DonHang.java`, `ChiTietDonHang.java`
- [ ] `repository/DonHangRepository.java` (`findByIdWithItems`), `ChiTietGioHangRepository` (đọc dòng giỏ active nếu chưa có query phù hợp)
- [ ] `dto/request/TaoDonHangRequest.java`
- [ ] `dto/response/DonHangResponse.java`, `ChiTietDonHangResponse.java`
- [ ] `repository/OrderStoredProcedureDao.java` (TVP + 2 SP)
- [ ] `exception/SqlServerErrorTranslator.java`
- [ ] `service/OrderService.java` + `service/impl/OrderServiceImpl.java`
- [ ] `controller/OrderController.java`

**Tạo — Pha B (kho, làm sau):**
- [ ] entity phiếu (`PhieuXuatKho`, `ChiTietXuatKho`, `PhieuNhapKho`, `ChiTietPhieuNhap`) + repo
- [ ] `repository/WarehouseStoredProcedureDao.java` (3 SP xác nhận)
- [ ] `service` + `controller/WarehouseController.java` (`@PreAuthorize WAREHOUSE_STAFF`)

**Test:**
- [ ] `OrderControllerTest`, `OrderServiceImplTest`, `SqlServerErrorTranslatorTest`

---

## 12. Liên hệ thiết kế gốc

| Mục design gốc | Phản ánh ở doc này |
|---|---|
| [§6 orchestration](THIET_KE_TAO_DON_HANG.md#6-dàn-xếp-orchestration--đã-hiện-thực-ở-sp-0) | [§1](#1-tổng-quan-luồng--phân-vai-tầng) (gọi `sp_ChonKhoNhan_ToiUu` trước `sp_DatHang`) |
| [§7 hợp đồng API](THIET_KE_TAO_DON_HANG.md#7-hợp-đồng-api-site-khu-vực) | [§4](#4-dto), [§9](#9-controller--ordercontroller) (request/response, 201, items từ giỏ) |
| [§8 tích hợp Java (TVP)](THIET_KE_TAO_DON_HANG.md#8-tích-hợp-java) | [§5](#5-lớp-gọi-stored-procedure-tvp--repositoryorderstoredproceduredao) |
| [§9 đồng thời / Pha B #7–#9](THIET_KE_TAO_DON_HANG.md#9-giao-dịch--đồng-thời) | [§8](#8-endpoint-pha-b-nhân-viên-kho--warehouse_staff) |
| [§12 quyết định đã chốt](THIET_KE_TAO_DON_HANG.md#12-quyết-định-đã-chốt) | TVP trực tiếp (m6), cùng tên SP (m4), items từ giỏ (m5), FE định tuyến khu vực (m7) |
