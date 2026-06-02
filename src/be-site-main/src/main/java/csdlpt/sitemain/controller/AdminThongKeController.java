package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.DoanhThuTheoThangResponse;
import csdlpt.sitemain.dto.response.DonHangNhieuKhoResponse;
import csdlpt.sitemain.dto.response.SanPhamBanChayResponse;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;
import csdlpt.sitemain.service.ThongKeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Thống kê Admin", description = "Báo cáo tồn kho, doanh thu và đơn hàng trên toàn hệ thống (SITE_BAC + SITE_NAM)")
@RestController
@RequestMapping("/api/admin/thong-ke")
public class AdminThongKeController {

    private final ThongKeService thongKeService;

    public AdminThongKeController(ThongKeService thongKeService) {
        this.thongKeService = thongKeService;
    }

    @Operation(
            summary = "Doanh thu tổng hợp",
            description = "Thống kê doanh thu có thể lọc theo khoảng ngày, kho, khu vực hoặc sản phẩm. "
                    + "Trả về chi tiết theo kho, theo vùng và tổng toàn hệ thống."
    )
    @GetMapping("/doanh-thu")
    public ResponseEntity<ApiResponse<ThongKeDoanhThuResponse>> thongKeDoanhThu(
            @Parameter(description = "Từ ngày (yyyy-MM-dd)", example = "2025-01-01")
            @RequestParam(name = "tuNgay", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,

            @Parameter(description = "Đến ngày (yyyy-MM-dd)", example = "2025-12-31")
            @RequestParam(name = "denNgay", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,

            @Parameter(description = "Mã kho cần lọc", example = "KHO001")
            @RequestParam(name = "maKho", required = false) String maKho,

            @Parameter(description = "Mã khu vực cần lọc", example = "KV_BAC")
            @RequestParam(name = "maKhuVuc", required = false) String maKhuVuc,

            @Parameter(description = "Mã sản phẩm cần lọc", example = "SP001")
            @RequestParam(name = "maSP", required = false) String maSP,

            @Parameter(description = "true = chỉ tính phiếu đã xuất kho; false = tính cả chưa xuất. Mặc định true")
            @RequestParam(name = "chiTinhDaXuat", required = false) Boolean chiTinhDaXuat
    ) {
        ThongKeDoanhThuResponse response = thongKeService.thongKeDoanhThu(new ThongKeDoanhThuFilter(
                toStartOfDay(tuNgay),
                toStartOfDay(denNgay),
                maKho,
                maKhuVuc,
                maSP,
                chiTinhDaXuat
        ));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Doanh thu theo tháng / kho",
            description = "Trả về toàn bộ doanh thu nhóm theo năm, tháng và kho xuất trên cả SITE_BAC và SITE_NAM. "
                    + "Kết quả bao gồm các dòng tổng toàn hệ thống với `siteNguon = \"TOAN_HE_THONG\"` và `maKho = null`. "
                    + "Chỉ tính đơn hàng đã thanh toán, đã hoàn thành và phiếu đã xuất kho."
    )
    @GetMapping("/doanh-thu-theo-thang")
    public ResponseEntity<ApiResponse<List<DoanhThuTheoThangResponse>>> thongKeDoanhThuTheoThang() {
        List<DoanhThuTheoThangResponse> response = thongKeService.thongKeDoanhThuTheoThang();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Top 10 sản phẩm bán chạy",
            description = "Trả về 10 sản phẩm bán chạy nhất trên toàn hệ thống (SITE_BAC + SITE_NAM), "
                    + "sắp xếp theo tổng số lượng bán giảm dần, sau đó theo doanh thu. "
                    + "Chỉ tính đơn hàng đã thanh toán (`TrangThaiTT = paid`) và hoàn thành (`TrangThaiDH = completed`)."
    )
    @GetMapping("/san-pham-ban-chay")
    public ResponseEntity<ApiResponse<List<SanPhamBanChayResponse>>> topSanPhamBanChay() {
        List<SanPhamBanChayResponse> response = thongKeService.topSanPhamBanChay();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
            summary = "Đơn hàng xuất từ nhiều kho",
            description = "Trả về các đơn hàng được xuất từ 2 kho trở lên (kể cả liên site SITE_BAC ↔ SITE_NAM). "
                    + "Dùng để phát hiện đơn hàng bị tách xuất hoặc xuất bù từ nhiều kho. "
                    + "Kết quả sắp xếp theo số kho xuất giảm dần."
    )
    @GetMapping("/don-hang-nhieu-kho")
    public ResponseEntity<ApiResponse<List<DonHangNhieuKhoResponse>>> donHangXuatNhieuKho() {
        List<DonHangNhieuKhoResponse> response = thongKeService.donHangXuatNhieuKho();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private LocalDateTime toStartOfDay(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }
}
