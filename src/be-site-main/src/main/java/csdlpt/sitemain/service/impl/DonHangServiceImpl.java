package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.request.CapNhatTrangThaiDonHangRequest;
import csdlpt.sitemain.dto.response.DonHangDetailResponse;
import csdlpt.sitemain.dto.response.DonHangListItemResponse;
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.DonHangRepository;
import csdlpt.sitemain.service.DonHangService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DonHangServiceImpl implements DonHangService {

    private final DonHangRepository donHangRepository;

    public DonHangServiceImpl(DonHangRepository donHangRepository) {
        this.donHangRepository = donHangRepository;
    }

    @Override
    public PageResponse<DonHangListItemResponse> layDanhSach(
            String siteNguon,
            String trangThaiDH,
            String trangThaiTT,
            LocalDate tuNgay,
            LocalDate denNgay,
            int page,
            int size
    ) {
        validateSiteNguon(siteNguon);
        if (size < 1 || size > 100) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "size phải từ 1 đến 100");
        }

        LocalDateTime from = tuNgay != null ? tuNgay.atStartOfDay() : null;
        LocalDateTime to = denNgay != null ? denNgay.plusDays(1).atStartOfDay() : null;

        List<DonHangListItemResponse> items = donHangRepository.timKiem(
                siteNguon, trangThaiDH, trangThaiTT, from, to, page, size);
        long total = donHangRepository.demTong(siteNguon, trangThaiDH, trangThaiTT, from, to);

        return PageResponse.of(items, page, size, total);
    }

    @Override
    public DonHangDetailResponse layChiTiet(String maDonHang, String siteNguon) {
        String siteTable = resolveSiteTable(siteNguon);
        return donHangRepository.layChiTiet(maDonHang, siteTable)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng " + maDonHang + " trên " + siteNguon));
    }

    @Override
    public DonHangDetailResponse capNhatTrangThai(String maDonHang, CapNhatTrangThaiDonHangRequest request) {
        if (request.trangThaiDH() == null && request.trangThaiTT() == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "Cần cung cấp ít nhất một trong hai: trangThaiDH hoặc trangThaiTT");
        }

        String siteTable = resolveSiteTable(request.siteNguon());

        // Kiểm tra đơn hàng tồn tại
        donHangRepository.layChiTiet(maDonHang, siteTable)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn hàng " + maDonHang + " trên " + request.siteNguon()));

        if (request.trangThaiDH() != null) {
            donHangRepository.capNhatTrangThaiDH(maDonHang, siteTable, request.trangThaiDH().getDbValue());
        }
        if (request.trangThaiTT() != null) {
            donHangRepository.capNhatTrangThaiTT(maDonHang, siteTable, request.trangThaiTT().getDbValue());
        }

        return donHangRepository.layChiTiet(maDonHang, siteTable)
                .orElseThrow(() -> new ResourceNotFoundException("Không thể tải lại đơn hàng sau khi cập nhật"));
    }

    private String resolveSiteTable(String siteNguon) {
        if (siteNguon == null || siteNguon.isBlank()) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "siteNguon không được để trống. Giá trị hợp lệ: SITE_BAC, SITE_NAM");
        }
        return switch (siteNguon.toUpperCase()) {
            case "SITE_BAC" -> "[SITE_BAC]";
            case "SITE_NAM" -> "[SITE_NAM]";
            default -> throw new BusinessException(ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
                    "siteNguon không hợp lệ: " + siteNguon + ". Giá trị hợp lệ: SITE_BAC, SITE_NAM");
        };
    }

    private void validateSiteNguon(String siteNguon) {
        if (siteNguon != null && !siteNguon.isBlank()) {
            resolveSiteTable(siteNguon);
        }
    }
}
