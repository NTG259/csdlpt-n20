package csdlpt.sitemain.dto.response;

import java.util.List;

public record ThongKeDoanhThuResponse(
        List<DoanhThuTheoKhoResponse> theoKho,
        List<DoanhThuTheoVungResponse> theoVung,
        DoanhThuToanHeThongResponse toanHeThong
) {
}
