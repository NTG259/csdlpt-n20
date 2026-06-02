package csdlpt.sitemain.service;

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
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {

    WarehouseContextResponse getContext(CustomUserDetails userDetails);

    WarehouseDashboardResponse getDashboard(
            CustomUserDetails userDetails,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );

    PageResponse<PhieuXuatSummaryResponse> listPhieuXuat(
            CustomUserDetails userDetails,
            String loai,
            String trangThaiXuat,
            String trangThaiNhan,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    PhieuXuatDetailResponse getPhieuXuatDetail(CustomUserDetails userDetails, UUID maPhieuXuat);

    WarehouseActionResponse confirmInternalExport(CustomUserDetails userDetails, UUID maPhieuXuat);

    WarehouseActionResponse confirmCustomerExport(CustomUserDetails userDetails, UUID maPhieuXuat);

    PageResponse<PhieuNhapSummaryResponse> listPhieuNhap(
            CustomUserDetails userDetails,
            String trangThaiNhap,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    PhieuNhapDetailResponse getPhieuNhapDetail(CustomUserDetails userDetails, UUID maPhieuNhap);

    WarehouseActionResponse confirmInternalImport(CustomUserDetails userDetails, UUID maPhieuNhap);

    PageResponse<ReadyToShipOrderResponse> listReadyToShipOrders(
            CustomUserDetails userDetails,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    WarehouseActionResponse createCustomerExportSlip(CustomUserDetails userDetails, UUID maDonHang);

    PageResponse<WarehouseTonKhoResponse> listStock(
            CustomUserDetails userDetails,
            String q,
            Boolean onlyReserved,
            Boolean onlyLowStock,
            Pageable pageable
    );
}
