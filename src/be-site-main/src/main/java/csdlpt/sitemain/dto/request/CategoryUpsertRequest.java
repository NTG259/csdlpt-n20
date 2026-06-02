package csdlpt.sitemain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryUpsertRequest(
        @NotBlank
        @Size(max = 20)
        String maDanhMuc,

        @NotBlank
        @Size(max = 100)
        String tenDanhMuc,

        @Size(max = 20)
        String maDanhMucCha,

        @Size(max = 500)
        String moTa,

        @NotNull
        Boolean trangThai
) {
}
