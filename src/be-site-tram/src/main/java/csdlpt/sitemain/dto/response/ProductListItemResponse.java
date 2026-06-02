package csdlpt.sitemain.dto.response;

import java.math.BigDecimal;

public record ProductListItemResponse(
        String maSP,
        String tenSP,
        BigDecimal giaBan,
        String donViTinh,
        String hinhAnh,
        Boolean trangThai,
        String tenDanhMuc,
        String tenThuongHieu
) {
}
