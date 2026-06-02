package csdlpt.sitemain.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import csdlpt.sitemain.domain.entity.ChiTietGioHang;
import csdlpt.sitemain.domain.entity.GioHang;
import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.dto.request.CapNhatSoLuongRequest;
import csdlpt.sitemain.dto.request.ThemVaoGioRequest;
import csdlpt.sitemain.dto.response.GioHangResponse;
import csdlpt.sitemain.dto.response.TonKhoResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.GioHangRepository;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.service.TonKhoService;
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

@ExtendWith(MockitoExtension.class)
class GioHangServiceImplTest {

    @Mock
    private GioHangRepository gioHangRepository;

    @Mock
    private SanPhamCoreRepository sanPhamCoreRepository;

    @Mock
    private TonKhoService tonKhoService;

    @InjectMocks
    private GioHangServiceImpl service;

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private SanPhamCore sanPhamActive() {
        SanPhamCore sp = new SanPhamCore();
        sp.setMaSP("SP001");
        sp.setTenSP("Laptop");
        sp.setGiaBan(new BigDecimal("15000000"));
        sp.setTrangThai(true);
        return sp;
    }

    private GioHang gioHangCoItem(SanPhamCore sp, int soLuong) {
        GioHang g = new GioHang();
        g.setMaGioHang(UUID.randomUUID());
        g.setMaND(USER_ID);
        g.setNgayTao(LocalDateTime.now());
        g.setNgayCapNhat(LocalDateTime.now());
        g.setTrangThai("active");

        ChiTietGioHang ct = new ChiTietGioHang();
        ct.setMaCTGH(UUID.randomUUID());
        ct.setGioHang(g);
        ct.setSanPham(sp);
        ct.setSoLuong(soLuong);
        ct.setNgayThem(LocalDateTime.now());

        g.setChiTietList(new ArrayList<>(List.of(ct)));
        return g;
    }

    private TonKhoResponse tonKhoConHang(String maSP) {
        return new TonKhoResponse(maSP, "Laptop", 15, 5, 10);
    }

    private TonKhoResponse tonKhoHetHang(String maSP) {
        return new TonKhoResponse(maSP, "Laptop", 5, 5, 0);
    }

    // ── getGioHang ──────────────────────────────────────────────────────────────

    @Test
    void getGioHang_khongCoGio_traRong() {
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.empty());

        GioHangResponse response = service.getGioHang(USER_ID);

        assertTrue(response.sanPhamHopLe().isEmpty());
        assertTrue(response.sanPhamHetHang().isEmpty());
        assertEquals(0, response.tongSoLuong());
        assertEquals(BigDecimal.ZERO, response.tongTien());
    }

    @Test
    void getGioHang_coGio_sanPhamConHang_vaoHopLe() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 3);
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.of(gioHang));
        when(tonKhoService.kiemTraTonKho("SP001")).thenReturn(tonKhoConHang("SP001"));

        GioHangResponse response = service.getGioHang(USER_ID);

        assertEquals(1, response.sanPhamHopLe().size());
        assertTrue(response.sanPhamHetHang().isEmpty());
        assertEquals(3, response.tongSoLuong());
        assertEquals(new BigDecimal("45000000"), response.tongTien());
        assertEquals("SP001", response.sanPhamHopLe().getFirst().maSP());
        assertEquals(new BigDecimal("15000000"), response.sanPhamHopLe().getFirst().giaBan());
        assertEquals(10, response.sanPhamHopLe().getFirst().soLuongKhaDung());
    }

    @Test
    void getGioHang_coGio_sanPhamHetHang_vaoHetHang() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 3);
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.of(gioHang));
        when(tonKhoService.kiemTraTonKho("SP001")).thenReturn(tonKhoHetHang("SP001"));

        GioHangResponse response = service.getGioHang(USER_ID);

        assertTrue(response.sanPhamHopLe().isEmpty());
        assertEquals(1, response.sanPhamHetHang().size());
        assertEquals(0, response.tongSoLuong());
        assertEquals(BigDecimal.ZERO, response.tongTien());
        assertEquals("SP001", response.sanPhamHetHang().getFirst().maSP());
        assertEquals(0, response.sanPhamHetHang().getFirst().soLuongKhaDung());
    }

    // ── themVaoGio ───────────────────────────────────────────────────────────────

    @Test
    void themVaoGio_sanPhamKhongTonTai_nemException() {
        when(sanPhamCoreRepository.findById("SP999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.themVaoGio(USER_ID, new ThemVaoGioRequest("SP999", 1)));
    }

    @Test
    void themVaoGio_sanPhamNgungKinhDoanh_nemException() {
        SanPhamCore sp = sanPhamActive();
        sp.setTrangThai(false);
        when(sanPhamCoreRepository.findById("SP001")).thenReturn(Optional.of(sp));

        assertThrows(ResourceNotFoundException.class,
                () -> service.themVaoGio(USER_ID, new ThemVaoGioRequest("SP001", 1)));
    }

    @Test
    void themVaoGio_gioMoi_taoGioVaThemItem() {
        SanPhamCore sp = sanPhamActive();
        when(sanPhamCoreRepository.findById("SP001")).thenReturn(Optional.of(sp));
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.empty());

        GioHang saved = new GioHang();
        saved.setMaGioHang(UUID.randomUUID());
        saved.setMaND(USER_ID);
        saved.setTrangThai("active");
        saved.setNgayTao(LocalDateTime.now());
        saved.setNgayCapNhat(LocalDateTime.now());
        ChiTietGioHang ct = new ChiTietGioHang();
        ct.setMaCTGH(UUID.randomUUID());
        ct.setGioHang(saved);
        ct.setSanPham(sp);
        ct.setSoLuong(2);
        ct.setNgayThem(LocalDateTime.now());
        saved.setChiTietList(new ArrayList<>(List.of(ct)));

        when(gioHangRepository.save(any(GioHang.class))).thenReturn(saved);
        when(tonKhoService.kiemTraTonKho("SP001")).thenReturn(tonKhoConHang("SP001"));

        GioHangResponse response = service.themVaoGio(USER_ID, new ThemVaoGioRequest("SP001", 2));

        verify(gioHangRepository).save(any(GioHang.class));
        assertEquals(1, response.sanPhamHopLe().size());
        assertEquals(2, response.tongSoLuong());
        assertEquals(new BigDecimal("30000000"), response.tongTien());
    }

    @Test
    void themVaoGio_daCoItem_congDonSoLuong() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 2);
        when(sanPhamCoreRepository.findById("SP001")).thenReturn(Optional.of(sp));
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.of(gioHang));
        when(gioHangRepository.save(gioHang)).thenReturn(gioHang);
        when(tonKhoService.kiemTraTonKho("SP001")).thenReturn(tonKhoConHang("SP001"));

        service.themVaoGio(USER_ID, new ThemVaoGioRequest("SP001", 3));

        assertEquals(5, gioHang.getChiTietList().getFirst().getSoLuong());
    }

    // ── capNhatSoLuong ───────────────────────────────────────────────────────────

    @Test
    void capNhatSoLuong_gioKhongTonTai_nemException() {
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.capNhatSoLuong(USER_ID, "SP001", new CapNhatSoLuongRequest(5)));
    }

    @Test
    void capNhatSoLuong_itemKhongCoTrongGio_nemException() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 2);
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.of(gioHang));

        assertThrows(ResourceNotFoundException.class,
                () -> service.capNhatSoLuong(USER_ID, "SP999", new CapNhatSoLuongRequest(5)));
    }

    @Test
    void capNhatSoLuong_thanhCong_ghiDeSoLuong() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 2);
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.of(gioHang));
        when(gioHangRepository.save(gioHang)).thenReturn(gioHang);
        when(tonKhoService.kiemTraTonKho("SP001")).thenReturn(tonKhoConHang("SP001"));

        service.capNhatSoLuong(USER_ID, "SP001", new CapNhatSoLuongRequest(10));

        assertEquals(10, gioHang.getChiTietList().getFirst().getSoLuong());
        verify(gioHangRepository).save(gioHang);
    }

    // ── xoaSanPham ───────────────────────────────────────────────────────────────

    @Test
    void xoaSanPham_gioKhongTonTai_nemException() {
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.xoaSanPham(USER_ID, "SP001"));
    }

    @Test
    void xoaSanPham_itemKhongCoTrongGio_nemException() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 1);
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.of(gioHang));

        assertThrows(ResourceNotFoundException.class,
                () -> service.xoaSanPham(USER_ID, "SP999"));
    }

    @Test
    void xoaSanPham_thanhCong_xoaItemKhoiList() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 1);
        when(gioHangRepository.findActiveByMaNDWithItems(USER_ID)).thenReturn(Optional.of(gioHang));

        service.xoaSanPham(USER_ID, "SP001");

        assertTrue(gioHang.getChiTietList().isEmpty());
        verify(gioHangRepository).save(gioHang);
    }

    // ── xoaGioHang ───────────────────────────────────────────────────────────────

    @Test
    void xoaGioHang_khongCoGio_noOp() {
        when(gioHangRepository.findByMaNDAndTrangThai(USER_ID, "active")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.xoaGioHang(USER_ID));
        verify(gioHangRepository, never()).save(any());
    }

    @Test
    void xoaGioHang_coGio_xoaHetItems() {
        SanPhamCore sp = sanPhamActive();
        GioHang gioHang = gioHangCoItem(sp, 1);
        when(gioHangRepository.findByMaNDAndTrangThai(USER_ID, "active")).thenReturn(Optional.of(gioHang));

        service.xoaGioHang(USER_ID);

        assertTrue(gioHang.getChiTietList().isEmpty());
        verify(gioHangRepository).save(gioHang);
    }
}
