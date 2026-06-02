package csdlpt.sitemain.repository;

import csdlpt.sitemain.dto.response.TonKhoChiTietKhoResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TonKhoRepository {

    private final JdbcTemplate jdbcTemplate;

    public TonKhoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TonKhoChiTietKhoResponse> timTonKhoToanHeThong(String maSP) {
        return jdbcTemplate.query(
                "EXEC dbo.sp_TonKho_ToanHeThong ?",
                (rs, rowNum) -> new TonKhoChiTietKhoResponse(
                        rs.getString("Site"),
                        rs.getString("MaKho"),
                        rs.getString("TenKho"),
                        rs.getInt("SoLuongTon"),
                        rs.getInt("SoLuongDatHang"),
                        rs.getInt("SoLuongKhaDung")
                ),
                maSP
        );
    }
}
