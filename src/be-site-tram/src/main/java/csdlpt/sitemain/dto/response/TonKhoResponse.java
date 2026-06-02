package csdlpt.sitemain.dto.response;

public record TonKhoResponse(
        String maSP,
        String tenSP,
        int tongSoLuongTon,
        int tongSoLuongDatHang,
        int tongSoLuongKhaDung
) {}
