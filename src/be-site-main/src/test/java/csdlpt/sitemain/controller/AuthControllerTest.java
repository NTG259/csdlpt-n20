package csdlpt.sitemain.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import csdlpt.sitemain.config.SecurityConfig;
import csdlpt.sitemain.dto.request.LoginRequest;
import csdlpt.sitemain.dto.request.RegisterRequest;
import csdlpt.sitemain.dto.response.AuthResponse;
import csdlpt.sitemain.dto.response.CheckAvailabilityResponse;
import csdlpt.sitemain.security.CustomUserDetailsService;
import csdlpt.sitemain.security.JwtAuthenticationFilter;
import csdlpt.sitemain.security.JwtService;
import csdlpt.sitemain.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldRegisterSuccessfullyAndReturnCreated() throws Exception {
        String requestBody = """
                {
                  "hoTen": "Nguyen Van A",
                  "email": "a@example.com",
                  "soDienThoai": "0123456789",
                  "matKhau": "matkhau123",
                  "maKhuVuc": "KV01",
                  "diaChi": "123 Demo Street",
                  "ngaySinh": "2000-01-01",
                  "gioiTinh": "Nam",
                  "cccd": "123456789012"
                }
                """;
        AuthResponse response = new AuthResponse(
                "token-123",
                "Bearer",
                31536000L,
                "user-1",
                "Nguyen Van A",
                "a@example.com",
                "KV01",
                "USER"
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng ký thành công"))
                .andExpect(jsonPath("$.data.token").value("token-123"))
                .andExpect(jsonPath("$.data.email").value("a@example.com"))
                .andExpect(jsonPath("$.data.maKhuVuc").value("KV01"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        String requestBody = """
                {
                  "email": "a@example.com",
                  "matKhau": "matkhau123"
                }
                """;
        AuthResponse response = new AuthResponse(
                "token-456",
                "Bearer",
                31536000L,
                "user-1",
                "Nguyen Van A",
                "a@example.com",
                "KV01",
                "USER"
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.vaiTro").value("USER"));
    }

    @Test
    void shouldCheckEmailAvailabilityWithoutToken() throws Exception {
        when(authService.isEmailAvailable("a@example.com"))
                .thenReturn(new CheckAvailabilityResponse(false));

        mockMvc.perform(get("/api/auth/check-email")
                        .param("email", "a@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void shouldCheckPhoneAvailabilityWithoutToken() throws Exception {
        when(authService.isPhoneAvailable("0123456789"))
                .thenReturn(new CheckAvailabilityResponse(true));

        mockMvc.perform(get("/api/auth/check-phone")
                        .param("phone", "0123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void shouldReturnValidationErrorForInvalidRegisterRequest() throws Exception {
        String requestBody = """
                {
                  "hoTen": "",
                  "email": "invalid-email",
                  "soDienThoai": "123",
                  "matKhau": "123",
                  "maKhuVuc": "",
                  "diaChi": "123 Demo Street",
                  "ngaySinh": "2000-01-01",
                  "gioiTinh": "Nam",
                  "cccd": "123456789012"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(authService);
    }
}
