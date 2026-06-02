package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;

public interface ThongKeService {

    ThongKeDoanhThuResponse thongKeDoanhThu(ThongKeDoanhThuFilter filter);
}
