package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.request.CapNhatTrangThaiDonHangRequest;
import csdlpt.sitemain.dto.response.DonHangDetailResponse;
import csdlpt.sitemain.dto.response.DonHangListItemResponse;
import csdlpt.sitemain.service.DonHangService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản lý Đơn hàng (Admin)", description = "Xem danh sách, chi tiết đơn hàng và cập nhật trạng thái trên SITE_BAC và SITE_NAM")
@RestController
@RequestMapping("/api/admin/orders")
public class AdminDonHangController {

    private final DonHangService donHangService;

    public AdminDonHangController(DonHangService donHangService) {
        this.donHangService = donHangService;
    }

    @Operation(
            summary = "Danh sách đơn hàng",
            description = "Tìm kiếm đơn hàng từ cả hai site SITE_BAC và SITE_NAM. "
                    + "Có thể lọc theo site, trạng thái đơn hàng, trạng thái thanh toán và khoảng ngày. "
                    + "Kết quả sắp xếp theo ngày đặt giảm dần."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DonHangListItemResponse>>> layDanhSach(
            @Parameter(description = "Lọc theo site: SITE_BAC hoặc SITE_NAM. Bỏ qua = cả hai site")
            @RequestParam(required = false) String siteNguon,

            @Parameter(description = "Lọc theo trạng thái đơn hàng: pending, processing, shipping, completed, cancelled")
            @RequestParam(required = false) String trangThaiDH,

            @Parameter(description = "Lọc theo trạng thái thanh toán: waiting_cod, paid, failed, cancelled")
            @RequestParam(required = false) String trangThaiTT,

            @Parameter(description = "Từ ngày (yyyy-MM-dd)", example = "2025-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,

            @Parameter(description = "Đến ngày (yyyy-MM-dd), bao gồm cả ngày này", example = "2025-12-31")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,

            @Parameter(description = "Trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Số bản ghi mỗi trang (1–100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                donHangService.layDanhSach(siteNguon, trangThaiDH, trangThaiTT, tuNgay, denNgay, page, size)
        ));
    }

    @Operation(
            summary = "Chi tiết đơn hàng",
            description = "Lấy thông tin đầy đủ của đơn hàng bao gồm danh sách sản phẩm. "
                    + "Tham số siteNguon bắt buộc để xác định đơn hàng thuộc site nào."
    )
    @GetMapping("/{maDonHang}")
    public ResponseEntity<ApiResponse<DonHangDetailResponse>> layChiTiet(
            @PathVariable String maDonHang,

            @Parameter(description = "Site chứa đơn hàng: SITE_BAC hoặc SITE_NAM", required = true)
            @RequestParam String siteNguon
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                donHangService.layChiTiet(maDonHang, siteNguon)
        ));
    }

    @Operation(
            summary = "Cập nhật trạng thái đơn hàng",
            description = "Cập nhật trạng thái đơn hàng (trangThaiDH) và/hoặc trạng thái thanh toán (trangThaiTT). "
                    + "Cần cung cấp ít nhất một trong hai. Trả về thông tin đơn hàng sau khi cập nhật."
    )
    @PatchMapping("/{maDonHang}/trang-thai")
    public ResponseEntity<ApiResponse<DonHangDetailResponse>> capNhatTrangThai(
            @PathVariable String maDonHang,
            @Valid @RequestBody CapNhatTrangThaiDonHangRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                donHangService.capNhatTrangThai(maDonHang, request)
        ));
    }
}
