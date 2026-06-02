package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.LoginRequest;
import csdlpt.sitemain.dto.request.RegisterRequest;
import csdlpt.sitemain.dto.response.AuthResponse;
import csdlpt.sitemain.dto.response.CheckAvailabilityResponse;
import csdlpt.sitemain.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký thành công", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkEmail(
            @RequestParam("email")
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không đúng định dạng")
            String email
    ) {
        CheckAvailabilityResponse response = authService.isEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/check-phone")
    public ResponseEntity<ApiResponse<CheckAvailabilityResponse>> checkPhone(
            @RequestParam("phone")
            @NotBlank(message = "Số điện thoại không được để trống")
            @Pattern(
                    regexp = "^(0|\\+84)\\d{9,10}$",
                    message = "Số điện thoại không đúng định dạng"
            )
            String phone
    ) {
        CheckAvailabilityResponse response = authService.isPhoneAvailable(phone);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
