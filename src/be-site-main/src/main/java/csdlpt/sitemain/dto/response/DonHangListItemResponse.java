package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DonHangListItemResponse(
        String siteNguon,
        String maDonHang,
        String maND,
        LocalDateTime ngayDat,
        String hoTenNguoiNhan,
        String soDienThoaiNhan,
        String maKhuVucXuLi,
        BigDecimal tongTien,
        String phuongThucTT,
        String trangThaiTT,
        String trangThaiDH
) {
}
