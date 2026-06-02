package csdlpt.sitemain.dto.response;

import java.time.LocalDateTime;

public record WarehouseTonKhoResponse(
        String maKho,
        String maSP,
        String tenSP,
        int soLuongTon,
        int soLuongDatHang,
        int soLuongKhaDung,
        LocalDateTime ngayCapNhat
) {
}
