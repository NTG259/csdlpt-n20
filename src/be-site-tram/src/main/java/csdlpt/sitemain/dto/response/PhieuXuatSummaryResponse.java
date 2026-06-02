package csdlpt.sitemain.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhieuXuatSummaryResponse(
        UUID maPhieuXuat,
        UUID maDonHang,
        String maKhoXuat,
        String tenKhoXuat,
        String maKhoNhan,
        String tenKhoNhan,
        String loaiPhieu,
        String trangThaiXuat,
        String trangThaiNhan,
        LocalDateTime ngayTao,
        long soDongHang,
        long tongSoLuong
) {
}
