package csdlpt.sitemain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoiMatKhauRequest(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String matKhauCu,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, max = 72, message = "Mật khẩu mới phải từ 6 đến 72 ký tự")
        String matKhauMoi,

        @NotBlank(message = "Xác nhận mật khẩu không được để trống")
        String xacNhanMatKhauMoi
) {
}
