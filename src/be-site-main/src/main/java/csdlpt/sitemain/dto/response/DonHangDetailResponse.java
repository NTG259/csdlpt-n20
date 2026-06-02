package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DonHangDetailResponse(
        String siteNguon,
        String maDonHang,
        String maND,
        LocalDateTime ngayDat,
        String hoTenNguoiNhan,
        String soDienThoaiNhan,
        String diaChiGiao,
        String maKhuVucXuLi,
        BigDecimal tongTien,
        String phuongThucTT,
        String trangThaiTT,
        String trangThaiDH,
        String ghiChu,
        List<ChiTietDonHangResponse> chiTiet
) {
}
