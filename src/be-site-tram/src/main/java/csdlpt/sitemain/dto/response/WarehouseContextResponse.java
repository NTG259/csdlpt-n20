package csdlpt.sitemain.dto.response;

import java.util.UUID;

public record WarehouseContextResponse(
        UUID maNhanVien,
        String hoTen,
        String vaiTro,
        String maKhoPhuTrach,
        String tenKho,
        String maKV
) {
}
