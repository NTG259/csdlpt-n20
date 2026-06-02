package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.domain.entity.KhuVuc;
import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.domain.entity.UserGlobalIndex;
import csdlpt.sitemain.domain.enums.VaiTro;
import csdlpt.sitemain.dto.request.LoginRequest;
import csdlpt.sitemain.dto.request.RegisterRequest;
import csdlpt.sitemain.dto.response.AuthResponse;
import csdlpt.sitemain.dto.response.CheckAvailabilityResponse;
import csdlpt.sitemain.exception.DuplicateEmailException;
import csdlpt.sitemain.exception.DuplicatePhoneException;
import csdlpt.sitemain.exception.InvalidCredentialsException;
import csdlpt.sitemain.exception.InvalidRegionException;
import csdlpt.sitemain.repository.KhuVucRepository;
import csdlpt.sitemain.repository.NguoiDungRepository;
import csdlpt.sitemain.repository.UserGlobalIndexRepository;
import csdlpt.sitemain.security.JwtService;
import csdlpt.sitemain.service.AuthService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_GENDER = "Nam";
    private static final String TOKEN_TYPE = "Bearer";

    private final NguoiDungRepository nguoiDungRepository;
    private final UserGlobalIndexRepository userGlobalIndexRepository;
    private final KhuVucRepository khuVucRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            NguoiDungRepository nguoiDungRepository,
            UserGlobalIndexRepository userGlobalIndexRepository,
            KhuVucRepository khuVucRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.userGlobalIndexRepository = userGlobalIndexRepository;
        this.khuVucRepository = khuVucRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizeRequiredText(request.soDienThoai());
        String maKhuVuc = normalizeRequiredText(request.maKhuVuc());

        if (userGlobalIndexRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        if (userGlobalIndexRepository.existsBySoDienThoai(normalizedPhone)) {
            throw new DuplicatePhoneException();
        }

        if (!khuVucRepository.existsById(maKhuVuc)) {
            throw new InvalidRegionException();
        }

        UUID userId = UUID.randomUUID();
        KhuVuc khuVuc = khuVucRepository.getReferenceById(maKhuVuc);

        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setMaND(userId);
        nguoiDung.setMatKhau(passwordEncoder.encode(request.matKhau()));
        nguoiDung.setKhuVuc(khuVuc);
        nguoiDung.setHoTen(normalizeRequiredText(request.hoTen()));
        nguoiDung.setEmail(normalizedEmail);
        nguoiDung.setSoDienThoai(normalizedPhone);
        nguoiDung.setDiaChi(normalizeOptionalText(request.diaChi()));
        nguoiDung.setTrangThai(true);
        nguoiDung.setNgaySinh(toLocalDateTime(request.ngaySinh()));
        nguoiDung.setGioiTinh(resolveGender(request.gioiTinh()));
        nguoiDung.setCccd(normalizeOptionalText(request.cccd()));
        nguoiDung.setVaiTro(VaiTro.USER);

        UserGlobalIndex userGlobalIndex = new UserGlobalIndex();
        userGlobalIndex.setMaND(userId);
        userGlobalIndex.setEmail(normalizedEmail);
        userGlobalIndex.setSoDienThoai(normalizedPhone);
        userGlobalIndex.setKhuVuc(khuVuc);

        NguoiDung savedUser = nguoiDungRepository.save(nguoiDung);
        userGlobalIndexRepository.save(userGlobalIndex);

        String token = jwtService.generateToken(savedUser);
        return toAuthResponse(savedUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        NguoiDung nguoiDung = nguoiDungRepository.findByEmailFetchKhuVuc(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!Boolean.TRUE.equals(nguoiDung.getTrangThai())) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.matKhau(), nguoiDung.getMatKhau())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(nguoiDung);
        return toAuthResponse(nguoiDung, token);
    }

    @Override
    public CheckAvailabilityResponse isEmailAvailable(String email) {
        String normalizedEmail = normalizeEmail(email);
        return new CheckAvailabilityResponse(!userGlobalIndexRepository.existsByEmail(normalizedEmail));
    }

    @Override
    public CheckAvailabilityResponse isPhoneAvailable(String phone) {
        String normalizedPhone = normalizeRequiredText(phone);
        return new CheckAvailabilityResponse(!userGlobalIndexRepository.existsBySoDienThoai(normalizedPhone));
    }

    private AuthResponse toAuthResponse(NguoiDung nguoiDung, String token) {
        return new AuthResponse(
                token,
                TOKEN_TYPE,
                jwtService.getExpirationSeconds(),
                nguoiDung.getMaND().toString(),
                nguoiDung.getHoTen(),
                nguoiDung.getEmail(),
                nguoiDung.getKhuVuc().getMaKhuVuc(),
                nguoiDung.getVaiTro().name()
        );
    }

    private String normalizeEmail(String email) {
        return normalizeRequiredText(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeRequiredText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveGender(String gioiTinh) {
        String normalized = normalizeOptionalText(gioiTinh);
        return normalized == null ? DEFAULT_GENDER : normalized;
    }

    private LocalDateTime toLocalDateTime(LocalDate localDate) {
        return localDate == null ? null : localDate.atStartOfDay();
    }
}
