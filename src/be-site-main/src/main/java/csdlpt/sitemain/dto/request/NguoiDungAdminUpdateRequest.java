package csdlpt.sitemain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record NguoiDungAdminUpdateRequest(
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String hoTen,

        @Email(message = "Email không đúng định dạng")
        @Size(max = 100, message = "Email tối đa 100 ký tự")
        String email,

        @Pattern(
                regexp = "^(0|\\+84)\\d{9,10}$",
                message = "Số điện thoại không đúng định dạng"
        )
        @Size(max = 15, message = "Số điện thoại tối đa 15 ký tự")
        String soDienThoai,

        @Size(max = 10, message = "Mã khu vực tối đa 10 ký tự")
        String maKhuVuc,

        @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
        String diaChi,

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate ngaySinh,

        @Size(max = 10, message = "Giới tính tối đa 10 ký tự")
        String gioiTinh,

        @Pattern(regexp = "^\\d{12}$", message = "CCCD phải gồm đúng 12 chữ số")
        String cccd,

        @Size(max = 20, message = "Mã kho phụ trách tối đa 20 ký tự")
        String maKhoPhuTrach
) {
}
