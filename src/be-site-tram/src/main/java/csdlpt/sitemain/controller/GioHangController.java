package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.CapNhatSoLuongRequest;
import csdlpt.sitemain.dto.request.ThemVaoGioRequest;
import csdlpt.sitemain.dto.response.GioHangResponse;
import csdlpt.sitemain.security.CustomUserDetails;
import csdlpt.sitemain.service.GioHangService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Giỏ hàng", description = "Quản lý giỏ hàng — tất cả endpoint yêu cầu Bearer token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/cart")
public class GioHangController {

    private final GioHangService gioHangService;

    public GioHangController(GioHangService gioHangService) {
        this.gioHangService = gioHangService;
    }

    @Operation(
            summary = "Xem giỏ hàng",
            description = "Trả về giỏ hàng đang hoạt động của người dùng. " +
                          "Nếu chưa có giỏ hàng, trả về danh sách rỗng với tongSoLuong = 0.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công",
                    content = @Content(schema = @Schema(implementation = GioHangResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<ApiResponse<GioHangResponse>> getGioHang(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(gioHangService.getGioHang(userDetails.getUserId())));
    }

    @Operation(
            summary = "Thêm sản phẩm vào giỏ",
            description = "Thêm sản phẩm vào giỏ hàng. " +
                          "Nếu sản phẩm đã có trong giỏ, số lượng được cộng dồn. " +
                          "Nếu chưa có giỏ hàng, tự động tạo mới.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã thêm — trả về giỏ hàng sau khi cập nhật",
                    content = @Content(schema = @Schema(implementation = GioHangResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (maSP rỗng, soLuong < 1)",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sản phẩm không tồn tại hoặc đã ngừng kinh doanh",
                    content = @Content)
    })
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<GioHangResponse>> themVaoGio(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ThemVaoGioRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm sản phẩm vào giỏ hàng",
                gioHangService.themVaoGio(userDetails.getUserId(), request)));
    }

    @Operation(
            summary = "Cập nhật số lượng",
            description = "Ghi đè số lượng của một sản phẩm trong giỏ. Không cộng dồn — đặt thẳng về giá trị gửi lên.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã cập nhật — trả về giỏ hàng sau khi cập nhật",
                    content = @Content(schema = @Schema(implementation = GioHangResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "soLuong < 1",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Giỏ hàng không tồn tại hoặc sản phẩm không có trong giỏ",
                    content = @Content)
    })
    @PutMapping("/items/{maSP}")
    public ResponseEntity<ApiResponse<GioHangResponse>> capNhatSoLuong(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(name = "maSP", description = "Mã sản phẩm cần cập nhật", example = "SP001")
            @PathVariable("maSP") String maSP,
            @Valid @RequestBody CapNhatSoLuongRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật số lượng",
                gioHangService.capNhatSoLuong(userDetails.getUserId(), maSP, request)));
    }

    @Operation(
            summary = "Xóa một sản phẩm khỏi giỏ",
            description = "Xóa toàn bộ dòng sản phẩm khỏi giỏ hàng, bất kể số lượng.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã xóa",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Giỏ hàng không tồn tại hoặc sản phẩm không có trong giỏ",
                    content = @Content)
    })
    @DeleteMapping("/items/{maSP}")
    public ResponseEntity<ApiResponse<Void>> xoaSanPham(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(name = "maSP", description = "Mã sản phẩm cần xóa", example = "SP001")
            @PathVariable("maSP") String maSP
    ) {
        gioHangService.xoaSanPham(userDetails.getUserId(), maSP);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa sản phẩm khỏi giỏ hàng", null));
    }

    @Operation(
            summary = "Xóa toàn bộ giỏ hàng",
            description = "Xóa tất cả sản phẩm trong giỏ hàng. Nếu chưa có giỏ, không làm gì (không báo lỗi).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đã xóa",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực",
                    content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> xoaGioHang(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        gioHangService.xoaGioHang(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa toàn bộ giỏ hàng", null));
    }
}
