package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record DoanhThuTheoThangResponse(
        String siteNguon,
        int nam,
        int thang,
        String maKho,
        String tenKho,
        BigDecimal doanhThu
) {
}
