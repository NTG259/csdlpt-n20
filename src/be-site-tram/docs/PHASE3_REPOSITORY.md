# PHASE 3 — TẦNG DỮ LIỆU / REPOSITORY (chi tiết)

> **Phạm vi:** Repository + query cho **Nhóm A (8 bảng Site Main)**, dựa trên mapping ở `PHASE2_DOMAIN_ENTITY.md`.
> **Mục tiêu phase:** Có repository cho mọi bảng cần dùng, đủ query cho nghiệp vụ Phase 7 (auth + đọc sản phẩm), có phân trang + lọc sản phẩm, tách rõ luồng đọc và luồng xác thực.
> **Tài liệu chỉ mô tả skeleton (chữ ký method + khung `@Query`) — KHÔNG viết logic đầy đủ.**

> **Tiến độ cập nhật 2026-06-02:**
> - Đã tạo đủ 7 repository cho Site Main
> - Đã thêm projection `ProductListItemView` và query `searchProjection(...)`
> - App khởi động thành công với repository/query hiện tại; phần service + controller vẫn ở phase sau

---

## 0. Điểm phải nhớ trước khi viết query (đồng bộ với Phase 2)

| # | Quy tắc | Hệ quả ở repository |
|---|---|---|
| 1 | **Kiểu khóa chính khác nhau** | `JpaRepository<Entity, IdType>`: PK `String` cho KhuVuc/DanhMuc/ThuongHieu/SanPham; PK **`UUID`** cho NguoiDung/UserGlobalIndex |
| 2 | **`SanPhamCore.danhMuc` / `thuongHieu` là QUAN HỆ (`@ManyToOne`), không phải field String** | Derived query đi theo đường dẫn: `findByDanhMuc_MaDanhMuc(...)`, **KHÔNG** dùng `findByMaDanhMuc(...)`. (Sửa lại so với bản nháp trong `KE_HOACH_TASK_SITE_MAIN.md`.) |
| 3 | **`@ManyToOne` đều LAZY** | Trả thẳng entity ra JSON sẽ lỗi/ lazy → **dùng DTO projection hoặc `join fetch`** cho API đọc |
| 4 | **`TrangThai` map `Boolean`** (converter TinyInt) | Tham số lọc trạng thái là `Boolean`, không phải Integer |
| 5 | **`User_Global_Index` là chỉ mục UNIQUE toàn cục** | Check trùng email/SĐT khi đăng ký dựa vào repo này (`existsByEmail`, `existsBySoDienThoai`) |
| 6 | **`SanPhamDetail` chia sẻ PK với Core (`@MapsId`)** | `SanPhamDetailRepository.findById(maSP)` dùng chính `maSP` làm khóa |

---

## 1. Danh sách repository (Task 3.1)

> Package `repository/`. Mỗi interface `@Repository` (không bắt buộc với Spring Data nhưng nên có cho rõ) `extends JpaRepository<Entity, IdType>`.

| Repository | Entity | IdType | Ghi chú |
|---|---|---|---|
| `KhuVucRepository` | KhuVuc | `String` | dropdown khu vực + validate maKhuVuc |
| `DanhMucRepository` | DanhMuc | `String` | danh mục, cây cha-con |
| `ThuongHieuRepository` | ThuongHieu | `String` | thương hiệu |
| `SanPhamCoreRepository` | SanPhamCore | `String` | trung tâm luồng đọc sản phẩm |
| `SanPhamDetailRepository` | SanPhamDetail | `String` | chi tiết (PK = maSP) |
| `NguoiDungRepository` | NguoiDung | **`UUID`** | luồng đăng nhập |
| `UserGlobalIndexRepository` | UserGlobalIndex | **`UUID`** | check trùng toàn cục khi đăng ký |

> `DanhMuc_ThuongHieu` **không có repository** — đã map bằng `@ManyToMany` trong `DanhMuc` (Phase 2). Nếu cần truy bảng nối độc lập thì mới tạo entity + repo riêng.

---

## 2. Query method theo từng repository (Task 3.2)

> Ưu tiên **derived query** (Spring tự sinh từ tên method) cho ca đơn giản; dùng `@Query` (JPQL) cho lọc động + `join fetch`.

### 2.1 `NguoiDungRepository` — luồng xác thực
```
Optional<NguoiDung> findByEmail(String email);          // đăng nhập
boolean existsByEmail(String email);                     // (phòng hờ, chính thì check ở UGI)
// load kèm khu vực để đưa vào JWT, tránh lazy khi đã đóng session:
@Query("select n from NguoiDung n join fetch n.khuVuc where n.email = :email")
Optional<NguoiDung> findByEmailFetchKhuVuc(String email);
```

### 2.2 `UserGlobalIndexRepository` — ràng buộc duy nhất toàn cục
```
boolean existsByEmail(String email);                     // check trùng email khi đăng ký
boolean existsBySoDienThoai(String soDienThoai);         // check trùng SĐT
Optional<UserGlobalIndex> findByEmail(String email);     // tra cứu user thuộc khu vực nào (định tuyến sau)
```
> **Điểm báo cáo:** 2 method `existsBy...` là nơi thể hiện kiểm tra **duy nhất toàn hệ thống** trước khi tạo user — chạy ở Site Main, áp cho mọi khu vực.

### 2.3 `SanPhamCoreRepository` — luồng đọc sản phẩm (quan trọng nhất)
**Derived query đơn giản** (đi theo đường dẫn quan hệ — xem điểm 0.2):
```
Page<SanPhamCore> findByTrangThai(Boolean trangThai, Pageable pageable);
Page<SanPhamCore> findByDanhMuc_MaDanhMuc(String maDanhMuc, Pageable pageable);
Page<SanPhamCore> findByThuongHieu_MaThuongHieu(String maThuongHieu, Pageable pageable);
```

**Lọc động (gộp nhiều filter tùy chọn) — KHUYẾN NGHỊ dùng 1 `@Query` với tham số nullable** (đơn giản, không cần Specification):
```
@Query("""
   select p from SanPhamCore p
   where (:maDanhMuc   is null or p.danhMuc.maDanhMuc   = :maDanhMuc)
     and (:maThuongHieu is null or p.thuongHieu.maThuongHieu = :maThuongHieu)
     and (:trangThai   is null or p.trangThai = :trangThai)
""")
Page<SanPhamCore> search(@Param("maDanhMuc") String maDanhMuc,
                         @Param("maThuongHieu") String maThuongHieu,
                         @Param("trangThai") Boolean trangThai,
                         Pageable pageable);
```

**Lấy chi tiết kèm quan hệ (tránh N+1 / LazyInitializationException):**
```
@Query("""
   select p from SanPhamCore p
   join fetch p.danhMuc
   join fetch p.thuongHieu
   where p.maSP = :maSP
""")
Optional<SanPhamCore> findDetailById(@Param("maSP") String maSP);
```
> `SanPham_Detail` lấy riêng qua `SanPhamDetailRepository.findById(maSP)` rồi ghép ở service (đơn giản, rõ ràng). Nếu đã map `@OneToOne chiTiet` trong Core thì có thể thêm `join fetch p.chiTiet`.

### 2.4 `SanPhamDetailRepository`
```
// PK = maSP (do @MapsId) → dùng luôn findById kế thừa:
Optional<SanPhamDetail> findById(String maSP);
// hoặc rõ nghĩa hơn:
Optional<SanPhamDetail> findBySanPhamCore_MaSP(String maSP);
```

### 2.5 `DanhMucRepository`
```
List<DanhMuc> findByTrangThai(Boolean trangThai);            // chỉ danh mục đang bật
List<DanhMuc> findByDanhMucChaIsNull();                       // danh mục gốc (cấp 1)
List<DanhMuc> findByDanhMucCha_MaDanhMuc(String maCha);       // danh mục con của 1 cha
```
> Muốn trả **cây** danh mục: lấy `findAll()` rồi dựng cây ở service (gom theo `maDanhMucCha`). Giai đoạn đầu trả phẳng cũng đủ demo.

### 2.6 `ThuongHieuRepository`
```
List<ThuongHieu> findByTrangThai(Boolean trangThai);
boolean existsByTenThuongHieu(String tenThuongHieu);         // (tùy chọn, nếu sau này thêm admin)
```

### 2.7 `KhuVucRepository`
```
// findAll() kế thừa → dropdown chọn khu vực khi đăng ký
boolean existsById(String maKhuVuc);                         // validate maKhuVuc khi đăng ký (kế thừa sẵn)
```

---

## 3. Hai luồng dữ liệu — phân biệt rõ (Task 3.3)

| Tiêu chí | Luồng ĐỌC sản phẩm | Luồng XÁC THỰC |
|---|---|---|
| Repo chính | SanPhamCore/Detail, DanhMuc, ThuongHieu | NguoiDung, UserGlobalIndex, KhuVuc |
| Tính chất | public, read-only, có phân trang | ghi (đăng ký), đọc nhạy cảm (mật khẩu hash) |
| Cách trả dữ liệu | **DTO projection / `join fetch`** (không lộ entity) | dùng entity nội bộ; ra ngoài chỉ trả DTO an toàn (không bao giờ trả `matKhau`) |
| Transaction | `@Transactional(readOnly = true)` ở service | đăng ký `@Transactional` (ghi 2 bảng cùng lúc) |

> **Bảo mật:** field `matKhau` của `NguoiDung` tuyệt đối không nằm trong DTO response. Tách rõ ở tầng service/DTO, không trả entity trực tiếp.

---

## 4. DTO projection & phân trang (Task 3.4)

### 4.1 Có dùng DTO projection không? → **CÓ**, cho danh sách sản phẩm
Hai cách (chọn 1, khuyến nghị **interface projection** cho nhanh):

**Cách A — Interface projection** (đặt trong `dto/projection/`):
```
public interface ProductListItemView {
    String getMaSP();
    String getTenSP();
    BigDecimal getGiaBan();
    String getHinhAnh();
    Boolean getTrangThai();
    String getTenDanhMuc();      // ánh xạ từ p.danhMuc.tenDanhMuc
    String getTenThuongHieu();   // ánh xạ từ p.thuongHieu.tenThuongHieu
}
```
+ query:
```
@Query("""
   select p.maSP as maSP, p.tenSP as tenSP, p.giaBan as giaBan,
          p.hinhAnh as hinhAnh, p.trangThai as trangThai,
          d.tenDanhMuc as tenDanhMuc, t.tenThuongHieu as tenThuongHieu
   from SanPhamCore p
   join p.danhMuc d
   join p.thuongHieu t
   where (:maDanhMuc is null or d.maDanhMuc = :maDanhMuc)
     and (:maThuongHieu is null or t.maThuongHieu = :maThuongHieu)
     and (:trangThai is null or p.trangThai = :trangThai)
""")
Page<ProductListItemView> searchProjection(@Param("maDanhMuc") String maDanhMuc,
                                           @Param("maThuongHieu") String maThuongHieu,
                                           @Param("trangThai") Boolean trangThai,
                                           Pageable pageable);
```
> Bí danh (`as maSP`...) **bắt buộc** trùng tên getter để Spring map đúng.

**Cách B — Constructor expression** (nếu thích DTO class rõ ràng): `select new csdlpt.sitemain.dto.response.ProductListItemResponse(p.maSP, p.tenSP, ...)` — cần constructor khớp thứ tự/kiểu.

> **Chi tiết sản phẩm:** không cần projection, dùng `findDetailById` (mục 2.3) + `SanPhamDetail`, map sang `ProductDetailResponse` ở service.

### 4.2 Phân trang → **CÓ** cho `GET /api/products`
- Controller nhận `Pageable` (`?page=0&size=10&sort=ngayTao,desc`) — Spring tự bind.
- Repo trả `Page<...>`; service chuyển sang `PageResponse<...>` chuẩn (Phase 4).
- Đặt `size` mặc định hợp lý (vd 10–20) để demo gọn.
- Các API danh mục/thương hiệu/khu vực: dữ liệu ít → trả `List<...>`, **không cần phân trang**.

---

## 5. Lưu ý kỹ thuật khi viết query

- **UUID:** method của `NguoiDungRepository`/`UserGlobalIndexRepository` nhận `UUID` (không trộn `String`); `findByEmail` nhận `String` là đúng vì email là VARCHAR.
- **`readOnly`:** các service đọc đặt `@Transactional(readOnly = true)` để Hibernate tối ưu (không dirty-checking).
- **Tránh `findAll()` không phân trang trên bảng lớn:** chỉ dùng cho KhuVuc/ThuongHieu/DanhMuc (dữ liệu nhỏ). Sản phẩm luôn qua `Page`.
- **`join` vs `join fetch`:** projection dùng `join` thường (chỉ lấy cột cần); lấy entity đầy đủ thì `join fetch` để nạp luôn quan hệ LAZY.
- **NVARCHAR/tiếng Việt:** không cần xử lý đặc biệt ở repo; chỉ cần JVM UTF-8. Nếu sau này cần tìm kiếm theo tên: cân nhắc `like` + collation `_CI_AI` để bỏ dấu (chưa cần giai đoạn đầu).
- **Đếm bản ghi cho `Page`:** với `@Query` có `join`, Spring tự sinh count query; nếu phức tạp có thể khai báo `countQuery=...`. Giai đoạn đầu query đơn giản nên không cần.

---

## 6. Cấu trúc file kết quả

```
repository/
├── KhuVucRepository.java
├── DanhMucRepository.java
├── ThuongHieuRepository.java
├── SanPhamCoreRepository.java
├── SanPhamDetailRepository.java
├── NguoiDungRepository.java
└── UserGlobalIndexRepository.java

dto/projection/
└── ProductListItemView.java        (nếu chọn interface projection)
```

---

## ✅ Checklist Phase 3

**Repository**
- [x] 7 repository, đúng `IdType` (chú ý NguoiDung/UserGlobalIndex = `UUID`)

**Query xác thực**
- [x] `NguoiDungRepository.findByEmail` (+ bản `join fetch khuVuc` nếu cần)
- [x] `UserGlobalIndexRepository.existsByEmail` / `existsBySoDienThoai`
- [x] `KhuVucRepository.existsById` để validate maKhuVuc

**Query sản phẩm**
- [x] `search(...)` lọc động maDanhMuc/maThuongHieu/trangThai + `Pageable`
- [x] `findDetailById(maSP)` có `join fetch` danh mục + thương hiệu
- [x] `SanPhamDetailRepository.findById(maSP)`
- [x] Đường dẫn quan hệ đúng (`DanhMuc_MaDanhMuc`), KHÔNG dùng field String không tồn tại

**Danh mục / thương hiệu / khu vực**
- [x] `DanhMucRepository`: theo trạng thái + cây cha-con
- [x] `ThuongHieuRepository.findByTrangThai`
- [x] `KhuVucRepository` dùng `findAll` cho dropdown

**Projection & phân trang**
- [x] Chốt cách projection cho danh sách sản phẩm (interface view hoặc constructor)
- [ ] `GET /api/products` trả `Page` → `PageResponse`
- [ ] Service đọc đặt `@Transactional(readOnly = true)`

**Kiểm chứng**
- [ ] Viết 1 test/lệnh gọi thử: `search` ra đúng số trang; `findDetailById` không lỗi lazy
- [ ] `existsByEmail` trả true với email đã seed, false với email mới

---

*Hết Phase 3. Bước tiếp theo: Phase 4 — DTO & response chuẩn (xem `KE_HOACH_TASK_SITE_MAIN.md`).*
