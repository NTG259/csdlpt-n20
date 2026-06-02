package csdlpt.sitemain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TaoDonHangRequest(
        @NotBlank(message = "Ho ten nguoi nhan khong duoc de trong")
        @Size(max = 100, message = "Ho ten nguoi nhan toi da 100 ky tu")
        String hoTenNguoiNhan,

        @NotBlank(message = "So dien thoai khong duoc de trong")
        @Pattern(regexp = "^0\\d{9}$", message = "So dien thoai khong hop le")
        String soDienThoaiNhan,

        @NotBlank(message = "Dia chi giao khong duoc de trong")
        @Size(max = 300, message = "Dia chi giao toi da 300 ky tu")
        String diaChiGiao,

        @NotBlank(message = "Phuong thuc thanh toan khong duoc de trong")
        @Pattern(regexp = "COD", message = "Hien chi ho tro thanh toan COD")
        String phuongThucTT,

        @Size(max = 500, message = "Ghi chu toi da 500 ky tu")
        String ghiChu
) {
}
