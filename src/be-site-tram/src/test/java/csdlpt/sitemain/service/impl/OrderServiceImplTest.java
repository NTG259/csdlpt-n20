package csdlpt.sitemain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.domain.entity.ChiTietDonHang;
import csdlpt.sitemain.domain.entity.DonHang;
import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.dto.response.DonHangResponse;
import csdlpt.sitemain.exception.BusinessException;
import csdlpt.sitemain.repository.DonHangRepository;
import csdlpt.sitemain.repository.GioHangRepository;
import csdlpt.sitemain.repository.OrderStoredProcedureDao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ORDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private GioHangRepository gioHangRepository;

    @Mock
    private DonHangRepository donHangRepository;

    @Mock
    private OrderStoredProcedureDao orderStoredProcedureDao;

    @InjectMocks
    private OrderServiceImpl service;

    @Test
    void xacNhanNhanHang_capNhatThanhCong_traDonCompleted() {
        DonHang completed = donHang(USER_ID, "completed");
        completed.setTrangThaiTT("paid");
        when(donHangRepository.hoanTatDonDangGiao(ORDER_ID, USER_ID)).thenReturn(1);
        when(donHangRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(completed));

        DonHangResponse response = service.xacNhanNhanHang(USER_ID, ORDER_ID);

        assertEquals(ORDER_ID, response.maDonHang());
        assertEquals("completed", response.trangThaiDH());
        assertEquals("paid", response.trangThaiTT());
        assertEquals(1, response.items().size());
        verify(donHangRepository).hoanTatDonDangGiao(ORDER_ID, USER_ID);
    }

    @Test
    void xacNhanNhanHang_khongTimThayDon_nemOrderNotFound() {
        when(donHangRepository.hoanTatDonDangGiao(ORDER_ID, USER_ID)).thenReturn(0);
        when(donHangRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.xacNhanNhanHang(USER_ID, ORDER_ID));

        assertEquals(ErrorCodes.ORDER_NOT_FOUND, ex.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void xacNhanNhanHang_donCuaNguoiKhac_nemAccessDenied() {
        when(donHangRepository.hoanTatDonDangGiao(ORDER_ID, USER_ID)).thenReturn(0);
        when(donHangRepository.findById(ORDER_ID)).thenReturn(Optional.of(donHang(OTHER_USER_ID, "shipping")));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.xacNhanNhanHang(USER_ID, ORDER_ID));

        assertEquals(ErrorCodes.ACCESS_DENIED, ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void xacNhanNhanHang_donKhongDangGiao_nemInvalidOrderState() {
        when(donHangRepository.hoanTatDonDangGiao(ORDER_ID, USER_ID)).thenReturn(0);
        when(donHangRepository.findById(ORDER_ID)).thenReturn(Optional.of(donHang(USER_ID, "processing")));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.xacNhanNhanHang(USER_ID, ORDER_ID));

        assertEquals(ErrorCodes.INVALID_ORDER_STATE, ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    private DonHang donHang(UUID maND, String trangThaiDH) {
        DonHang donHang = new DonHang();
        donHang.setMaDonHang(ORDER_ID);
        donHang.setMaND(maND);
        donHang.setNgayDat(LocalDateTime.now());
        donHang.setHoTenNguoiNhan("Nguyen Van A");
        donHang.setSoDienThoaiNhan("0900000000");
        donHang.setDiaChiGiao("Ha Noi");
        donHang.setMaKhuVucXuLi("BAC");
        donHang.setTongTien(new BigDecimal("100000"));
        donHang.setPhuongThucTT("COD");
        donHang.setTrangThaiTT("waiting_cod");
        donHang.setTrangThaiDH(trangThaiDH);
        donHang.setChiTietList(new ArrayList<>(List.of(item(donHang))));
        return donHang;
    }

    private ChiTietDonHang item(DonHang donHang) {
        SanPhamCore sanPham = new SanPhamCore();
        sanPham.setMaSP("SP001");
        sanPham.setTenSP("Laptop");

        ChiTietDonHang item = new ChiTietDonHang();
        item.setMaCTDH(UUID.randomUUID());
        item.setDonHang(donHang);
        item.setSanPham(sanPham);
        item.setSoLuong(1);
        item.setDonGia(new BigDecimal("100000"));
        item.setThanhTien(new BigDecimal("100000"));
        return item;
    }
}
