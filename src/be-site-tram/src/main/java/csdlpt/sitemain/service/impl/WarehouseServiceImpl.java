package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.ErrorCodes;
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
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.exception.SqlServerErrorTranslator;
import csdlpt.sitemain.repository.WarehouseQueryRepository;
import csdlpt.sitemain.repository.WarehouseStoredProcedureDao;
import csdlpt.sitemain.security.CustomUserDetails;
import csdlpt.sitemain.service.WarehouseService;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private static final String TYPE_INTERNAL = "noi_bo";
    private static final String TYPE_CUSTOMER = "giao_khach";
    private static final String STATUS_WAITING_EXPORT = "waiting_export";
    private static final String STATUS_WAITING_IMPORT = "waiting_import";
    private static final String SOURCE_REMOTE = "remote";

    private static final Set<String> EXPORT_TYPES = Set.of(TYPE_INTERNAL, TYPE_CUSTOMER);
    private static final Set<String> EXPORT_STATUSES = Set.of("waiting_export", "exported", "cancelled");
    private static final Set<String> RECEIVE_STATUSES = Set.of("waiting_receive", "received");
    private static final Set<String> IMPORT_STATUSES = Set.of("waiting_import", "imported", "cancelled");

    private final WarehouseQueryRepository warehouseQueryRepository;
    private final WarehouseStoredProcedureDao warehouseStoredProcedureDao;

    public WarehouseServiceImpl(
            WarehouseQueryRepository warehouseQueryRepository,
            WarehouseStoredProcedureDao warehouseStoredProcedureDao
    ) {
        this.warehouseQueryRepository = warehouseQueryRepository;
        this.warehouseStoredProcedureDao = warehouseStoredProcedureDao;
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseContextResponse getContext(CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        return warehouseQueryRepository.findWarehouseContext(userDetails.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nguoi dung"));
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseDashboardResponse getDashboard(
            CustomUserDetails userDetails,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        String maKho = requireWarehouseCode(userDetails);
        validateDateRange(fromDate, toDate);
        return warehouseQueryRepository.getDashboard(maKho, fromDate, toDate);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PhieuXuatSummaryResponse> listPhieuXuat(
            CustomUserDetails userDetails,
            String loai,
            String trangThaiXuat,
            String trangThaiNhan,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        String maKho = requireWarehouseCode(userDetails);
        String normalizedLoai = normalizeOptional(loai);
        validateOptionalValue(normalizedLoai, EXPORT_TYPES, "Loai phieu xuat khong hop le");
        validateOptionalValue(normalizeOptional(trangThaiXuat), EXPORT_STATUSES, "Trang thai xuat khong hop le");
        validateOptionalValue(normalizeOptional(trangThaiNhan), RECEIVE_STATUSES, "Trang thai nhan khong hop le");
        validateDateRange(fromDate, toDate);

        return PageResponse.from(warehouseQueryRepository.findPhieuXuat(
                maKho,
                normalizedLoai,
                normalizeOptional(trangThaiXuat),
                normalizeOptional(trangThaiNhan),
                maDonHang,
                fromDate,
                toDate,
                pageable
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PhieuXuatDetailResponse getPhieuXuatDetail(CustomUserDetails userDetails, UUID maPhieuXuat) {
        String maKho = requireWarehouseCode(userDetails);
        return findPhieuXuatOrThrow(maKho, maPhieuXuat);
    }

    @Override
    @Transactional
    public WarehouseActionResponse confirmInternalExport(CustomUserDetails userDetails, UUID maPhieuXuat) {
        String maKho = requireWarehouseCode(userDetails);
        PhieuXuatDetailResponse phieuXuat = findPhieuXuatOrThrow(maKho, maPhieuXuat);
        if (!TYPE_INTERNAL.equals(phieuXuat.loaiPhieu())) {
            throw invalidSlipStatus("Phieu nay khong phai phieu xuat noi bo");
        }
        if (!STATUS_WAITING_EXPORT.equals(phieuXuat.trangThaiXuat())) {
            throw invalidSlipStatus("Phieu xuat khong o trang thai waiting_export");
        }

        try {
            return warehouseStoredProcedureDao.xacNhanXuatNoiBo(maPhieuXuat);
        } catch (DataAccessException ex) {
            throw SqlServerErrorTranslator.translateWarehouse(ex);
        }
    }

    @Override
    @Transactional
    public WarehouseActionResponse confirmCustomerExport(CustomUserDetails userDetails, UUID maPhieuXuat) {
        String maKho = requireWarehouseCode(userDetails);
        PhieuXuatDetailResponse phieuXuat = findPhieuXuatOrThrow(maKho, maPhieuXuat);
        if (!TYPE_CUSTOMER.equals(phieuXuat.loaiPhieu())) {
            throw invalidSlipStatus("Phieu nay khong phai phieu xuat giao khach");
        }
        if (!STATUS_WAITING_EXPORT.equals(phieuXuat.trangThaiXuat())) {
            throw invalidSlipStatus("Phieu xuat khong o trang thai waiting_export");
        }

        try {
            return warehouseStoredProcedureDao.xacNhanXuatGiaoKhach(maPhieuXuat);
        } catch (DataAccessException ex) {
            throw SqlServerErrorTranslator.translateWarehouse(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PhieuNhapSummaryResponse> listPhieuNhap(
            CustomUserDetails userDetails,
            String trangThaiNhap,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        String maKho = requireWarehouseCode(userDetails);
        validateOptionalValue(normalizeOptional(trangThaiNhap), IMPORT_STATUSES, "Trang thai nhap khong hop le");
        validateDateRange(fromDate, toDate);
        return PageResponse.from(warehouseQueryRepository.findPhieuNhap(
                maKho,
                normalizeOptional(trangThaiNhap),
                maDonHang,
                fromDate,
                toDate,
                pageable
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PhieuNhapDetailResponse getPhieuNhapDetail(CustomUserDetails userDetails, UUID maPhieuNhap) {
        String maKho = requireWarehouseCode(userDetails);
        return findPhieuNhapOrThrow(maKho, maPhieuNhap);
    }

    @Override
    @Transactional
    public WarehouseActionResponse confirmInternalImport(CustomUserDetails userDetails, UUID maPhieuNhap) {
        String maKho = requireWarehouseCode(userDetails);
        PhieuNhapDetailResponse phieuNhap = findPhieuNhapOrThrow(maKho, maPhieuNhap);
        if (!STATUS_WAITING_IMPORT.equals(phieuNhap.trangThaiNhap())) {
            throw invalidSlipStatus("Phieu nhap khong o trang thai waiting_import");
        }
        if (phieuNhap.sourceExportStatus() != null
                && !SOURCE_REMOTE.equals(phieuNhap.sourceExportStatus())
                && !"exported".equals(phieuNhap.sourceExportStatus())) {
            throw invalidSlipStatus("Phieu xuat nguon chua exported");
        }

        try {
            return warehouseStoredProcedureDao.xacNhanNhapNoiBo(maPhieuNhap, userDetails.getUserId());
        } catch (DataAccessException ex) {
            throw SqlServerErrorTranslator.translateWarehouse(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReadyToShipOrderResponse> listReadyToShipOrders(
            CustomUserDetails userDetails,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        String maKho = requireWarehouseCode(userDetails);
        validateDateRange(fromDate, toDate);
        return PageResponse.from(warehouseQueryRepository.findReadyToShipOrders(
                maKho,
                maDonHang,
                fromDate,
                toDate,
                pageable
        ));
    }

    @Override
    @Transactional
    public WarehouseActionResponse createCustomerExportSlip(CustomUserDetails userDetails, UUID maDonHang) {
        String maKho = requireWarehouseCode(userDetails);
        if (!warehouseQueryRepository.isReadyToShip(maKho, maDonHang)) {
            throw new BusinessException(
                    ErrorCodes.ORDER_NOT_READY_TO_SHIP,
                    HttpStatus.CONFLICT,
                    "Don hang chua san sang giao khach"
            );
        }

        try {
            return warehouseStoredProcedureDao.taoPhieuXuatGiaoKhach(maDonHang, maKho);
        } catch (DataAccessException ex) {
            throw SqlServerErrorTranslator.translateWarehouse(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WarehouseTonKhoResponse> listStock(
            CustomUserDetails userDetails,
            String q,
            Boolean onlyReserved,
            Boolean onlyLowStock,
            Pageable pageable
    ) {
        String maKho = requireWarehouseCode(userDetails);
        return PageResponse.from(warehouseQueryRepository.findTonKho(
                maKho,
                normalizeOptional(q),
                onlyReserved,
                onlyLowStock,
                pageable
        ));
    }

    private PhieuXuatDetailResponse findPhieuXuatOrThrow(String maKho, UUID maPhieuXuat) {
        return warehouseQueryRepository.findPhieuXuatDetail(maKho, maPhieuXuat)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.SLIP_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay phieu xuat"));
    }

    private PhieuNhapDetailResponse findPhieuNhapOrThrow(String maKho, UUID maPhieuNhap) {
        return warehouseQueryRepository.findPhieuNhapDetail(maKho, maPhieuNhap)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.SLIP_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay phieu nhap"));
    }

    private String requireWarehouseCode(CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        String maKho = userDetails.getNguoiDung().getMaKhoPhuTrach();
        if (maKho == null || maKho.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.WAREHOUSE_NOT_ASSIGNED,
                    HttpStatus.FORBIDDEN,
                    "Nguoi dung chua duoc gan kho phu trach"
            );
        }
        return maKho;
    }

    private void requireAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    HttpStatus.UNAUTHORIZED,
                    "Chua xac thuc"
            );
        }
    }

    private void validateDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "fromDate phai nho hon hoac bang toDate"
            );
        }
    }

    private void validateOptionalValue(String value, Set<String> allowedValues, String message) {
        if (value != null && !allowedValues.contains(value)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
        }
    }

    private BusinessException invalidSlipStatus(String message) {
        return new BusinessException(ErrorCodes.INVALID_SLIP_STATUS, HttpStatus.CONFLICT, message);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
