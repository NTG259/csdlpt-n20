package csdlpt.sitemain.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserProfileResponse(
        String userId,
        String hoTen,
        String email,
        String soDienThoai,
        String maKhuVuc,
        String tenKhuVuc,
        String diaChi,
        LocalDate ngayDangKy,
        Boolean trangThai,
        LocalDateTime ngaySinh,
        String gioiTinh,
        String cccd,
        String vaiTro
) {
}
