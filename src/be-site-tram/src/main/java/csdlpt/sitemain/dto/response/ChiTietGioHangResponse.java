package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record ChiTietGioHangResponse(
        String maSP,
        String tenSP,
        String hinhAnh,
        String donViTinh,
        Integer soLuong,
        BigDecimal giaBan,
        BigDecimal thanhTien,
        int soLuongKhaDung
) {}
