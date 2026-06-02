package csdlpt.sitemain.repository;

import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.DoanhThuTheoKhoResponse;
import csdlpt.sitemain.dto.response.DoanhThuTheoVungResponse;
import csdlpt.sitemain.dto.response.DoanhThuToanHeThongResponse;
import csdlpt.sitemain.dto.response.ThongKeDoanhThuResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@Repository
public class ThongKeRepository {

    private final SimpleJdbcCall thongKeDoanhThuCall;

    public ThongKeRepository(JdbcTemplate jdbcTemplate) {
        this.thongKeDoanhThuCall = new SimpleJdbcCall(jdbcTemplate)
                .withSchemaName("dbo")
                .withProcedureName("sp_ThongKeDoanhThu_ToanHeThong")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("TuNgay", Types.TIMESTAMP),
                        new SqlParameter("DenNgay", Types.TIMESTAMP),
                        new SqlParameter("MaKho", Types.VARCHAR),
                        new SqlParameter("MaKhuVuc", Types.VARCHAR),
                        new SqlParameter("MaSP", Types.VARCHAR),
                        new SqlParameter("ChiTinhDaXuat", Types.BIT)
                )
                .returningResultSet("theoKho", (rs, rowNum) -> new DoanhThuTheoKhoResponse(
                        rs.getString("SiteXuat"),
                        rs.getString("MaKhuVuc"),
                        rs.getString("MaKhoXuat"),
                        rs.getString("TenKho"),
                        rs.getInt("SoDonHang"),
                        rs.getInt("SoPhieuXuat"),
                        rs.getInt("TongSoLuongXuat"),
                        nonNullBigDecimal(rs.getBigDecimal("DoanhThu"))
                ))
                .returningResultSet("theoVung", (rs, rowNum) -> new DoanhThuTheoVungResponse(
                        rs.getString("MaKhuVuc"),
                        rs.getInt("SoDonHang"),
                        rs.getInt("SoPhieuXuat"),
                        rs.getInt("SoKhoThamGiaXuat"),
                        rs.getInt("TongSoLuongXuat"),
                        nonNullBigDecimal(rs.getBigDecimal("DoanhThu"))
                ))
                .returningResultSet("toanHeThong", (rs, rowNum) -> new DoanhThuToanHeThongResponse(
                        rs.getInt("TongSoDonHang"),
                        rs.getInt("TongSoPhieuXuat"),
                        rs.getInt("TongSoKhoThamGiaXuat"),
                        rs.getInt("TongSoLuongXuat"),
                        nonNullBigDecimal(rs.getBigDecimal("TongDoanhThu"))
                ));
    }

    public ThongKeDoanhThuResponse thongKeDoanhThu(ThongKeDoanhThuFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("TuNgay", toTimestamp(filter.tuNgay()))
                .addValue("DenNgay", toTimestamp(filter.denNgay()))
                .addValue("MaKho", normalizeNullable(filter.maKho()))
                .addValue("MaKhuVuc", normalizeNullable(filter.maKhuVuc()))
                .addValue("MaSP", normalizeNullable(filter.maSP()))
                .addValue("ChiTinhDaXuat", filter.chiTinhDaXuat() == null || filter.chiTinhDaXuat());

        Map<String, Object> result = thongKeDoanhThuCall.execute(params);
        List<DoanhThuTheoKhoResponse> theoKho = getResultSet(result, "theoKho");
        List<DoanhThuTheoVungResponse> theoVung = getResultSet(result, "theoVung");
        List<DoanhThuToanHeThongResponse> toanHeThong = getResultSet(result, "toanHeThong");

        return new ThongKeDoanhThuResponse(
                theoKho,
                theoVung,
                toanHeThong.isEmpty()
                        ? new DoanhThuToanHeThongResponse(0, 0, 0, 0, BigDecimal.ZERO)
                        : toanHeThong.getFirst()
        );
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BigDecimal nonNullBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getResultSet(Map<String, Object> result, String key) {
        Object value = result.get(key);
        return value instanceof List<?> ? (List<T>) value : List.of();
    }
}
