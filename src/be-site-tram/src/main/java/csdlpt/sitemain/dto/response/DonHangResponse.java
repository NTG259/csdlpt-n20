package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DonHangResponse(
        UUID maDonHang,
        String trangThaiDH,
        String trangThaiTT,
        BigDecimal tongTien,
        LocalDateTime ngayDat,
        String khuVucXuLi,
        String hoTenNguoiNhan,
        String soDienThoaiNhan,
        String diaChiGiao,
        String phuongThucTT,
        String ghiChu,
        List<ChiTietDonHangResponse> items
) {
}
