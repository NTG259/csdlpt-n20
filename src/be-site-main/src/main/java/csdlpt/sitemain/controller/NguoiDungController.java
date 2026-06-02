package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.CapNhatHoSoRequest;
import csdlpt.sitemain.dto.request.DoiMatKhauRequest;
import csdlpt.sitemain.dto.response.UserProfileResponse;
import csdlpt.sitemain.security.CustomUserDetails;
import csdlpt.sitemain.service.NguoiDungService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tài khoản người dùng", description = "Xem và cập nhật hồ sơ cá nhân, đổi mật khẩu")
@RestController
@RequestMapping("/api/users")
public class NguoiDungController {

    private final NguoiDungService nguoiDungService;

    public NguoiDungController(NguoiDungService nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @Operation(
            summary = "Hồ sơ cá nhân",
            description = "Lấy thông tin của tài khoản đang đăng nhập."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> layHoSo(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                nguoiDungService.layHoSoCaNhan(currentUser.getUserId())
        ));
    }

    @Operation(
            summary = "Cập nhật hồ sơ",
            description = "Cập nhật thông tin cá nhân. Chỉ các trường được gửi (không null) mới được cập nhật. "
                    + "Không thể thay đổi email và vai trò qua endpoint này."
    )
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> capNhatHoSo(
            @Valid @RequestBody CapNhatHoSoRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                nguoiDungService.capNhatHoSoCaNhan(currentUser.getUserId(), request)
        ));
    }

    @Operation(
            summary = "Đổi mật khẩu",
            description = "Đổi mật khẩu của tài khoản đang đăng nhập. Yêu cầu cung cấp mật khẩu hiện tại."
    )
    @PutMapping("/me/mat-khau")
    public ResponseEntity<Void> doiMatKhau(
            @Valid @RequestBody DoiMatKhauRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        nguoiDungService.doiMatKhau(currentUser.getUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
