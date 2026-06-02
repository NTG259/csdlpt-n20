# PHASE 2 — THIẾT KẾ DOMAIN / ENTITY (chi tiết — đủ 18 bảng)

> **Phạm vi:** Mapping JPA cho **toàn bộ 18 bảng** trong DDL SQL Server, chia theo mô hình phân tán 3 site.
> **Tài liệu chỉ mô tả mapping (field ↔ cột) + lưu ý — KHÔNG viết code đầy đủ.**

> **Tiến độ cập nhật 2026-06-02:**
> - Đã triển khai xong Nhóm A cho Site Main (`KhuVuc`, `DanhMuc`, `ThuongHieu`, `SanPhamCore`, `SanPhamDetail`, `NguoiDung`, `UserGlobalIndex`)
> - `DanhMuc_ThuongHieu` đang được map qua `@ManyToMany @JoinTable`
> - Nhóm B vẫn giữ ở mức tài liệu tham chiếu, chưa code trong project này

## Phân nhóm theo site (đọc trước)

| Nhóm | Bảng | Site sở hữu | Backend cần code |
|---|---|---|---|
| **A — Dữ liệu dùng chung / xác thực** | KhuVuc, DanhMuc, ThuongHieu, DanhMuc_ThuongHieu, SanPham_Core, SanPham_Detail, NguoiDung, User_Global_Index | **Site Main** | ✅ Giai đoạn đầu |
| **B — Kho / giao dịch khu vực** | Kho, GioHang, ChiTietGioHang, DonHang, ChiTietDonHang, PhieuXuatKho, ChiTietXuatKho, TonKho, PhieuNhapKho, ChiTietPhieuNhap | **Site Bắc / Nam** (phân mảnh) | ⏳ Giai đoạn sau |

> **Lưu ý quan trọng:** Backend **Site Main giai đoạn đầu CHỈ code Nhóm A.** Nhóm B đưa vào đây làm **tham chiếu mapping đầy đủ** cho backend site khu vực (và để hiểu toàn cảnh phân tán khi viết báo cáo). Đừng tạo entity Nhóm B trong project Site Main lúc này.

---

## 0. Những điểm "đặc biệt" rút ra từ DDL (quyết định cách map)

| # | Vấn đề | Quyết định mapping |
|---|---|---|
| A | **2 kiểu khóa chính**: VARCHAR (mã nghiệp vụ) vs **UNIQUEIDENTIFIER** (GUID) | VARCHAR → `String` (tự gán). GUID → `java.util.UUID` + `@JdbcTypeCode(SqlTypes.UUID)` |
| B | **`NguoiDung.MaND` = `User_Global_Index.MaND`** (cùng user, 2 bảng, KHÔNG FK) | Sinh `UUID` **1 lần ở service**, gán cho cả 2. KHÔNG để DB `NEWID()` riêng từng bảng |
| C | **`TrangThai TINYINT CHECK (0,1)`** (nhiều bảng) | `Boolean` + converter TinyInt riêng (`BooleanToTinyIntConverter`) để Hibernate validate đúng với SQL Server `TINYINT` |
| D | **Cột trạng thái VARCHAR có CHECK IN (...)** (VaiTro, GioHang, DonHang, PhieuXuat/Nhap) | Enum + `@Enumerated(EnumType.STRING)` (khuyến nghị) — hoặc `String` cho nhanh |
| E | **Quan hệ 1-1 chia sẻ khóa** (`SanPham_Detail.MaSP`) | `@OneToOne` + `@MapsId` + `@JoinColumn` |
| F | **Bảng nối chỉ có 2 cột khóa** (`DanhMuc_ThuongHieu`) | `@ManyToMany` + `@JoinTable` (không cần entity riêng) |
| G | **Tự tham chiếu** (`DanhMuc.MaDanhMucCha`) | `@ManyToOne` trỏ chính nó |
| H | **FK liên site bị cố ý BỎ** (PhieuXuatKho.MaDonHang/MaKhoNhan, ChiTietXuatKho.MaCTDH) | Map **cột thường** (`UUID`/`String`), **KHÔNG** tạo `@ManyToOne`. Đây là **khóa logic** phân tán |
| I | **Cột default phía DB** (NgayTao, NgayDangKy, NgayCapNhat...) | `@Column(insertable=false, updatable=false)` để DB điền — hoặc `@CreationTimestamp` |
| J | **`ThongSoKyThuat` NVARCHAR(MAX) JSON** (CHECK ISJSON) | `String` (JSON thô); parse ở service nếu cần |
| K | **`ThanhTien AS (SoLuong*DonGia) PERSISTED`** — cột tính toán | `BigDecimal` + `@Column(insertable=false, updatable=false)` + `@org.hibernate.annotations.Generated` (chỉ đọc) |
| L | **`RowVer ROWVERSION`** (TonKho) — chống ghi đè đồng thời | `byte[]` + `@Version` (optimistic locking) |
| M | **Filtered unique index** `UX_GioHang_Active ... WHERE TrangThai='active'` | JPA KHÔNG diễn đạt được → **DB tự enforce**; service phải query đúng giỏ `active` |
| N | **`DECIMAL(15,2)`** | `BigDecimal` (KHÔNG dùng `double`) |
| O | **Dữ liệu tiếng Việt** `NVARCHAR` | JVM/file UTF-8; `NVARCHAR(MAX)` dùng `@Column(columnDefinition="NVARCHAR(MAX)")` hoặc `@Lob` |
| P | **Bẫy tên cột khu vực**: `NguoiDung.MaKV` vs `User_Global_Index.MaKhuVuc` vs `DonHang.MaKhuVucXuLi` | Map đúng `@JoinColumn(name=...)` từng bảng, đừng nhầm |

### Nguyên tắc FK trong CSDL phân tán (rất quan trọng cho báo cáo)
DDL thể hiện rõ quy tắc:
- **Tham chiếu trong cùng site → GIỮ FK** (vd `ChiTietDonHang→DonHang`, `TonKho→Kho`, `PhieuXuatKho→Kho xuất`).
- **Tham chiếu tới dữ liệu nhân bản toàn phần → GIỮ FK** (mọi bảng tham chiếu `SanPham_Core`, vì sản phẩm được nhân bản về mọi site).
- **Tham chiếu có thể trỏ sang site khác → BỎ FK, dùng khóa logic** (`PhieuXuatKho.MaDonHang`, `PhieuXuatKho.MaKhoNhan`, `ChiTietXuatKho.MaCTDH`).

⟹ Khi map entity: khóa logic = field giá trị (`UUID`/`String`), KHÔNG dùng quan hệ JPA.

---

## ════════ NHÓM A — SITE MAIN (8 bảng, code giai đoạn đầu) ════════

### A1. `KhuVuc` → `KhuVuc`
| Cột DDL | Field | Kiểu | Annotation |
|---|---|---|---|
| MaKhuVuc VARCHAR(10) PK | `maKhuVuc` | String | `@Id @Column(name="MaKhuVuc", length=10)` — tự gán |
| TenKhuVuc NVARCHAR(100) | `tenKhuVuc` | String | `@Column(nullable=false, length=100)` |

### A2. `ThuongHieu` → `ThuongHieu`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaThuongHieu VARCHAR(20) PK | `maThuongHieu` | String | `@Id` tự gán |
| TenThuongHieu NVARCHAR(100) UNIQUE | `tenThuongHieu` | String | `@Column(nullable=false, unique=true)` |
| TrangThai TINYINT (0,1) | `trangThai` | Boolean | `@Convert(BooleanToTinyIntConverter.class)` |

### A3. `DanhMuc` → `DanhMuc` (tự tham chiếu + N-N ThuongHieu)
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaDanhMuc VARCHAR(20) PK | `maDanhMuc` | String | `@Id` tự gán |
| TenDanhMuc NVARCHAR(100) | `tenDanhMuc` | String | `@Column(nullable=false)` |
| MaDanhMucCha VARCHAR(20) *(null)* | `danhMucCha` | DanhMuc | `@ManyToOne(LAZY) @JoinColumn(name="MaDanhMucCha")` |
| MoTa NVARCHAR(500) *(null)* | `moTa` | String | |
| TrangThai TINYINT (0,1) | `trangThai` | Boolean | converter |

- N-N với ThuongHieu (qua `DanhMuc_ThuongHieu`):
  ```
  @ManyToMany(fetch=LAZY)
  @JoinTable(name="DanhMuc_ThuongHieu",
             joinColumns=@JoinColumn(name="MaDanhMuc"),
             inverseJoinColumns=@JoinColumn(name="MaThuongHieu"))
  private Set<ThuongHieu> thuongHieus;
  ```

### A4. `DanhMuc_ThuongHieu` — **KHÔNG tạo entity**
- Dùng `@ManyToMany @JoinTable` ở A3. Chỉ tạo entity riêng (`@EmbeddedId`) nếu sau này bảng có thêm cột phụ.

### A5. `SanPhamCore` → `SanPham_Core`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaSP VARCHAR(20) PK | `maSP` | String | `@Id` tự gán |
| TenSP NVARCHAR(255) | `tenSP` | String | `@Column(nullable=false)` |
| MaDanhMuc VARCHAR(20) FK | `danhMuc` | DanhMuc | `@ManyToOne(LAZY) @JoinColumn(name="MaDanhMuc", nullable=false)` |
| MaThuongHieu VARCHAR(20) FK | `thuongHieu` | ThuongHieu | `@ManyToOne(LAZY) @JoinColumn(name="MaThuongHieu", nullable=false)` |
| GiaBan DECIMAL(15,2) | `giaBan` | BigDecimal | `@Column(precision=15, scale=2, nullable=false)` |
| DonViTinh NVARCHAR(20) | `donViTinh` | String | default DB = N'Cái' |
| HinhAnh VARCHAR(500) *(null)* | `hinhAnh` | String | |
| TrangThai TINYINT (0,1) | `trangThai` | Boolean | converter |
| NgayTao DATETIME2 default | `ngayTao` | LocalDateTime | `@Column(insertable=false, updatable=false)` |
| (ngược) | `chiTiet` | SanPhamDetail | (tùy chọn) `@OneToOne(mappedBy="sanPhamCore", fetch=LAZY)` |

### A6. `SanPhamDetail` → `SanPham_Detail` (1-1 chia sẻ khóa)
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaSP VARCHAR(20) PK+FK | `maSP` | String | `@Id` |
| (quan hệ) | `sanPhamCore` | SanPhamCore | `@OneToOne(LAZY) @MapsId @JoinColumn(name="MaSP")` |
| MoTa NVARCHAR(MAX) *(null)* | `moTa` | String | `@Column(columnDefinition="NVARCHAR(MAX)")` |
| ThongSoKyThuat NVARCHAR(MAX) JSON *(null)* | `thongSoKyThuat` | String | giữ JSON thô |

### A7. `NguoiDung` → `NguoiDung`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaND UNIQUEIDENTIFIER PK | `maND` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID) @Column(name="MaND")` — **gán thủ công** |
| MatKhau VARCHAR(255) | `matKhau` | String | hash BCrypt |
| MaKV VARCHAR(10) FK→KhuVuc | `khuVuc` | KhuVuc | `@ManyToOne(LAZY) @JoinColumn(name="MaKV", nullable=false)` ⚠️ **MaKV** |
| MaKhoPhuTrach VARCHAR(20) *(null)* | `maKhoPhuTrach` | String | **cột thường** (Kho ngoài scope Main) — nâng cấp `@ManyToOne Kho` sau |
| HoTen NVARCHAR(100) | `hoTen` | String | |
| Email VARCHAR(100) UNIQUE | `email` | String | `@Column(nullable=false, unique=true)` |
| SoDienThoai VARCHAR(15) *(null)* UNIQUE | `soDienThoai` | String | `@Column(unique=true)` |
| DiaChi NVARCHAR(300) *(null)* | `diaChi` | String | |
| NgayDangKy DATE default | `ngayDangKy` | LocalDate | `insertable=false, updatable=false` |
| TrangThai TINYINT (0,1) | `trangThai` | Boolean | converter |
| NgaySinh DATETIME2 *(null)* | `ngaySinh` | LocalDateTime | |
| GioiTinh NVARCHAR(10) | `gioiTinh` | String | default N'Nam' |
| CCCD VARCHAR(12) *(null)* | `cccd` | String | validate len=12 ở DTO |
| VaiTro VARCHAR(20) CHECK | `vaiTro` | enum `VaiTro` | `@Enumerated(EnumType.STRING)` |

### A8. `UserGlobalIndex` → `User_Global_Index`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaND UNIQUEIDENTIFIER PK | `maND` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` — = MaND NguoiDung, **không** map quan hệ (DDL không FK) |
| Email VARCHAR(100) UNIQUE | `email` | String | `@Column(nullable=false, unique=true)` |
| SoDienThoai VARCHAR(15) *(null)* UNIQUE | `soDienThoai` | String | `@Column(unique=true)` |
| MaKhuVuc VARCHAR(10) FK→KhuVuc | `khuVuc` | KhuVuc | `@ManyToOne(LAZY) @JoinColumn(name="MaKhuVuc", nullable=false)` ⚠️ **MaKhuVuc** |
| NgayTao DATETIME2 default | `ngayTao` | LocalDateTime | `insertable=false, updatable=false` |

---

## ════════ NHÓM B — SITE KHU VỰC (10 bảng, tham chiếu / giai đoạn sau) ════════

### B1. `Kho` → `Kho`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaKho VARCHAR(20) PK | `maKho` | String | `@Id` tự gán |
| TenKho NVARCHAR(100) | `tenKho` | String | `@Column(nullable=false)` |
| DiaChi NVARCHAR(300) | `diaChi` | String | `@Column(nullable=false)` |
| MaKhuVuc VARCHAR(10) FK→KhuVuc | `khuVuc` | KhuVuc | `@ManyToOne(LAZY) @JoinColumn(name="MaKhuVuc", nullable=false)` |
| SucChua INT (CHECK ≥0) | `sucChua` | Integer | default 0 |
| TrangThai TINYINT (0,1) | `trangThai` | Boolean | converter |

### B2. `GioHang` → `GioHang`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaGioHang UNIQUEIDENTIFIER PK | `maGioHang` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` — có thể `@GeneratedValue(strategy=UUID)` (giỏ độc lập) |
| MaND UNIQUEIDENTIFIER FK→NguoiDung | `nguoiDung` | NguoiDung* | `@ManyToOne(LAZY) @JoinColumn(name="MaND")` — *nếu cùng site; nếu khác site dùng `UUID maND` cột thường |
| NgayTao DATETIME2 default | `ngayTao` | LocalDateTime | `insertable=false, updatable=false` |
| NgayCapNhat DATETIME2 default | `ngayCapNhat` | LocalDateTime | `@UpdateTimestamp` (hoặc set tay) |
| TrangThai VARCHAR(30) CHECK | `trangThai` | enum `TrangThaiGioHang` | `@Enumerated(STRING)` — {active, ordered, cancelled, expired} |

- ⚠️ **Filtered unique index** `UX_GioHang_Active(MaND) WHERE TrangThai='active'`: JPA không map được → **DB enforce**; mỗi user chỉ 1 giỏ `active`. Service phải `findByNguoiDungAndTrangThai(..., active)`.

### B3. `ChiTietGioHang` → `ChiTietGioHang`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaCTGH UNIQUEIDENTIFIER PK | `maCTGH` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| MaGioHang UNIQUEIDENTIFIER FK | `gioHang` | GioHang | `@ManyToOne(LAZY) @JoinColumn(name="MaGioHang")` |
| MaSP VARCHAR(20) FK→SanPham_Core | `sanPham` | SanPhamCore | `@ManyToOne(LAZY) @JoinColumn(name="MaSP")` (SP nhân bản → giữ FK) |
| SoLuong INT (CHECK >0) | `soLuong` | Integer | |
| NgayThem DATETIME2 default | `ngayThem` | LocalDateTime | `insertable=false, updatable=false` |
| UNIQUE(MaGioHang, MaSP) | — | — | DB enforce; có thể `@Table(uniqueConstraints=...)` |

### B4. `DonHang` → `DonHang`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaDonHang UNIQUEIDENTIFIER PK | `maDonHang` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| MaND UNIQUEIDENTIFIER FK→NguoiDung | `nguoiDung` | NguoiDung* | `@ManyToOne(LAZY) @JoinColumn(name="MaND")` (*xem ghi chú liên site như B2) |
| NgayDat DATETIME2 default | `ngayDat` | LocalDateTime | `insertable=false, updatable=false` |
| HoTenNguoiNhan NVARCHAR(100) | `hoTenNguoiNhan` | String | |
| SoDienThoaiNhan VARCHAR(15) | `soDienThoaiNhan` | String | |
| DiaChiGiao NVARCHAR(300) | `diaChiGiao` | String | |
| MaKhuVucXuLi VARCHAR(10) FK→KhuVuc | `khuVucXuLi` | KhuVuc | `@ManyToOne(LAZY) @JoinColumn(name="MaKhuVucXuLi")` ⚠️ **MaKhuVucXuLi** |
| TongTien DECIMAL(15,2) | `tongTien` | BigDecimal | default 0, CHECK ≥0 |
| PhuongThucTT VARCHAR(20) CHECK='COD' | `phuongThucTT` | String/enum | hiện chỉ 'COD' |
| TrangThaiTT VARCHAR(30) CHECK | `trangThaiTT` | enum `TrangThaiThanhToan` | {waiting_cod, paid, failed, cancelled} |
| TrangThaiDH VARCHAR(30) CHECK | `trangThaiDH` | enum `TrangThaiDonHang` | {pending, processing, shipping, completed, cancelled} |
| GhiChu NVARCHAR(500) *(null)* | `ghiChu` | String | |

### B5. `ChiTietDonHang` → `ChiTietDonHang`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaCTDH UNIQUEIDENTIFIER PK | `maCTDH` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| MaDonHang UNIQUEIDENTIFIER FK | `donHang` | DonHang | `@ManyToOne(LAZY) @JoinColumn(name="MaDonHang")` (phân mảnh dẫn xuất theo DonHang → giữ FK) |
| MaSP VARCHAR(20) FK→SanPham_Core | `sanPham` | SanPhamCore | `@ManyToOne(LAZY) @JoinColumn(name="MaSP")` |
| SoLuong INT (CHECK >0) | `soLuong` | Integer | |
| DonGia DECIMAL(15,2) (CHECK ≥0) | `donGia` | BigDecimal | `precision=15, scale=2` |
| **ThanhTien AS (SoLuong*DonGia) PERSISTED** | `thanhTien` | BigDecimal | `@Column(insertable=false, updatable=false)` + `@Generated` — **chỉ đọc** |

### B6. `PhieuXuatKho` → `PhieuXuatKho` (⚠️ nhiều khóa logic liên site)
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaPhieuXuat UNIQUEIDENTIFIER PK | `maPhieuXuat` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| **MaDonHang** UNIQUEIDENTIFIER (KHÔNG FK) | `maDonHang` | UUID | **cột thường** — khóa logic (DonHang có thể ở site khác) |
| MaKhoXuat VARCHAR(20) FK→Kho | `khoXuat` | Kho | `@ManyToOne(LAZY) @JoinColumn(name="MaKhoXuat")` (cùng site → giữ FK) |
| **MaKhoNhan** VARCHAR(20) *(null)* (KHÔNG FK) | `maKhoNhan` | String | **cột thường** — kho nhận có thể ở site khác |
| NgayTao DATETIME2 default | `ngayTao` | LocalDateTime | `insertable=false, updatable=false` |
| TrangThaiXuat VARCHAR(30) CHECK | `trangThaiXuat` | enum | {waiting_export, exported, cancelled} |
| TrangThaiNhan VARCHAR(30) *(null)* CHECK | `trangThaiNhan` | enum *(null)* | {waiting_receive, received} hoặc null |

- Các CHECK liên cột (MaKhoNhan↔TrangThaiNhan, KhoNhan≠KhoXuat) → **DB enforce**, không map ở JPA; service phải đảm bảo trước khi save.

### B7. `ChiTietXuatKho` → `ChiTietXuatKho`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaCTXK UNIQUEIDENTIFIER PK | `maCTXK` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| MaPhieuXuat UNIQUEIDENTIFIER FK | `phieuXuat` | PhieuXuatKho | `@ManyToOne(LAZY) @JoinColumn(name="MaPhieuXuat")` (giữ FK) |
| **MaCTDH** UNIQUEIDENTIFIER *(null)* (KHÔNG FK) | `maCTDH` | UUID | **cột thường** — khóa logic (ChiTietDonHang có thể ở site khác) |
| MaSP VARCHAR(20) FK→SanPham_Core | `sanPham` | SanPhamCore | `@ManyToOne(LAZY) @JoinColumn(name="MaSP")` |
| SoLuongXuat INT (CHECK >0) | `soLuongXuat` | Integer | |

### B8. `TonKho` → `TonKho` (⚠️ có ROWVERSION)
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaTonKho UNIQUEIDENTIFIER PK | `maTonKho` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| MaKho VARCHAR(20) FK→Kho | `kho` | Kho | `@ManyToOne(LAZY) @JoinColumn(name="MaKho")` (cùng site → FK) |
| MaSP VARCHAR(20) FK→SanPham_Core | `sanPham` | SanPhamCore | `@ManyToOne(LAZY) @JoinColumn(name="MaSP")` |
| SoLuongTon INT (CHECK ≥0) | `soLuongTon` | Integer | |
| SoLuongDatHang INT (CHECK ≥0) | `soLuongDatHang` | Integer | default 0 |
| NgayCapNhat DATETIME2 default | `ngayCapNhat` | LocalDateTime | `@UpdateTimestamp` hoặc insertable=false |
| **RowVer ROWVERSION** | `rowVer` | byte[] | `@Version @JdbcTypeCode(SqlTypes.VARBINARY)` (optimistic lock) — KHÔNG set tay |
| UNIQUE(MaKho, MaSP) | — | — | DB enforce; `@Table(uniqueConstraints=...)` |
| CHECK SoLuongTon ≥ SoLuongDatHang | — | — | DB enforce |

- **RowVer** là điểm hay cho báo cáo: chống mất cập nhật khi nhiều giao dịch cùng trừ tồn kho (cập nhật phân tán).

### B9. `PhieuNhapKho` → `PhieuNhapKho`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaPhieuNhap UNIQUEIDENTIFIER PK | `maPhieuNhap` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| MaKhoXuat VARCHAR(20) FK→Kho | `khoXuat` | Kho | `@ManyToOne(LAZY) @JoinColumn(name="MaKhoXuat")` |
| MaKhoNhap VARCHAR(20) FK→Kho | `khoNhap` | Kho | `@ManyToOne(LAZY) @JoinColumn(name="MaKhoNhap")` |
| NgayNhap DATETIME2 default | `ngayNhap` | LocalDateTime | `insertable=false, updatable=false` |
| MaNhanVienNhap UNIQUEIDENTIFIER FK→NguoiDung | `nhanVienNhap` | NguoiDung | `@ManyToOne(LAZY) @JoinColumn(name="MaNhanVienNhap")` |
| TrangThaiNhap VARCHAR(30) | `trangThaiNhap` | enum/String | default 'waiting_import' |
| GhiChu NVARCHAR(500) *(null)* | `ghiChu` | String | |

> Lưu ý: cả 2 FK `MaKhoXuat`, `MaKhoNhap` đều trỏ `Kho` → khai báo 2 `@ManyToOne` khác `@JoinColumn`. Trong DDL này cả hai đều có FK (giả định cùng site); nếu triển khai phân tán thực, kho xuất có thể thuộc site khác → khi đó đổi `MaKhoXuat` thành khóa logic.

### B10. `ChiTietPhieuNhap` → `ChiTietPhieuNhap`
| Cột | Field | Kiểu | Annotation |
|---|---|---|---|
| MaCTPN UNIQUEIDENTIFIER PK | `maCTPN` | UUID | `@Id @JdbcTypeCode(SqlTypes.UUID)` |
| MaPhieuNhap UNIQUEIDENTIFIER FK | `phieuNhap` | PhieuNhapKho | `@ManyToOne(LAZY) @JoinColumn(name="MaPhieuNhap")` |
| MaSP VARCHAR(20) FK→SanPham_Core | `sanPham` | SanPhamCore | `@ManyToOne(LAZY) @JoinColumn(name="MaSP")` |
| SoLuong INT (CHECK >0) | `soLuong` | Integer | |
| DonGiaNhap DECIMAL(15,2) (CHECK ≥0) | `donGiaNhap` | BigDecimal | `precision=15, scale=2` |

---

## 1. Thành phần dùng chung cần tạo

**Enums** (`domain/enums/`):
- `VaiTro { ADMIN, WAREHOUSE_STAFF, USER }`  (Nhóm A)
- `TrangThaiGioHang { active, ordered, cancelled, expired }` (B2)
- `TrangThaiThanhToan { waiting_cod, paid, failed, cancelled }` (B4)
- `TrangThaiDonHang { pending, processing, shipping, completed, cancelled }` (B4)
- `TrangThaiXuat { waiting_export, exported, cancelled }`, `TrangThaiNhan { waiting_receive, received }` (B6)
- `TrangThaiNhap { waiting_import, ... }` (B9) — bổ sung giá trị theo nghiệp vụ
- (Tùy chọn) `GioiTinh` — DDL không CHECK chặt → có thể giữ `String`

> Enum dùng `@Enumerated(EnumType.STRING)` để giá trị DB khớp tên chuỗi trong CHECK. **Tên hằng enum phải trùng y hệt chuỗi DDL** (kể cả chữ thường như `active`, `waiting_cod`).

**Converter:** dùng `csdlpt.sitemain.common.converter.BooleanToTinyIntConverter` cho mọi `TrangThai TINYINT(0,1)` để khớp kiểu `tinyint` của SQL Server khi chạy `ddl-auto=validate`.

**Quy ước Lombok:** `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` — **không** `@Data`/`@EqualsAndHashCode` trên entity.

---

## 2. Xử lý các kiểu đặc biệt (chi tiết)

### 2.1 UNIQUEIDENTIFIER
```java
@Id @JdbcTypeCode(SqlTypes.UUID)
@Column(name="...", columnDefinition="uniqueidentifier")
private UUID id;
```
- **NguoiDung & UserGlobalIndex:** KHÔNG `@GeneratedValue` → sinh `UUID.randomUUID()` ở service, gán cho cả 2.
- **Các bảng UUID độc lập (GioHang, DonHang, TonKho...):** có thể `@GeneratedValue(strategy=GenerationType.UUID)` cho tiện, vì không cần dùng chung id.

### 2.2 Cột tính toán `ThanhTien` (B5)
```java
@Column(name="ThanhTien", insertable=false, updatable=false)
@org.hibernate.annotations.Generated(event = {EventType.INSERT, EventType.UPDATE})
private BigDecimal thanhTien;   // DB tự tính, app chỉ đọc
```

### 2.3 `ROWVERSION` (B8)
```java
@Version
@JdbcTypeCode(SqlTypes.VARBINARY)
@Column(name="RowVer", insertable=false, updatable=false)
private byte[] rowVer;          // Hibernate dùng cho optimistic locking
```

### 2.4 Khóa logic liên site (H)
- `PhieuXuatKho.maDonHang`, `PhieuXuatKho.maKhoNhan`, `ChiTietXuatKho.maCTDH` → map **field giá trị** (`UUID`/`String`) với `@Column(name=...)`, **tuyệt đối không** `@ManyToOne`.
- Trong báo cáo: nhấn đây là cách CSDL phân tán "nối" dữ liệu giữa các site mà không tạo ràng buộc khóa ngoại xuyên site.

### 2.5 Thời gian default DB (I)
- Mọi `NgayTao/NgayDat/NgayNhap/NgayThem/NgayDangKy` có `DEFAULT` → `@Column(insertable=false, updatable=false)`, đọc lại sau khi save nếu cần.

---

## 3. Sơ đồ quan hệ tổng hợp (rút gọn)

```
KhuVuc 1─< Kho, NguoiDung(MaKV), UserGlobalIndex(MaKhuVuc), DonHang(MaKhuVucXuLi)
DanhMuc 1─< DanhMuc(con, MaDanhMucCha)          [self]
DanhMuc >─< ThuongHieu  (DanhMuc_ThuongHieu)    [ManyToMany]
DanhMuc 1─< SanPhamCore ;  ThuongHieu 1─< SanPhamCore
SanPhamCore 1─1 SanPhamDetail (MaSP shared, @MapsId)
SanPhamCore 1─< ChiTietGioHang, ChiTietDonHang, ChiTietXuatKho, TonKho, ChiTietPhieuNhap  [SP nhân bản → giữ FK]
NguoiDung 1─< GioHang, DonHang, PhieuNhapKho(MaNhanVienNhap)
NguoiDung ··· UserGlobalIndex (MaND bằng nhau, KHÔNG FK)
GioHang 1─< ChiTietGioHang ;  DonHang 1─< ChiTietDonHang
Kho 1─< TonKho, PhieuXuatKho(KhoXuat), PhieuNhapKho(KhoXuat/KhoNhap)
PhieuXuatKho 1─< ChiTietXuatKho ;  PhieuNhapKho 1─< ChiTietPhieuNhap
── Khóa logic (KHÔNG FK): PhieuXuatKho.MaDonHang, .MaKhoNhan ; ChiTietXuatKho.MaCTDH
```
- **Tất cả `@ManyToOne`/`@OneToOne` = `FetchType.LAZY`.**

---

## 4. Quy ước đặt tên (chốt)

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Entity | PascalCase, bỏ `_` | `SanPhamCore`, `ChiTietDonHang`, `UserGlobalIndex`, `PhieuXuatKho` |
| `@Table(name=...)` | **đúng y hệt DDL (giữ `_`)** | `@Table(name="SanPham_Core")`, `@Table(name="User_Global_Index")` |
| Field | camelCase | `maSP`, `giaBan`, `thanhTien`, `rowVer` |
| `@Column(name=...)` | **đúng y hệt DDL** | `name="MaKV"`, `name="MaKhuVucXuLi"`, `name="MaCTDH"` |
| Enum | `domain/enums/`, hằng = chuỗi DDL | `TrangThaiDonHang.pending` |
| Repository | `<Entity>Repository` | `ChiTietDonHangRepository` |

---

## 5. Cấu trúc file kết quả

```
domain/
├── enums/
│   ├── VaiTro.java
│   ├── TrangThaiGioHang.java
│   ├── TrangThaiThanhToan.java
│   ├── TrangThaiDonHang.java
│   ├── TrangThaiXuat.java   TrangThaiNhan.java   TrangThaiNhap.java
└── entity/
    ── Nhóm A (Site Main, làm trước) ──
    ├── KhuVuc.java  ThuongHieu.java  DanhMuc.java
    ├── SanPhamCore.java  SanPhamDetail.java
    ├── NguoiDung.java  UserGlobalIndex.java
    ── Nhóm B (site khu vực, giai đoạn sau) ──
    ├── Kho.java
    ├── GioHang.java  ChiTietGioHang.java
    ├── DonHang.java  ChiTietDonHang.java
    ├── PhieuXuatKho.java  ChiTietXuatKho.java
    ├── TonKho.java
    └── PhieuNhapKho.java  ChiTietPhieuNhap.java
        (DanhMuc_ThuongHieu: KHÔNG có file — dùng @ManyToMany)
```

---

## ✅ Checklist Phase 2

**Chuẩn bị**
- [x] Tạo enum cần dùng cho Nhóm A (`VaiTro`)
- [x] Chốt cách map `TrangThai TINYINT` (Boolean + `BooleanToTinyIntConverter`) dùng nhất quán

**Nhóm A — Site Main (BẮT BUỘC giai đoạn đầu)**
- [x] `KhuVuc`, `ThuongHieu`, `DanhMuc` (self + `@ManyToMany`)
- [x] `SanPhamCore` (2 `@ManyToOne` LAZY, `BigDecimal`, NgayTao insertable=false)
- [x] `SanPhamDetail` (`@OneToOne @MapsId`, NVARCHAR(MAX))
- [x] `NguoiDung` (UUID, KhuVuc qua **MaKV**, MaKhoPhuTrach=String, VaiTro enum)
- [x] `UserGlobalIndex` (UUID không FK NguoiDung, KhuVuc qua **MaKhuVuc**)

**Nhóm B — Site khu vực (tham chiếu / khi mở rộng)**
- [ ] `Kho`
- [ ] `GioHang` (+ lưu ý filtered unique active) , `ChiTietGioHang` (UNIQUE giỏ+SP)
- [ ] `DonHang` (KhuVuc qua **MaKhuVucXuLi**, 3 enum trạng thái), `ChiTietDonHang` (**ThanhTien** chỉ đọc)
- [ ] `PhieuXuatKho` (**MaDonHang/MaKhoNhan = khóa logic, KHÔNG FK**), `ChiTietXuatKho` (**MaCTDH khóa logic**)
- [ ] `TonKho` (**RowVer @Version**, UNIQUE MaKho+MaSP)
- [ ] `PhieuNhapKho` (2 FK Kho), `ChiTietPhieuNhap`

**Kiểm chứng**
- [x] `@Table`/`@Column` khớp 100% DDL (chú ý `MaKV` / `MaKhuVuc` / `MaKhuVucXuLi`)
- [x] Mọi `@ManyToOne`/`@OneToOne` = LAZY
- [ ] Khóa logic liên site map dạng giá trị, KHÔNG quan hệ
- [x] Khởi động app `ddl-auto: validate` → Hibernate khớp toàn bộ entity ↔ bảng

---

*Hết Phase 2 (đủ 18 bảng). Bước tiếp theo: Phase 3 — Repository + query + phân trang.*
