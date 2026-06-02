package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record GioHangResponse(
        List<ChiTietGioHangResponse> sanPhamHopLe,
        List<ChiTietGioHangResponse> sanPhamHetHang,
        int tongSoLuong,
        BigDecimal tongTien
) {}
