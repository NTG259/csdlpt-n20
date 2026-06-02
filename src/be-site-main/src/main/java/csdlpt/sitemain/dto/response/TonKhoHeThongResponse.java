package csdlpt.sitemain.dto.response;

import java.util.List;

public record TonKhoHeThongResponse(
        String maSP,
        String tenSP,
        int tongTonKho,
        int tongDatHang,
        int tongKhaDung,
        int soLuongKho,
        List<TonKhoChiTietKhoResponse> chiTietKho
) {
}
