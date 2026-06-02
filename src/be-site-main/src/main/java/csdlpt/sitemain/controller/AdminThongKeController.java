package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;
import csdlpt.sitemain.service.ThongKeService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/thong-ke")
public class AdminThongKeController {

    private final ThongKeService thongKeService;

    public AdminThongKeController(ThongKeService thongKeService) {
        this.thongKeService = thongKeService;
    }

    @GetMapping("/doanh-thu")
    public ResponseEntity<ApiResponse<ThongKeDoanhThuResponse>> thongKeDoanhThu(
            @RequestParam(name = "tuNgay", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(name = "denNgay", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(name = "maKho", required = false) String maKho,
            @RequestParam(name = "maKhuVuc", required = false) String maKhuVuc,
            @RequestParam(name = "maSP", required = false) String maSP,
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

    private LocalDateTime toStartOfDay(LocalDate value) {
        return value == null ? null : value.atStartOfDay();
    }
}
