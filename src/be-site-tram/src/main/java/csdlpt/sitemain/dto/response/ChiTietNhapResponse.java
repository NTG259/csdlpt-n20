package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ChiTietNhapResponse(
        UUID maCTPN,
        String maSP,
        String tenSP,
        int soLuong,
        BigDecimal donGiaNhap
) {
}
