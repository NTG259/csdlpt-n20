package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.DoanhThuTheoThangResponse;
import csdlpt.sitemain.dto.response.DonHangNhieuKhoResponse;
import csdlpt.sitemain.dto.response.SanPhamBanChayResponse;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;
import csdlpt.sitemain.repository.ThongKeRepository;
import csdlpt.sitemain.service.ThongKeService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ThongKeServiceImpl implements ThongKeService {

    private final ThongKeRepository thongKeRepository;

    public ThongKeServiceImpl(ThongKeRepository thongKeRepository) {
        this.thongKeRepository = thongKeRepository;
    }

    @Override
    public ThongKeDoanhThuResponse thongKeDoanhThu(ThongKeDoanhThuFilter filter) {
        return thongKeRepository.thongKeDoanhThu(filter);
    }

    @Override
    public List<DoanhThuTheoThangResponse> thongKeDoanhThuTheoThang() {
        return thongKeRepository.thongKeDoanhThuTheoThang();
    }

    @Override
    public List<SanPhamBanChayResponse> topSanPhamBanChay() {
        return thongKeRepository.topSanPhamBanChay();
    }

    @Override
    public List<DonHangNhieuKhoResponse> donHangXuatNhieuKho() {
        return thongKeRepository.donHangXuatNhieuKho();
    }
}
