package csdlpt.sitemain.dto.response;

import java.util.UUID;

public record WarehouseActionResponse(
        UUID maPhieuXuat,
        UUID maPhieuNhap,
        UUID maDonHang,
        String maKhoXuat,
        String maKhoNhap,
        String trangThaiMoi,
        String message
) {
}
