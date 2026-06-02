package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record DoanhThuToanHeThongResponse(
        Integer tongSoDonHang,
        Integer tongSoPhieuXuat,
        Integer tongSoKhoThamGiaXuat,
        Integer tongSoLuongXuat,
        BigDecimal tongDoanhThu
) {
}
