package csdlpt.sitemain.controller;

import csdlpt.sitemain.common.ApiResponse;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.response.PhieuNhapDetailResponse;
import csdlpt.sitemain.dto.response.PhieuNhapSummaryResponse;
import csdlpt.sitemain.dto.response.PhieuXuatDetailResponse;
import csdlpt.sitemain.dto.response.PhieuXuatSummaryResponse;
import csdlpt.sitemain.dto.response.ReadyToShipOrderResponse;
import csdlpt.sitemain.dto.response.WarehouseActionResponse;
import csdlpt.sitemain.dto.response.WarehouseContextResponse;
import csdlpt.sitemain.dto.response.WarehouseDashboardResponse;
import csdlpt.sitemain.dto.response.WarehouseTonKhoResponse;
import csdlpt.sitemain.security.CustomUserDetails;
import csdlpt.sitemain.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quan ly kho", description = "API cho nguoi quan ly kho/nhan vien kho tai site chi nhanh")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_STAFF')")
@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @Operation(summary = "Thong tin kho dang phu trach")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WarehouseContextResponse>> getContext(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getContext(userDetails)));
    }

    @Operation(summary = "Dashboard quan ly kho")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<WarehouseDashboardResponse>> getDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getDashboard(userDetails, fromDate, toDate)));
    }

    @Operation(summary = "Danh sach phieu xuat cua kho phu trach")
    @GetMapping("/phieu-xuat")
    public ResponseEntity<ApiResponse<PageResponse<PhieuXuatSummaryResponse>>> listPhieuXuat(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "loai", required = false) String loai,
            @RequestParam(name = "trangThaiXuat", required = false) String trangThaiXuat,
            @RequestParam(name = "trangThaiNhan", required = false) String trangThaiNhan,
            @RequestParam(name = "maDonHang", required = false) UUID maDonHang,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,
            @ParameterObject @PageableDefault(size = 20, sort = "ngayTao", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.listPhieuXuat(
                userDetails,
                loai,
                trangThaiXuat,
                trangThaiNhan,
                maDonHang,
                fromDate,
                toDate,
                pageable
        )));
    }

    @Operation(summary = "Chi tiet phieu xuat")
    @GetMapping("/phieu-xuat/{maPhieuXuat}")
    public ResponseEntity<ApiResponse<PhieuXuatDetailResponse>> getPhieuXuatDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("maPhieuXuat") UUID maPhieuXuat
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getPhieuXuatDetail(userDetails, maPhieuXuat)));
    }

    @Operation(summary = "Xac nhan xuat noi bo")
    @PostMapping("/phieu-xuat/{maPhieuXuat}/xac-nhan-noi-bo")
    public ResponseEntity<ApiResponse<WarehouseActionResponse>> confirmInternalExport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("maPhieuXuat") UUID maPhieuXuat
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Xac nhan xuat noi bo thanh cong",
                warehouseService.confirmInternalExport(userDetails, maPhieuXuat)
        ));
    }

    @Operation(summary = "Xac nhan xuat giao khach")
    @PostMapping("/phieu-xuat/{maPhieuXuat}/xac-nhan-giao-khach")
    public ResponseEntity<ApiResponse<WarehouseActionResponse>> confirmCustomerExport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("maPhieuXuat") UUID maPhieuXuat
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Xac nhan xuat giao khach thanh cong",
                warehouseService.confirmCustomerExport(userDetails, maPhieuXuat)
        ));
    }

    @Operation(summary = "Danh sach phieu nhap cua kho phu trach")
    @GetMapping("/phieu-nhap")
    public ResponseEntity<ApiResponse<PageResponse<PhieuNhapSummaryResponse>>> listPhieuNhap(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "trangThaiNhap", required = false) String trangThaiNhap,
            @RequestParam(name = "maDonHang", required = false) UUID maDonHang,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,
            @ParameterObject @PageableDefault(size = 20, sort = "ngayNhap", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.listPhieuNhap(
                userDetails,
                trangThaiNhap,
                maDonHang,
                fromDate,
                toDate,
                pageable
        )));
    }

    @Operation(summary = "Chi tiet phieu nhap")
    @GetMapping("/phieu-nhap/{maPhieuNhap}")
    public ResponseEntity<ApiResponse<PhieuNhapDetailResponse>> getPhieuNhapDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("maPhieuNhap") UUID maPhieuNhap
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.getPhieuNhapDetail(userDetails, maPhieuNhap)));
    }

    @Operation(summary = "Xac nhan nhap noi bo")
    @PostMapping("/phieu-nhap/{maPhieuNhap}/xac-nhan")
    public ResponseEntity<ApiResponse<WarehouseActionResponse>> confirmInternalImport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("maPhieuNhap") UUID maPhieuNhap
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Xac nhan nhap noi bo thanh cong",
                warehouseService.confirmInternalImport(userDetails, maPhieuNhap)
        ));
    }

    @Operation(summary = "Danh sach don san sang giao khach")
    @GetMapping("/orders/ready-to-ship")
    public ResponseEntity<ApiResponse<PageResponse<ReadyToShipOrderResponse>>> listReadyToShipOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "maDonHang", required = false) UUID maDonHang,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(name = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,
            @ParameterObject @PageableDefault(size = 20, sort = "ngayDat", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.listReadyToShipOrders(
                userDetails,
                maDonHang,
                fromDate,
                toDate,
                pageable
        )));
    }

    @Operation(summary = "Tao phieu xuat giao khach cho don da du hang")
    @PostMapping("/orders/{maDonHang}/tao-phieu-giao-khach")
    public ResponseEntity<ApiResponse<WarehouseActionResponse>> createCustomerExportSlip(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("maDonHang") UUID maDonHang
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Tao phieu xuat giao khach thanh cong",
                warehouseService.createCustomerExportSlip(userDetails, maDonHang)
        ));
    }

    @Operation(summary = "Ton kho cua kho phu trach")
    @GetMapping("/ton-kho")
    public ResponseEntity<ApiResponse<PageResponse<WarehouseTonKhoResponse>>> listStock(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "onlyReserved", required = false) Boolean onlyReserved,
            @RequestParam(name = "onlyLowStock", required = false) Boolean onlyLowStock,
            @ParameterObject @PageableDefault(size = 20, sort = "tenSP", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(warehouseService.listStock(
                userDetails,
                q,
                onlyReserved,
                onlyLowStock,
                pageable
        )));
    }
}
