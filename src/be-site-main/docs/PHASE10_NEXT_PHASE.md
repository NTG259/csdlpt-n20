# PHASE 10 — CHUẨN BỊ GIAI ĐOẠN SAU (liên site) — chi tiết

> **Phạm vi:** Định hướng kiến trúc khi mở rộng sang Site Bắc/Nam. **KHÔNG code ở giai đoạn này.**
> **Mục tiêu phase:** Chốt vai trò Site Main + những thứ cần chuẩn bị trước để việc nối site sau này dễ.

---

## 0. Vai trò mỗi site (chốt cho báo cáo)

| Vai trò | Site Main | Site Bắc / Nam |
|---|---|---|
| Xác thực (JWT) | ✅ sở hữu | dùng token do Main cấp |
| Sản phẩm/Danh mục/Thương hiệu/Khu vực (dùng chung) | ✅ nguồn gốc + **nhân bản toàn phần** xuống site | đọc bản sao |
| `User_Global_Index` (chỉ mục toàn cục) | ✅ sở hữu | tra cứu qua Main |
| Giỏ hàng / Đơn hàng / Tồn kho / Xuất-Nhập kho | ❌ | ✅ phân mảnh theo khu vực |
| Định tuyến request theo khu vực | ✅ | nhận request đã định tuyến |

---

## 1. API sẽ chuyển sang site khu vực
- **GioHang / ChiTietGioHang** → site theo `maKhuVuc` của user.
- **DonHang / ChiTietDonHang** → site xử lý theo `MaKhuVucXuLi`.
- **TonKho / PhieuXuatKho / PhieuNhapKho / Kho** → site sở hữu kho.

> Đây chính là **Nhóm B** trong `PHASE2_DOMAIN_ENTITY.md` — đã có sẵn mapping tham chiếu.

---

## 2. Đã chuẩn bị sẵn từ giai đoạn đầu (không cần làm lại)
- [x] **JWT mang `maKhuVuc` + `vaiTro`** → site khu vực biết user thuộc đâu mà không hỏi lại Main mỗi lần.
- [x] **`ApiResponse`/`ErrorResponse` chuẩn** → mọi site trả cùng format.
- [x] **Tách "dữ liệu dùng chung" vs "phân mảnh"** rõ trong tài liệu (Nhóm A vs B).
- [x] **Khóa logic liên site** (PhieuXuatKho.MaDonHang/MaKhoNhan, ChiTietXuatKho.MaCTDH) đã thiết kế không FK.

---

## 3. Việc cần làm khi mở rộng (TODO giai đoạn sau)
- [ ] **Cơ chế định tuyến:** Main đọc `maKhuVuc` (từ token / `User_Global_Index`) → chuyển tiếp request giỏ hàng/đơn hàng tới URL site tương ứng (cấu hình `site.bac.url`, `site.nam.url`). Dùng `RestClient`/`WebClient` — KHÔNG cần Feign/Gateway phức tạp.
- [ ] **Nhân bản dữ liệu dùng chung:** đồng bộ SanPham/DanhMuc/ThuongHieu/KhuVuc/Kho xuống site khu vực (replication SQL Server hoặc job đồng bộ định kỳ — chốt theo yêu cầu môn học).
- [ ] **Backend site khu vực:** project riêng, map Nhóm B + đọc bản sao Nhóm A; xử lý nghiệp vụ giỏ hàng/đơn hàng/kho.
- [ ] **Giao dịch phân tán (nếu đề tài yêu cầu):** chỉ khi cần đảm bảo nhất quán xuyên site (vd trừ tồn kho + tạo đơn) → cân nhắc 2PC/saga. **Tránh đưa vào sớm.**

---

## 4. Nguyên tắc giữ vững
- Site Main **không** ôm nghiệp vụ khu vực; chỉ xác thực + dữ liệu chung + định tuyến.
- Không over-engineering: thêm 2PC/replication **chỉ khi** đề tài/giáo viên yêu cầu chứng minh.
- Mọi mở rộng phải giữ được format response & cơ chế JWT đã thống nhất.

---

## ✅ Checklist Phase 10 (định hướng)
- [ ] Sơ đồ kiến trúc 3 site cho báo cáo (Main = auth + dữ liệu chung + định tuyến; Bắc/Nam = giao dịch)
- [ ] Liệt kê rõ API tương lai chuyển sang site khu vực
- [ ] Ghi chú cơ chế định tuyến dựa trên `maKhuVuc`
- [ ] Xác nhận JWT + ApiResponse đã sẵn sàng cho đa site
- [ ] Quyết định có làm replication / giao dịch phân tán hay không (theo yêu cầu môn)

---
*Hết Phase 10 — kết thúc bộ tài liệu kế hoạch Site Main.*
