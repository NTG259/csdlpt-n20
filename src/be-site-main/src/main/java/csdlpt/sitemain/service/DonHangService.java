package csdlpt.sitemain.service;

import csdlpt.sitemain.common.PageResponse;
import csdlpt.sitemain.dto.request.CapNhatTrangThaiDonHangRequest;
import csdlpt.sitemain.dto.response.DonHangDetailResponse;
import csdlpt.sitemain.dto.response.DonHangListItemResponse;
import java.time.LocalDate;

public interface DonHangService {

    PageResponse<DonHangListItemResponse> layDanhSach(
            String siteNguon,
            String trangThaiDH,
            String trangThaiTT,
            LocalDate tuNgay,
            LocalDate denNgay,
            int page,
            int size
    );

    DonHangDetailResponse layChiTiet(String maDonHang, String siteNguon);

    DonHangDetailResponse capNhatTrangThai(String maDonHang, CapNhatTrangThaiDonHangRequest request);
}
