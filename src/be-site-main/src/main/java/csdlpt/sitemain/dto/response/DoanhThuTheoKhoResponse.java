package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record DoanhThuTheoKhoResponse(
        String siteXuat,
        String maKhuVuc,
        String maKhoXuat,
        String tenKho,
        Integer soDonHang,
        Integer soPhieuXuat,
        Integer tongSoLuongXuat,
        BigDecimal doanhThu
) {
}
