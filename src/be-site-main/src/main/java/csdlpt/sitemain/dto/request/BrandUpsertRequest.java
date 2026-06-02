package csdlpt.sitemain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BrandUpsertRequest(
        @NotBlank
        @Size(max = 20)
        String maThuongHieu,

        @NotBlank
        @Size(max = 100)
        String tenThuongHieu,

        @NotNull
        Boolean trangThai
) {
}
