# PHASE 7 — SERVICE NGHIỆP VỤ (chi tiết)

> **Phạm vi:** Hiện thực các luồng: đăng ký, đăng nhập, xem sản phẩm, danh mục, thương hiệu, khu vực.
> **Quy ước:** Mỗi service tách **interface + impl** (`service/` + `service/impl/`). Service đọc đặt `@Transactional(readOnly=true)`; ghi đặt `@Transactional`.

> **Tiến độ cập nhật 2026-06-02:**
> - Đã tạo interface + impl cho `AuthService`, `UserService`, `ProductService`, `CategoryService`, `BrandService`, `RegionService`
> - `AuthService.register` đang dùng auto-login và trả `AuthResponse`; UUID được dùng chung cho `NguoiDung` và `User_Global_Index`
> - `AuthService.login` đang gộp lỗi email sai/mật khẩu sai/trạng thái khóa về `INVALID_CREDENTIALS`
> - `ProductService` đã map `ProductListItemView -> ProductListItemResponse` và bổ sung `donViTinh` vào projection/query
> - `UserService` đang có `getProfile(UUID)` trả `UserProfileResponse` không lộ `matKhau`
> - Đã có unit test cho register/login và product list/detail/not-found

---

## Task 7.1 — AuthService
**Phụ thuộc:** `NguoiDungRepository`, `UserGlobalIndexRepository`, `KhuVucRepository`, `PasswordEncoder`, `JwtService`.

### Đăng ký — `register(RegisterRequest)` `@Transactional`
```
1. existsByEmail(email)         → có ⇒ DuplicateEmailException
2. existsBySoDienThoai(sdt)     → có ⇒ DuplicatePhoneException
3. KhuVucRepository.existsById(maKhuVuc) → không ⇒ InvalidRegionException
4. id = UUID.randomUUID()       // dùng CHUNG cho 2 bảng (xem Phase 2 điểm B)
5. NguoiDung:  setMaND(id); khuVuc=ref(maKhuVuc); matKhau=encode(...); vaiTro=USER; trangThai=true
6. UserGlobalIndex: setMaND(id); email; sdt; khuVuc=ref(maKhuVuc)
7. save(nguoiDung); save(userGlobalIndex)   // cùng transaction
8. trả AuthResponse (auto-login: sinh token) — hoặc RegisterResponse
```
> **Điểm báo cáo:** bước 1–2 (check trên `User_Global_Index`) = **ràng buộc duy nhất toàn cục**; chạy ở Site Main, áp cho mọi khu vực. DB UNIQUE là chốt chặn cuối (bắt `DataIntegrityViolationException` ở Phase 5).

### Đăng nhập — `login(LoginRequest)`
```
1. findByEmail(email) (kèm fetch khuVuc) → rỗng ⇒ InvalidCredentialsException
2. passwordEncoder.matches(raw, hash)     → sai ⇒ InvalidCredentialsException
3. (tùy) kiểm tra trangThai = true        → khóa ⇒ InvalidCredentialsException
4. token = jwtService.generateToken(user)
5. trả AuthResponse(token, userId, hoTen, email, maKhuVuc, vaiTro)
```
> Lỗi email-không-tồn-tại và sai-mật-khẩu trả **cùng** `INVALID_CREDENTIALS` (không tiết lộ email có tồn tại hay không).

### Check trùng — `isEmailAvailable(email)`, `isPhoneAvailable(phone)`
```
return !userGlobalIndexRepository.existsByEmail(email);   // true = đăng ký được
```

---

## Task 7.2 — UserService
- **Mục tiêu:** đọc profile, tách khỏi AuthService. Giai đoạn đầu tối giản: `getProfile(UUID maND)` → DTO (không gồm `matKhau`).

---

## Task 7.3 — ProductService `@Transactional(readOnly=true)`
**Phụ thuộc:** `SanPhamCoreRepository`, `SanPhamDetailRepository`.

### Danh sách — `getProducts(filter, Pageable)`
```
1. page = sanPhamCoreRepository.searchProjection(maDanhMuc, maThuongHieu, trangThai, pageable)
2. trả PageResponse.from(page)   // T = ProductListItemView/ProductListItemResponse
```
- Filter null ⇒ bỏ điều kiện (query nullable ở Phase 3).

### Chi tiết — `getProductDetail(maSP)`
```
1. core = findDetailById(maSP) (join fetch danhMuc + thuongHieu) → rỗng ⇒ ResourceNotFoundException
2. detail = sanPhamDetailRepository.findById(maSP).orElse(null)
3. map core + detail → ProductDetailResponse (thongSoKyThuat: String JSON)
```

---

## Task 7.4 — CategoryService / BrandService / RegionService `readOnly`
| Service | Method | Nguồn | Trả về |
|---|---|---|---|
| CategoryService | `getAll()` | `DanhMucRepository.findByTrangThai(true)` (hoặc findAll) | `List<CategoryResponse>` (phẳng) |
| BrandService | `getAll()` | `ThuongHieuRepository.findByTrangThai(true)` | `List<BrandResponse>` |
| RegionService | `getAll()` | `KhuVucRepository.findAll()` | `List<RegionResponse>` (dropdown đăng ký) |

> Cây danh mục: nếu cần, `CategoryService` lấy `findAll()` rồi gom theo `maDanhMucCha` → set `children`. Giai đoạn đầu trả phẳng.

---

## Mapping (entity → DTO)
- Map **thủ công** trong service (không MapStruct). Có thể gom vào method `toResponse(entity)` riêng từng service, hoặc lớp `*Mapper` tĩnh trong `dto/`.
- **Tuyệt đối không** đưa `matKhau` vào bất kỳ response nào.

---

## ✅ Checklist Phase 7
- [x] `AuthService.register` đúng 8 bước, `@Transactional`, UUID dùng chung 2 bảng
- [x] `AuthService.login` trả token + thông tin user, lỗi gộp `INVALID_CREDENTIALS`
- [x] `isEmailAvailable` / `isPhoneAvailable`
- [x] `ProductService` list (projection + PageResponse) + detail (join fetch + 404)
- [x] `CategoryService` / `BrandService` / `RegionService`
- [x] Service đọc `readOnly=true`; mapping thủ công, không lộ `matKhau`

---
*Hết Phase 7. Tiếp theo: Phase 8 — API endpoint.*
