package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record SanPhamBanChayResponse(
        String maSP,
        String tenSP,
        int tongSoLuongBan,
        BigDecimal tongDoanhThu
) {
}
