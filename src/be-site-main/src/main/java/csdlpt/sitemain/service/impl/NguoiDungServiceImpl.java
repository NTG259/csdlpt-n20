package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.domain.entity.KhuVuc;
import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.domain.enums.VaiTro;
import csdlpt.sitemain.dto.request.CapNhatHoSoRequest;
import csdlpt.sitemain.dto.request.DoiMatKhauRequest;
import csdlpt.sitemain.dto.request.DoiVaiTroRequest;
import csdlpt.sitemain.dto.request.NguoiDungAdminUpdateRequest;
import csdlpt.sitemain.dto.response.UserProfileResponse;
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.exception.InvalidRegionException;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.KhuVucRepository;
import csdlpt.sitemain.repository.NguoiDungRepository;
import csdlpt.sitemain.service.NguoiDungService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NguoiDungServiceImpl implements NguoiDungService {

    private final NguoiDungRepository nguoiDungRepository;
    private final KhuVucRepository khuVucRepository;
    private final PasswordEncoder passwordEncoder;

    public NguoiDungServiceImpl(
            NguoiDungRepository nguoiDungRepository,
            KhuVucRepository khuVucRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.khuVucRepository = khuVucRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> layDanhSachNguoiDung(
            String tuKhoa, String vaiTro, Boolean trangThai, Pageable pageable
    ) {
        VaiTro vaiTroEnum = parseVaiTro(vaiTro);
        String keyword = (tuKhoa == null || tuKhoa.isBlank()) ? null : tuKhoa.trim();
        return nguoiDungRepository.timKiem(keyword, vaiTroEnum, trangThai, pageable)
                .map(this::toUserProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse layChiTietNguoiDung(UUID userId) {
        return toUserProfileResponse(findByIdOrThrow(userId));
    }

    @Override
    public UserProfileResponse capNhatNguoiDung(UUID userId, NguoiDungAdminUpdateRequest request) {
        NguoiDung nd = findByIdOrThrow(userId);

        if (request.hoTen() != null) {
            nd.setHoTen(request.hoTen().trim());
        }
        if (request.email() != null) {
            String newEmail = request.email().trim().toLowerCase();
            if (!newEmail.equals(nd.getEmail()) && nguoiDungRepository.existsByEmail(newEmail)) {
                throw new BusinessException(ErrorCodes.DUPLICATE_EMAIL, HttpStatus.CONFLICT,
                        "Email đã tồn tại trong hệ thống");
            }
            nd.setEmail(newEmail);
        }
        if (request.soDienThoai() != null) {
            String newPhone = request.soDienThoai().trim();
            if (!newPhone.equals(nd.getSoDienThoai()) && nguoiDungRepository.existsBySoDienThoai(newPhone)) {
                throw new BusinessException(ErrorCodes.DUPLICATE_PHONE, HttpStatus.CONFLICT,
                        "Số điện thoại đã tồn tại trong hệ thống");
            }
            nd.setSoDienThoai(newPhone);
        }
        if (request.maKhuVuc() != null) {
            KhuVuc khuVuc = khuVucRepository.findById(request.maKhuVuc())
                    .orElseThrow(InvalidRegionException::new);
            nd.setKhuVuc(khuVuc);
        }
        if (request.diaChi() != null) nd.setDiaChi(request.diaChi().trim());
        if (request.ngaySinh() != null) nd.setNgaySinh(request.ngaySinh().atStartOfDay());
        if (request.gioiTinh() != null) nd.setGioiTinh(request.gioiTinh().trim());
        if (request.cccd() != null) nd.setCccd(request.cccd().trim());
        if (request.maKhoPhuTrach() != null) nd.setMaKhoPhuTrach(request.maKhoPhuTrach().trim());

        return toUserProfileResponse(nguoiDungRepository.save(nd));
    }

    @Override
    public void doiVaiTro(UUID userId, DoiVaiTroRequest request, UUID currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCodes.CANNOT_DELETE_SELF, HttpStatus.BAD_REQUEST,
                    "Không thể thay đổi vai trò của chính mình");
        }
        NguoiDung nd = findByIdOrThrow(userId);
        nd.setVaiTro(request.vaiTro());
        nguoiDungRepository.save(nd);
    }

    @Override
    public void xoaMem(UUID userId, UUID currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCodes.CANNOT_DELETE_SELF, HttpStatus.BAD_REQUEST,
                    "Không thể xoá tài khoản của chính mình");
        }
        NguoiDung nd = findByIdOrThrow(userId);
        if (Boolean.FALSE.equals(nd.getTrangThai())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.CONFLICT,
                    "Tài khoản đã bị vô hiệu hoá trước đó");
        }
        nd.setTrangThai(false);
        nguoiDungRepository.save(nd);
    }

    @Override
    public void khoiPhuc(UUID userId) {
        NguoiDung nd = findByIdOrThrow(userId);
        if (Boolean.TRUE.equals(nd.getTrangThai())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.CONFLICT,
                    "Tài khoản đang hoạt động, không cần khôi phục");
        }
        nd.setTrangThai(true);
        nguoiDungRepository.save(nd);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse layHoSoCaNhan(UUID currentUserId) {
        return toUserProfileResponse(findByIdOrThrow(currentUserId));
    }

    @Override
    public UserProfileResponse capNhatHoSoCaNhan(UUID currentUserId, CapNhatHoSoRequest request) {
        NguoiDung nd = findByIdOrThrow(currentUserId);

        if (request.hoTen() != null) nd.setHoTen(request.hoTen().trim());
        if (request.soDienThoai() != null) {
            String newPhone = request.soDienThoai().trim();
            if (!newPhone.equals(nd.getSoDienThoai()) && nguoiDungRepository.existsBySoDienThoai(newPhone)) {
                throw new BusinessException(ErrorCodes.DUPLICATE_PHONE, HttpStatus.CONFLICT,
                        "Số điện thoại đã tồn tại trong hệ thống");
            }
            nd.setSoDienThoai(newPhone);
        }
        if (request.diaChi() != null) nd.setDiaChi(request.diaChi().trim());
        if (request.ngaySinh() != null) nd.setNgaySinh(request.ngaySinh().atStartOfDay());
        if (request.gioiTinh() != null) nd.setGioiTinh(request.gioiTinh().trim());
        if (request.cccd() != null) nd.setCccd(request.cccd().trim());

        return toUserProfileResponse(nguoiDungRepository.save(nd));
    }

    @Override
    public void doiMatKhau(UUID currentUserId, DoiMatKhauRequest request) {
        if (!request.matKhauMoi().equals(request.xacNhanMatKhauMoi())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Mật khẩu mới và xác nhận mật khẩu không khớp");
        }
        NguoiDung nd = findByIdOrThrow(currentUserId);
        if (!passwordEncoder.matches(request.matKhauCu(), nd.getMatKhau())) {
            throw new BusinessException(ErrorCodes.INVALID_PASSWORD, HttpStatus.BAD_REQUEST,
                    "Mật khẩu hiện tại không đúng");
        }
        nd.setMatKhau(passwordEncoder.encode(request.matKhauMoi()));
        nguoiDungRepository.save(nd);
    }

    private NguoiDung findByIdOrThrow(UUID userId) {
        return nguoiDungRepository.findByIdFetchKhuVuc(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng với ID: " + userId));
    }

    private VaiTro parseVaiTro(String vaiTro) {
        if (vaiTro == null || vaiTro.isBlank()) return null;
        try {
            return VaiTro.valueOf(vaiTro.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Vai trò không hợp lệ: " + vaiTro + ". Các giá trị hợp lệ: ADMIN, WAREHOUSE_STAFF, USER");
        }
    }

    private UserProfileResponse toUserProfileResponse(NguoiDung nd) {
        KhuVuc kv = nd.getKhuVuc();
        return new UserProfileResponse(
                nd.getMaND().toString(),
                nd.getHoTen(),
                nd.getEmail(),
                nd.getSoDienThoai(),
                kv != null ? kv.getMaKhuVuc() : null,
                kv != null ? kv.getTenKhuVuc() : null,
                nd.getDiaChi(),
                nd.getNgayDangKy(),
                nd.getTrangThai(),
                nd.getNgaySinh(),
                nd.getGioiTinh(),
                nd.getCccd(),
                nd.getVaiTro().name()
        );
    }
}
