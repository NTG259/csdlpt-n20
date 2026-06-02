package csdlpt.sitemain.dto.response;

import java.util.UUID;

public record ChiTietXuatResponse(
        UUID maCTXK,
        UUID maCTDH,
        String maSP,
        String tenSP,
        int soLuongXuat,
        Integer soLuongTon,
        Integer soLuongDatHang
) {
}
