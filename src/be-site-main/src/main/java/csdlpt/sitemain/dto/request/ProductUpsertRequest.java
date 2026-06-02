package csdlpt.sitemain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpsertRequest(
        @NotBlank
        @Size(max = 20)
        String maSP,

        @NotBlank
        @Size(max = 255)
        String tenSP,

        @NotBlank
        @Size(max = 20)
        String maDanhMuc,

        @NotBlank
        @Size(max = 20)
        String maThuongHieu,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal giaBan,

        @NotBlank
        @Size(max = 20)
        String donViTinh,

        @Size(max = 500)
        String hinhAnh,

        @NotNull
        Boolean trangThai,

        String moTa,

        String thongSoKyThuat
) {
}
