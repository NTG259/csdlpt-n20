package csdlpt.sitemain.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PhieuNhapDetailResponse(
        UUID maPhieuNhap,
        UUID maDonHang,
        String maKhoXuat,
        String tenKhoXuat,
        String maKhoNhap,
        String tenKhoNhap,
        String trangThaiNhap,
        LocalDateTime ngayNhap,
        UUID maNhanVienNhap,
        String sourceExportStatus,
        List<ChiTietNhapResponse> items
) {
}
