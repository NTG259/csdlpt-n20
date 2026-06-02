package csdlpt.sitemain.dto.request;

import csdlpt.sitemain.domain.enums.VaiTro;
import jakarta.validation.constraints.NotNull;

public record DoiVaiTroRequest(
        @NotNull(message = "Vai trò không được để trống")
        VaiTro vaiTro
) {
}
