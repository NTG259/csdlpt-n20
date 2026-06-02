package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.response.TonKhoChiTietKhoResponse;
import csdlpt.sitemain.dto.response.TonKhoHeThongResponse;
import csdlpt.sitemain.exception.ResourceNotFoundException;
import csdlpt.sitemain.repository.SanPhamCoreRepository;
import csdlpt.sitemain.repository.TonKhoRepository;
import csdlpt.sitemain.service.TonKhoService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TonKhoServiceImpl implements TonKhoService {

    private final TonKhoRepository tonKhoRepository;
    private final SanPhamCoreRepository sanPhamCoreRepository;

    public TonKhoServiceImpl(
            TonKhoRepository tonKhoRepository,
            SanPhamCoreRepository sanPhamCoreRepository
    ) {
        this.tonKhoRepository = tonKhoRepository;
        this.sanPhamCoreRepository = sanPhamCoreRepository;
    }

    @Override
    public TonKhoHeThongResponse getTonKhoToanHeThong(String maSP) {
        String normalizedMaSP = normalizeRequired(maSP);
        var sanPham = sanPhamCoreRepository.findById(normalizedMaSP)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với mã: " + normalizedMaSP
                ));

        List<TonKhoChiTietKhoResponse> chiTietKho =
                tonKhoRepository.timTonKhoToanHeThong(normalizedMaSP);

        int tongTonKho = chiTietKho.stream()
                .mapToInt(TonKhoChiTietKhoResponse::soLuongTon)
                .sum();
        int tongDatHang = chiTietKho.stream()
                .mapToInt(TonKhoChiTietKhoResponse::soLuongDatHang)
                .sum();
        int tongKhaDung = chiTietKho.stream()
                .mapToInt(TonKhoChiTietKhoResponse::soLuongKhaDung)
                .sum();

        return new TonKhoHeThongResponse(
                normalizedMaSP,
                sanPham.getTenSP(),
                tongTonKho,
                tongDatHang,
                tongKhaDung,
                chiTietKho.size(),
                chiTietKho
        );
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }
}
