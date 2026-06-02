package csdlpt.sitemain.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReadyToShipOrderResponse(
        UUID maDonHang,
        LocalDateTime ngayTao,
        String trangThaiDH,
        String maKhoXuat,
        String tenKhoXuat,
        boolean daCoPhieuXuatGiaoKhach,
        UUID maPhieuXuatGiaoKhach,
        long soDongHang,
        long tongSoLuong
) {
}
