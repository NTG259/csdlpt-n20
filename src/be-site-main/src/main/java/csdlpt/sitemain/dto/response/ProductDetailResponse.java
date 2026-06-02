package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductDetailResponse(
        String maSP,
        String tenSP,
        BigDecimal giaBan,
        String donViTinh,
        String hinhAnh,
        Boolean trangThai,
        LocalDateTime ngayTao,
        String maDanhMuc,
        String tenDanhMuc,
        String maThuongHieu,
        String tenThuongHieu,
        String moTa,
        String thongSoKyThuat
) {
}
