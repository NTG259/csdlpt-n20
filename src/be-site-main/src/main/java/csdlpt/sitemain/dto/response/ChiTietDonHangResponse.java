package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record ChiTietDonHangResponse(
        String maCTDH,
        String maSP,
        String tenSP,
        int soLuong,
        BigDecimal donGia,
        BigDecimal thanhTien
) {
}
