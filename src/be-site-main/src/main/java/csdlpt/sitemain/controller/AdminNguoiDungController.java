package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.DoiVaiTroRequest;
import csdlpt.sitemain.dto.request.NguoiDungAdminUpdateRequest;
import csdlpt.sitemain.dto.response.UserProfileResponse;
import csdlpt.sitemain.security.CustomUserDetails;
import csdlpt.sitemain.service.NguoiDungService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản lý Người dùng (Admin)", description = "CRUD người dùng, đổi vai trò, vô hiệu hoá và khôi phục tài khoản")
@RestController
@RequestMapping("/api/admin/users")
public class AdminNguoiDungController {

    private final NguoiDungService nguoiDungService;

    public AdminNguoiDungController(NguoiDungService nguoiDungService) {
        this.nguoiDungService = nguoiDungService;
    }

    @Operation(
            summary = "Danh sách người dùng",
            description = "Tìm kiếm và phân trang toàn bộ người dùng. Có thể lọc theo từ khoá, vai trò và trạng thái."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> layDanhSach(
            @Parameter(description = "Tìm theo tên, email hoặc số điện thoại")
            @RequestParam(required = false) String tuKhoa,

            @Parameter(description = "Lọc theo vai trò: ADMIN, WAREHOUSE_STAFF, USER")
            @RequestParam(required = false) String vaiTro,

            @Parameter(description = "true = đang hoạt động, false = đã vô hiệu hoá. Không truyền = tất cả")
            @RequestParam(required = false) Boolean trangThai,

            @PageableDefault(size = 20, sort = "hoTen") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                nguoiDungService.layDanhSachNguoiDung(tuKhoa, vaiTro, trangThai, pageable)
        ));
    }

    @Operation(
            summary = "Chi tiết người dùng",
            description = "Lấy thông tin đầy đủ của một người dùng theo ID."
    )
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> layChiTiet(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                nguoiDungService.layChiTietNguoiDung(userId)
        ));
    }

    @Operation(
            summary = "Cập nhật thông tin người dùng",
            description = "Admin cập nhật thông tin của người dùng bất kỳ. Chỉ các trường được gửi (không null) mới được cập nhật."
    )
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> capNhat(
            @PathVariable UUID userId,
            @Valid @RequestBody NguoiDungAdminUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                nguoiDungService.capNhatNguoiDung(userId, request)
        ));
    }

    @Operation(
            summary = "Đổi vai trò người dùng",
            description = "Thay đổi vai trò của người dùng. Admin không thể đổi vai trò của chính mình."
    )
    @PatchMapping("/{userId}/vai-tro")
    public ResponseEntity<Void> doiVaiTro(
            @PathVariable UUID userId,
            @Valid @RequestBody DoiVaiTroRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        nguoiDungService.doiVaiTro(userId, request, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Vô hiệu hoá tài khoản (soft delete)",
            description = "Đặt trangThai = false, tài khoản không thể đăng nhập nhưng dữ liệu vẫn được giữ lại. "
                    + "Admin không thể tự xoá chính mình."
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> xoaMem(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        nguoiDungService.xoaMem(userId, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Khôi phục tài khoản",
            description = "Đặt trangThai = true, cho phép tài khoản đăng nhập trở lại."
    )
    @PatchMapping("/{userId}/khoiphuc")
    public ResponseEntity<ApiResponse<UserProfileResponse>> khoiPhuc(
            @PathVariable UUID userId
    ) {
        nguoiDungService.khoiPhuc(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                nguoiDungService.layChiTietNguoiDung(userId)
        ));
    }
}
