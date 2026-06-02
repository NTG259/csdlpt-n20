package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.domain.entity.ChiTietDonHang;
import csdlpt.sitemain.domain.entity.ChiTietGioHang;
import csdlpt.sitemain.domain.entity.DonHang;
import csdlpt.sitemain.domain.entity.GioHang;
import csdlpt.sitemain.dto.request.TaoDonHangRequest;
import csdlpt.sitemain.dto.response.ChiTietDonHangResponse;
import csdlpt.sitemain.dto.response.DonHangResponse;
import csdlpt.sitemain.dto.response.DonHangSummaryResponse;
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.exception.SqlServerErrorTranslator;
import csdlpt.sitemain.repository.DonHangRepository;
import csdlpt.sitemain.repository.GioHangRepository;
import csdlpt.sitemain.repository.OrderStoredProcedureDao;
import csdlpt.sitemain.repository.OrderStoredProcedureDao.DatHangParams;
import csdlpt.sitemain.service.OrderService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final GioHangRepository gioHangRepository;
    private final DonHangRepository donHangRepository;
    private final OrderStoredProcedureDao orderStoredProcedureDao;

    public OrderServiceImpl(GioHangRepository gioHangRepository,
                            DonHangRepository donHangRepository,
                            OrderStoredProcedureDao orderStoredProcedureDao) {
        this.gioHangRepository = gioHangRepository;
        this.donHangRepository = donHangRepository;
        this.orderStoredProcedureDao = orderStoredProcedureDao;
    }

    @Override
    public DonHangResponse taoDonHang(UUID maND, String maKhuVuc, TaoDonHangRequest request) {
        GioHang gioHang = gioHangRepository.findActiveByMaNDWithItems(maND)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.CART_EMPTY,
                        HttpStatus.BAD_REQUEST,
                        "Gio hang trong"));

        List<ChiTietGioHang> items = gioHang.getChiTietList();
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCodes.CART_EMPTY, HttpStatus.BAD_REQUEST, "Gio hang trong");
        }

        try {
            String maKhoNhan = orderStoredProcedureDao.chonKhoNhanToiUu(maKhuVuc, items);
            UUID maDonHang = orderStoredProcedureDao.datHang(new DatHangParams(
                    maND,
                    request.hoTenNguoiNhan(),
                    request.soDienThoaiNhan(),
                    request.diaChiGiao(),
                    maKhuVuc,
                    maKhoNhan,
                    request.phuongThucTT(),
                    request.ghiChu()
            ), items);

            gioHang.setTrangThai("ordered");
            gioHangRepository.save(gioHang);

            DonHang donHang = donHangRepository.findByIdWithItems(maDonHang)
                    .orElseThrow(() -> new ResourceNotFoundException("Don hang vua tao khong ton tai"));
            return toResponse(donHang);
        } catch (DataAccessException ex) {
            throw SqlServerErrorTranslator.translate(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DonHangSummaryResponse> getMyOrders(UUID maND, Pageable pageable) {
        return PageResponse.from(donHangRepository.findByMaND(maND, pageable).map(this::toSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public DonHangResponse getMyOrderDetail(UUID maND, UUID maDonHang) {
        DonHang donHang = donHangRepository.findByIdAndMaNDWithItems(maDonHang, maND)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.ORDER_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay don hang"));
        return toResponse(donHang);
    }

    @Override
    @Transactional
    public DonHangResponse xacNhanNhanHang(UUID maND, UUID maDonHang) {
        int updated = donHangRepository.hoanTatDonDangGiao(maDonHang, maND);

        if (updated == 0) {
            DonHang donHang = donHangRepository.findById(maDonHang)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.ORDER_NOT_FOUND,
                            HttpStatus.NOT_FOUND,
                            "Khong tim thay don hang"));

            if (!donHang.getMaND().equals(maND)) {
                throw new BusinessException(
                        ErrorCodes.ACCESS_DENIED,
                        HttpStatus.FORBIDDEN,
                        "Don hang khong thuoc ve ban");
            }

            throw new BusinessException(
                    ErrorCodes.INVALID_ORDER_STATE,
                    HttpStatus.CONFLICT,
                    "Don khong o trang thai dang giao nen khong the xac nhan");
        }

        DonHang donHang = donHangRepository.findByIdWithItems(maDonHang)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.ORDER_NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "Khong tim thay don hang"));
        return toResponse(donHang);
    }

    private DonHangSummaryResponse toSummaryResponse(DonHang donHang) {
        return new DonHangSummaryResponse(
                donHang.getMaDonHang(),
                donHang.getTrangThaiDH(),
                donHang.getTrangThaiTT(),
                donHang.getTongTien(),
                donHang.getNgayDat(),
                donHang.getMaKhuVucXuLi(),
                donHang.getHoTenNguoiNhan(),
                donHang.getSoDienThoaiNhan(),
                donHang.getDiaChiGiao(),
                donHang.getPhuongThucTT()
        );
    }

    private DonHangResponse toResponse(DonHang donHang) {
        List<ChiTietDonHangResponse> items = donHang.getChiTietList().stream()
                .sorted(Comparator.comparing(ct -> ct.getSanPham().getMaSP()))
                .map(this::toItemResponse)
                .toList();

        return new DonHangResponse(
                donHang.getMaDonHang(),
                donHang.getTrangThaiDH(),
                donHang.getTrangThaiTT(),
                donHang.getTongTien(),
                donHang.getNgayDat(),
                donHang.getMaKhuVucXuLi(),
                donHang.getHoTenNguoiNhan(),
                donHang.getSoDienThoaiNhan(),
                donHang.getDiaChiGiao(),
                donHang.getPhuongThucTT(),
                donHang.getGhiChu(),
                items
        );
    }

    private ChiTietDonHangResponse toItemResponse(ChiTietDonHang chiTiet) {
        BigDecimal thanhTien = chiTiet.getThanhTien();
        if (thanhTien == null) {
            thanhTien = chiTiet.getDonGia().multiply(BigDecimal.valueOf(chiTiet.getSoLuong()));
        }
        return new ChiTietDonHangResponse(
                chiTiet.getSanPham().getMaSP(),
                chiTiet.getSanPham().getTenSP(),
                chiTiet.getSoLuong(),
                chiTiet.getDonGia(),
                thanhTien
        );
    }
}
