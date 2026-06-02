package csdlpt.sitemain.dto.request;

import java.time.LocalDateTime;

public record ThongKeDoanhThuFilter(
        LocalDateTime tuNgay,
        LocalDateTime denNgay,
        String maKho,
        String maKhuVuc,
        String maSP,
        Boolean chiTinhDaXuat
) {
}
