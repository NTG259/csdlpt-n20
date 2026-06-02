package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.domain.entity.ChiTietGioHang;
import csdlpt.sitemain.domain.entity.GioHang;
import csdlpt.sitemain.domain.entity.SanPhamCore;
import csdlpt.sitemain.dto.request.CapNhatSoLuongRequest;
import csdlpt.sitemain.dto.request.ThemVaoGioRequest;
import csdlpt.sitemain.dto.response.ChiTietGioHangResponse;
import csdlpt.sitemain.dto.response.GioHangResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.GioHangRepository;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.service.GioHangService;
import csdlpt.sitemain.service.TonKhoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GioHangServiceImpl implements GioHangService {

    private final GioHangRepository gioHangRepository;
    private final SanPhamCoreRepository sanPhamCoreRepository;
    private final TonKhoService tonKhoService;

    public GioHangServiceImpl(GioHangRepository gioHangRepository,
                               SanPhamCoreRepository sanPhamCoreRepository,
                               TonKhoService tonKhoService) {
        this.gioHangRepository = gioHangRepository;
        this.sanPhamCoreRepository = sanPhamCoreRepository;
        this.tonKhoService = tonKhoService;
    }

    @Override
    @Transactional(readOnly = true)
    public GioHangResponse getGioHang(UUID maND) {
        return gioHangRepository.findActiveByMaNDWithItems(maND)
                .map(this::toResponse)
                .orElse(emptyResponse());
    }

    @Override
    public GioHangResponse themVaoGio(UUID maND, ThemVaoGioRequest request) {
        SanPhamCore sanPham = sanPhamCoreRepository.findById(request.maSP())
                .filter(sp -> Boolean.TRUE.equals(sp.getTrangThai()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sản phẩm không tồn tại hoặc ngừng kinh doanh"));

        GioHang gioHang = gioHangRepository.findActiveByMaNDWithItems(maND)
                .orElseGet(() -> taoGioHangMoi(maND));

        gioHang.getChiTietList().stream()
                .filter(ct -> ct.getSanPham().getMaSP().equals(request.maSP()))
                .findFirst()
                .ifPresentOrElse(
                        ct -> ct.setSoLuong(ct.getSoLuong() + request.soLuong()),
                        () -> gioHang.getChiTietList().add(taoChiTiet(gioHang, sanPham, request.soLuong()))
                );

        gioHang.setNgayCapNhat(LocalDateTime.now());
        return toResponse(gioHangRepository.save(gioHang));
    }

    @Override
    public GioHangResponse capNhatSoLuong(UUID maND, String maSP, CapNhatSoLuongRequest request) {
        GioHang gioHang = gioHangRepository.findActiveByMaNDWithItems(maND)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));

        ChiTietGioHang chiTiet = gioHang.getChiTietList().stream()
                .filter(ct -> ct.getSanPham().getMaSP().equals(maSP))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng"));

        chiTiet.setSoLuong(request.soLuong());
        gioHang.setNgayCapNhat(LocalDateTime.now());
        return toResponse(gioHangRepository.save(gioHang));
    }

    @Override
    public void xoaSanPham(UUID maND, String maSP) {
        GioHang gioHang = gioHangRepository.findActiveByMaNDWithItems(maND)
                .orElseThrow(() -> new ResourceNotFoundException("Giỏ hàng không tồn tại"));

        boolean removed = gioHang.getChiTietList()
                .removeIf(ct -> ct.getSanPham().getMaSP().equals(maSP));
        if (!removed) {
            throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng");
        }

        gioHang.setNgayCapNhat(LocalDateTime.now());
        gioHangRepository.save(gioHang);
    }

    @Override
    public void xoaGioHang(UUID maND) {
        gioHangRepository.findByMaNDAndTrangThai(maND, "active").ifPresent(gioHang -> {
            gioHang.getChiTietList().clear();
            gioHang.setNgayCapNhat(LocalDateTime.now());
            gioHangRepository.save(gioHang);
        });
    }

    private GioHang taoGioHangMoi(UUID maND) {
        GioHang gioHang = new GioHang();
        gioHang.setMaGioHang(UUID.randomUUID());
        gioHang.setMaND(maND);
        LocalDateTime now = LocalDateTime.now();
        gioHang.setNgayTao(now);
        gioHang.setNgayCapNhat(now);
        gioHang.setTrangThai("active");
        return gioHang;
    }

    private ChiTietGioHang taoChiTiet(GioHang gioHang, SanPhamCore sanPham, int soLuong) {
        ChiTietGioHang chiTiet = new ChiTietGioHang();
        chiTiet.setMaCTGH(UUID.randomUUID());
        chiTiet.setGioHang(gioHang);
        chiTiet.setSanPham(sanPham);
        chiTiet.setSoLuong(soLuong);
        chiTiet.setNgayThem(LocalDateTime.now());
        return chiTiet;
    }

    private GioHangResponse toResponse(GioHang gioHang) {
        Map<String, Integer> khaDungMap = gioHang.getChiTietList().stream()
                .collect(Collectors.toMap(
                        ct -> ct.getSanPham().getMaSP(),
                        ct -> tonKhoService.kiemTraTonKho(ct.getSanPham().getMaSP())
                                .tongSoLuongKhaDung()
                ));

        Map<Boolean, List<ChiTietGioHangResponse>> partitioned = gioHang.getChiTietList().stream()
                .map(ct -> {
                    BigDecimal giaBan = ct.getSanPham().getGiaBan();
                    int khaDung = khaDungMap.getOrDefault(ct.getSanPham().getMaSP(), 0);
                    return new ChiTietGioHangResponse(
                            ct.getSanPham().getMaSP(),
                            ct.getSanPham().getTenSP(),
                            ct.getSanPham().getHinhAnh(),
                            ct.getSanPham().getDonViTinh(),
                            ct.getSoLuong(),
                            giaBan,
                            giaBan.multiply(BigDecimal.valueOf(ct.getSoLuong())),
                            khaDung
                    );
                })
                .collect(Collectors.partitioningBy(ct -> ct.soLuongKhaDung() > 0));

        List<ChiTietGioHangResponse> hopLe = partitioned.get(true);
        List<ChiTietGioHangResponse> hetHang = partitioned.get(false);

        int tongSoLuong = hopLe.stream().mapToInt(ChiTietGioHangResponse::soLuong).sum();
        BigDecimal tongTien = hopLe.stream()
                .map(ChiTietGioHangResponse::thanhTien)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new GioHangResponse(hopLe, hetHang, tongSoLuong, tongTien);
    }

    private GioHangResponse emptyResponse() {
        return new GioHangResponse(List.of(), List.of(), 0, BigDecimal.ZERO);
    }
}
