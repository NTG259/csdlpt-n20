package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.request.CapNhatHoSoRequest;
import csdlpt.sitemain.dto.request.DoiMatKhauRequest;
import csdlpt.sitemain.dto.request.DoiVaiTroRequest;
import csdlpt.sitemain.dto.request.NguoiDungAdminUpdateRequest;
import csdlpt.sitemain.dto.response.UserProfileResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NguoiDungService {

    Page<UserProfileResponse> layDanhSachNguoiDung(String tuKhoa, String vaiTro, Boolean trangThai, Pageable pageable);

    UserProfileResponse layChiTietNguoiDung(UUID userId);

    UserProfileResponse capNhatNguoiDung(UUID userId, NguoiDungAdminUpdateRequest request);

    void doiVaiTro(UUID userId, DoiVaiTroRequest request, UUID currentUserId);

    void xoaMem(UUID userId, UUID currentUserId);

    void khoiPhuc(UUID userId);

    UserProfileResponse layHoSoCaNhan(UUID currentUserId);

    UserProfileResponse capNhatHoSoCaNhan(UUID currentUserId, CapNhatHoSoRequest request);

    void doiMatKhau(UUID currentUserId, DoiMatKhauRequest request);
}
