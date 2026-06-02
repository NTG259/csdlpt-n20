package csdlpt.sitemain.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PhieuNhapSummaryResponse(
        UUID maPhieuNhap,
        UUID maDonHang,
        String maKhoXuat,
        String tenKhoXuat,
        String maKhoNhap,
        String tenKhoNhap,
        String trangThaiNhap,
        LocalDateTime ngayNhap,
        long soDongHang,
        long tongSoLuong,
        String sourceExportStatus
) {
}
