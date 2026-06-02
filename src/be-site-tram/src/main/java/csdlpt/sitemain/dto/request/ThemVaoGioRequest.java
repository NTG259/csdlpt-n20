package csdlpt.sitemain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ThemVaoGioRequest(
        @NotBlank(message = "Mã sản phẩm không được để trống")
        String maSP,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải ít nhất là 1")
        Integer soLuong
) {}
