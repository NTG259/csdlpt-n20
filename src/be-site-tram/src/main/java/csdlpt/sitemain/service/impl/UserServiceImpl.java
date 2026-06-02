package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.dto.response.UserProfileResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.NguoiDungRepository;
import csdlpt.sitemain.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final NguoiDungRepository nguoiDungRepository;

    public UserServiceImpl(NguoiDungRepository nguoiDungRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Override
    public UserProfileResponse getProfile(UUID maND) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(maND)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã: " + maND));

        return new UserProfileResponse(
                nguoiDung.getMaND().toString(),
                nguoiDung.getHoTen(),
                nguoiDung.getEmail(),
                nguoiDung.getSoDienThoai(),
                nguoiDung.getKhuVuc().getMaKhuVuc(),
                nguoiDung.getKhuVuc().getTenKhuVuc(),
                nguoiDung.getDiaChi(),
                nguoiDung.getNgayDangKy(),
                nguoiDung.getTrangThai(),
                nguoiDung.getNgaySinh(),
                nguoiDung.getGioiTinh(),
                nguoiDung.getCccd(),
                nguoiDung.getVaiTro().name()
        );
    }
}
