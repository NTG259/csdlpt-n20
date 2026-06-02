package csdlpt.sitemain.dto.request;

import csdlpt.sitemain.domain.enums.TrangThaiDonHang;
import csdlpt.sitemain.domain.enums.TrangThaiThanhToan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CapNhatTrangThaiDonHangRequest(
        @NotBlank(message = "siteNguon không được để trống")
        @Pattern(
                regexp = "^(SITE_BAC|SITE_NAM)$",
                message = "siteNguon phải là SITE_BAC hoặc SITE_NAM"
        )
        String siteNguon,

        TrangThaiDonHang trangThaiDH,

        TrangThaiThanhToan trangThaiTT
) {
}
