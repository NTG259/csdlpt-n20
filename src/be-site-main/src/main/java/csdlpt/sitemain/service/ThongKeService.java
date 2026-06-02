package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.DoanhThuTheoThangResponse;
import csdlpt.sitemain.dto.response.DonHangNhieuKhoResponse;
import csdlpt.sitemain.dto.response.SanPhamBanChayResponse;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;
import java.util.List;

public interface ThongKeService {

    ThongKeDoanhThuResponse thongKeDoanhThu(ThongKeDoanhThuFilter filter);

    List<DoanhThuTheoThangResponse> thongKeDoanhThuTheoThang();

    List<SanPhamBanChayResponse> topSanPhamBanChay();

    List<DonHangNhieuKhoResponse> donHangXuatNhieuKho();
}
