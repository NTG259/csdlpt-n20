package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record DoanhThuTheoVungResponse(
        String maKhuVuc,
        Integer soDonHang,
        Integer soPhieuXuat,
        Integer soKhoThamGiaXuat,
        Integer tongSoLuongXuat,
        BigDecimal doanhThu
) {
}
