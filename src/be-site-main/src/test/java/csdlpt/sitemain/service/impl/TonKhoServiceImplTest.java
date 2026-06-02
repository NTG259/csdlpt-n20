package csdlpt.sitemain.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.dto.response.TonKhoChiTietKhoResponse;
import csdlpt.sitemain.dto.response.TonKhoHeThongResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.repository.TonKhoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TonKhoServiceImplTest {

    @Mock
    private TonKhoRepository tonKhoRepository;

    @Mock
    private SanPhamCoreRepository sanPhamCoreRepository;

    @InjectMocks
    private TonKhoServiceImpl tonKhoService;

    @Test
    void getTonKhoToanHeThongShouldAggregateWarehouseRows() {
        SanPhamCore sanPham = new SanPhamCore();
        sanPham.setMaSP("SP01");
        sanPham.setTenSP("Laptop A");

        when(sanPhamCoreRepository.findById("SP01")).thenReturn(Optional.of(sanPham));
        when(tonKhoRepository.timTonKhoToanHeThong("SP01")).thenReturn(List.of(
                new TonKhoChiTietKhoResponse("SITE_BAC", "KB01", "Kho Ha Noi", 30, 5, 25),
                new TonKhoChiTietKhoResponse("SITE_BAC", "KB02", "Kho Hai Phong", 10, 2, 8),
                new TonKhoChiTietKhoResponse("SITE_NAM", "KN01", "Kho HCM", 10, 1, 9)
        ));

        TonKhoHeThongResponse response = tonKhoService.getTonKhoToanHeThong(" SP01 ");

        assertEquals("SP01", response.maSP());
        assertEquals("Laptop A", response.tenSP());
        assertEquals(50, response.tongTonKho());
        assertEquals(8, response.tongDatHang());
        assertEquals(42, response.tongKhaDung());
        assertEquals(3, response.soLuongKho());
        assertEquals("KB01", response.chiTietKho().getFirst().maKho());
    }

    @Test
    void getTonKhoToanHeThongShouldReturnZeroTotalsWhenNoWarehouseRows() {
        SanPhamCore sanPham = new SanPhamCore();
        sanPham.setMaSP("SP01");
        sanPham.setTenSP("Laptop A");

        when(sanPhamCoreRepository.findById("SP01")).thenReturn(Optional.of(sanPham));
        when(tonKhoRepository.timTonKhoToanHeThong("SP01")).thenReturn(List.of());

        TonKhoHeThongResponse response = tonKhoService.getTonKhoToanHeThong("SP01");

        assertEquals(0, response.tongTonKho());
        assertEquals(0, response.tongDatHang());
        assertEquals(0, response.tongKhaDung());
        assertEquals(0, response.soLuongKho());
        assertEquals(List.of(), response.chiTietKho());
    }

    @Test
    void getTonKhoToanHeThongShouldThrowWhenProductNotFound() {
        when(sanPhamCoreRepository.findById("SP99")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tonKhoService.getTonKhoToanHeThong("SP99"));
        verifyNoInteractions(tonKhoRepository);
    }
}
