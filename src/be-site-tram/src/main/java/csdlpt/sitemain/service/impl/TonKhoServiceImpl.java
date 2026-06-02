package csdlpt.sitemain.service.impl;

import csdlpt.sitemain.dto.response.TonKhoResponse;
import csdlpt.sitemain.service.TonKhoService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TonKhoServiceImpl implements TonKhoService {

    private final JdbcTemplate jdbcTemplate;

    public TonKhoServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TonKhoResponse kiemTraTonKho(String maSP) {
        return jdbcTemplate.queryForObject(
                "{call dbo.sp_KiemTraTonKho_ToanHeThong(?)}",
                (rs, rowNum) -> new TonKhoResponse(
                        rs.getString("MaSP"),
                        rs.getString("TenSP"),
                        rs.getInt("TongSoLuongTon"),
                        rs.getInt("TongSoLuongDatHang"),
                        rs.getInt("TongSoLuongKhaDung")
                ),
                maSP
        );
    }
}
