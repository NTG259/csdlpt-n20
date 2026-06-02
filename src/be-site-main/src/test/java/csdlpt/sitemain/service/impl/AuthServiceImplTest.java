package csdlpt.sitemain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csdlpt.sitemain.domain.entity.KhuVuc;
import csdlpt.sitemain.domain.entity.NguoiDung;
import csdlpt.sitemain.domain.entity.UserGlobalIndex;
import csdlpt.sitemain.domain.enums.VaiTro;
import csdlpt.sitemain.dto.request.LoginRequest;
import csdlpt.sitemain.dto.request.RegisterRequest;
import csdlpt.sitemain.dto.response.AuthResponse;
import csdlpt.sitemain.exception.DuplicateEmailException;
import csdlpt.sitemain.exception.InvalidCredentialsException;
import csdlpt.sitemain.repository.KhuVucRepository;
import csdlpt.sitemain.repository.NguoiDungRepository;
import csdlpt.sitemain.repository.UserGlobalIndexRepository;
import csdlpt.sitemain.security.JwtService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private NguoiDungRepository nguoiDungRepository;

    @Mock
    private UserGlobalIndexRepository userGlobalIndexRepository;

    @Mock
    private KhuVucRepository khuVucRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest(
                " Nguyen Van A ",
                " USER@Example.com ",
                " 0123456789 ",
                "secret123",
                " KV01 ",
                " 123 Demo Street ",
                LocalDate.of(2000, 1, 1),
                null,
                " 123456789012 "
        );

        KhuVuc khuVuc = new KhuVuc("KV01", "Miền Bắc");

        when(userGlobalIndexRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userGlobalIndexRepository.existsBySoDienThoai("0123456789")).thenReturn(false);
        when(khuVucRepository.existsById("KV01")).thenReturn(true);
        when(khuVucRepository.getReferenceById("KV01")).thenReturn(khuVuc);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
        when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGlobalIndexRepository.save(any(UserGlobalIndex.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(NguoiDung.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86_400L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<NguoiDung> userCaptor = ArgumentCaptor.forClass(NguoiDung.class);
        ArgumentCaptor<UserGlobalIndex> indexCaptor = ArgumentCaptor.forClass(UserGlobalIndex.class);
        verify(nguoiDungRepository).save(userCaptor.capture());
        verify(userGlobalIndexRepository).save(indexCaptor.capture());

        NguoiDung savedUser = userCaptor.getValue();
        UserGlobalIndex savedIndex = indexCaptor.getValue();

        assertNotNull(savedUser.getMaND());
        assertEquals(savedUser.getMaND(), savedIndex.getMaND());
        assertEquals("Nguyen Van A", savedUser.getHoTen());
        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals("0123456789", savedUser.getSoDienThoai());
        assertEquals("123 Demo Street", savedUser.getDiaChi());
        assertEquals("Nam", savedUser.getGioiTinh());
        assertEquals("123456789012", savedUser.getCccd());
        assertEquals(VaiTro.USER, savedUser.getVaiTro());
        assertEquals(true, savedUser.getTrangThai());
        assertEquals("hashed-password", savedUser.getMatKhau());
        assertEquals("KV01", savedIndex.getKhuVuc().getMaKhuVuc());

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86_400L, response.expiresIn());
        assertEquals(savedUser.getMaND().toString(), response.userId());
        assertEquals("KV01", response.maKhuVuc());
        assertEquals("USER", response.vaiTro());
    }

    @Test
    void registerShouldThrowDuplicateEmailWhenEmailExists() {
        RegisterRequest request = new RegisterRequest(
                "Nguyen Van A",
                "user@example.com",
                "0123456789",
                "secret123",
                "KV01",
                null,
                null,
                null,
                null
        );

        when(userGlobalIndexRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(nguoiDungRepository, never()).save(any(NguoiDung.class));
        verify(userGlobalIndexRepository, never()).save(any(UserGlobalIndex.class));
    }

    @Test
    void loginShouldReturnAuthResponseWhenCredentialsAreValid() {
        KhuVuc khuVuc = new KhuVuc("KV01", "Miền Bắc");
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setMaND(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        nguoiDung.setEmail("user@example.com");
        nguoiDung.setMatKhau("hashed-password");
        nguoiDung.setHoTen("Nguyen Van A");
        nguoiDung.setKhuVuc(khuVuc);
        nguoiDung.setVaiTro(VaiTro.USER);
        nguoiDung.setTrangThai(true);

        when(nguoiDungRepository.findByEmailFetchKhuVuc("user@example.com")).thenReturn(Optional.of(nguoiDung));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(nguoiDung)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(86_400L);

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "secret123"));

        assertEquals("jwt-token", response.token());
        assertEquals("11111111-1111-1111-1111-111111111111", response.userId());
        assertEquals("KV01", response.maKhuVuc());
        assertEquals("USER", response.vaiTro());
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenPasswordDoesNotMatch() {
        KhuVuc khuVuc = new KhuVuc("KV01", "Miền Bắc");
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setEmail("user@example.com");
        nguoiDung.setMatKhau("hashed-password");
        nguoiDung.setKhuVuc(khuVuc);
        nguoiDung.setVaiTro(VaiTro.USER);
        nguoiDung.setTrangThai(true);

        when(nguoiDungRepository.findByEmailFetchKhuVuc("user@example.com")).thenReturn(Optional.of(nguoiDung));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("user@example.com", "wrong-password"))
        );
    }
}
