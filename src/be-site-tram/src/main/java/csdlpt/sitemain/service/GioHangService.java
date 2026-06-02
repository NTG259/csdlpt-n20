package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.request.CapNhatSoLuongRequest;
import csdlpt.sitemain.dto.request.ThemVaoGioRequest;
import csdlpt.sitemain.dto.response.GioHangResponse;
import java.util.UUID;

public interface GioHangService {
    GioHangResponse getGioHang(UUID maND);
    GioHangResponse themVaoGio(UUID maND, ThemVaoGioRequest request);
    GioHangResponse capNhatSoLuong(UUID maND, String maSP, CapNhatSoLuongRequest request);
    void xoaSanPham(UUID maND, String maSP);
    void xoaGioHang(UUID maND);
}
