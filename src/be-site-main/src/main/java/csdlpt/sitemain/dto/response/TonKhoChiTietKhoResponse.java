package csdlpt.sitemain.dto.response;

public record TonKhoChiTietKhoResponse(
        String site,
        String maKho,
        String tenKho,
        int soLuongTon,
        int soLuongDatHang,
        int soLuongKhaDung
) {
}
