# PHASE 9 — DỮ LIỆU MẪU & KIỂM THỬ (chi tiết)

> **Phạm vi:** Chuẩn bị data demo trong SQL Server + kịch bản test happy-path và lỗi.
> **Mục tiêu phase:** Demo trơn tru trước người chấm, chứng minh ràng buộc toàn cục + đọc sản phẩm hoạt động.

---

## Task 9.1 — Dữ liệu mẫu

### Thứ tự insert (theo phụ thuộc khóa ngoại)
```
1. KhuVuc            -- KV00 Trung tâm, KV01 Bắc, KV02 Nam
2. DanhMuc           -- vài danh mục; 1–2 cặp cha-con (MaDanhMucCha)
3. ThuongHieu        -- vài thương hiệu
4. DanhMuc_ThuongHieu-- gán thương hiệu vào danh mục
5. SanPham_Core      -- 5–10 sản phẩm (gắn MaDanhMuc + MaThuongHieu)
6. SanPham_Detail    -- chi tiết từng sản phẩm (MoTa, ThongSoKyThuat JSON hợp lệ)
-- NguoiDung / User_Global_Index: TẠO QUA API đăng ký (để mật khẩu được hash đúng)
```

### Lưu ý khi viết SQL
- Chuỗi tiếng Việt: tiền tố `N'...'` (NVARCHAR).
- `TrangThai` = `1` (đang bật) cho dữ liệu demo.
- `ThongSoKyThuat` phải là JSON hợp lệ (CHECK `ISJSON`), vd `N'{"mau":"Đỏ","size":"L"}'`.
- `GiaBan` ≥ 0; `MaDanhMucCha` để `NULL` cho danh mục gốc.
- **File:** `docs/sample-data.sql` (chạy tay) — KHÔNG để `data.sql` tự chạy với `ddl-auto: validate` trừ khi chủ đích seed.

---

## Task 9.2 — Kịch bản test

### Happy path
| # | Hành động | Kỳ vọng |
|---|---|---|
| 1 | POST register (user mới hợp lệ) | 201, có bản ghi `NguoiDung` **và** `User_Global_Index` cùng `MaND`, trả token |
| 2 | POST login đúng | 200, nhận token; decode token thấy `maKhuVuc`, `vaiTro` |
| 3 | GET /api/products | 200, có `items` + thông tin phân trang |
| 4 | GET /api/products?maDanhMuc=... | 200, lọc đúng |
| 5 | GET /api/products/{maSP} tồn tại | 200, đủ core + detail (moTa, thongSoKyThuat) |
| 6 | GET /api/categories, /brands, /regions | 200, danh sách |
| 7 | GET /api/auth/check-email (email mới) | `available=true` |

### Lỗi
| # | Hành động | Kỳ vọng |
|---|---|---|
| 1 | register trùng email | 409 `DUPLICATE_EMAIL` |
| 2 | register trùng SĐT | 409 `DUPLICATE_PHONE` |
| 3 | register maKhuVuc không tồn tại | 400 `INVALID_REGION` |
| 4 | register email sai định dạng / mật khẩu < 6 | 400 `VALIDATION_ERROR` + `details[]` |
| 5 | login sai mật khẩu | 401 `INVALID_CREDENTIALS` |
| 6 | login email không tồn tại | 401 `INVALID_CREDENTIALS` (không lộ) |
| 7 | GET /api/products/{maSP} không tồn tại | 404 `RESOURCE_NOT_FOUND` |

### Kiểm tra DB sau đăng ký (chứng minh ràng buộc toàn cục)
```sql
SELECT n.MaND, n.Email, g.MaND, g.Email, g.MaKhuVuc
FROM NguoiDung n JOIN User_Global_Index g ON n.MaND = g.MaND
WHERE n.Email = 'a@example.com';   -- MaND hai bảng phải TRÙNG nhau
```

### Công cụ
- Postman collection hoặc `docs/requests.http` (VS Code REST Client).
- Nhóm request theo: Auth / Products / Lookup; lưu token vào biến môi trường để tái dùng.

---

## ✅ Checklist Phase 9
- [ ] `sample-data.sql` đủ KhuVuc→DanhMuc→ThuongHieu→DanhMuc_ThuongHieu→SanPham_Core→SanPham_Detail
- [ ] Tạo user qua API (không insert tay để hash đúng)
- [ ] Test happy-path (7 ca) pass
- [ ] Test lỗi (7 ca) trả đúng status + errorCode
- [ ] Verify `MaND` trùng nhau giữa `NguoiDung` và `User_Global_Index`
- [ ] Postman/`.http` lưu trong `docs/` để demo

---
*Hết Phase 9. Tiếp theo: Phase 10 — chuẩn bị giai đoạn sau.*
