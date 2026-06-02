package csdlpt.sitemain.repository;

import csdlpt.sitemain.dto.response.ChiTietDonHangResponse;
import csdlpt.sitemain.dto.response.DonHangDetailResponse;
import csdlpt.sitemain.dto.response.DonHangListItemResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DonHangRepository {

    private static final String SQL_LIST = """
            SELECT SiteNguon, MaDonHang, MaND, NgayDat, HoTenNguoiNhan, SoDienThoaiNhan,
                   MaKhuVucXuLi, TongTien, PhuongThucTT, TrangThaiTT, TrangThaiDH
            FROM (
                SELECT 'SITE_BAC' AS SiteNguon, dh.MaDonHang, dh.MaND, dh.NgayDat,
                       dh.HoTenNguoiNhan, dh.SoDienThoaiNhan, dh.MaKhuVucXuLi,
                       dh.TongTien, dh.PhuongThucTT, dh.TrangThaiTT, dh.TrangThaiDH
                FROM [SITE_BAC].[store_management].dbo.DonHang dh
                WHERE (:siteNguon IS NULL OR :siteNguon = 'SITE_BAC')
                  AND (:trangThaiDH IS NULL OR dh.TrangThaiDH = :trangThaiDH)
                  AND (:trangThaiTT IS NULL OR dh.TrangThaiTT = :trangThaiTT)
                  AND (:tuNgay IS NULL OR dh.NgayDat >= :tuNgay)
                  AND (:denNgay IS NULL OR dh.NgayDat < :denNgay)

                UNION ALL

                SELECT 'SITE_NAM' AS SiteNguon, dh.MaDonHang, dh.MaND, dh.NgayDat,
                       dh.HoTenNguoiNhan, dh.SoDienThoaiNhan, dh.MaKhuVucXuLi,
                       dh.TongTien, dh.PhuongThucTT, dh.TrangThaiTT, dh.TrangThaiDH
                FROM [SITE_NAM].[store_management].dbo.DonHang dh
                WHERE (:siteNguon IS NULL OR :siteNguon = 'SITE_NAM')
                  AND (:trangThaiDH IS NULL OR dh.TrangThaiDH = :trangThaiDH)
                  AND (:trangThaiTT IS NULL OR dh.TrangThaiTT = :trangThaiTT)
                  AND (:tuNgay IS NULL OR dh.NgayDat >= :tuNgay)
                  AND (:denNgay IS NULL OR dh.NgayDat < :denNgay)
            ) AS T
            ORDER BY NgayDat DESC
            OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
            """;

    private static final String SQL_COUNT = """
            SELECT COUNT(*)
            FROM (
                SELECT 1 AS N
                FROM [SITE_BAC].[store_management].dbo.DonHang dh
                WHERE (:siteNguon IS NULL OR :siteNguon = 'SITE_BAC')
                  AND (:trangThaiDH IS NULL OR dh.TrangThaiDH = :trangThaiDH)
                  AND (:trangThaiTT IS NULL OR dh.TrangThaiTT = :trangThaiTT)
                  AND (:tuNgay IS NULL OR dh.NgayDat >= :tuNgay)
                  AND (:denNgay IS NULL OR dh.NgayDat < :denNgay)

                UNION ALL

                SELECT 1 AS N
                FROM [SITE_NAM].[store_management].dbo.DonHang dh
                WHERE (:siteNguon IS NULL OR :siteNguon = 'SITE_NAM')
                  AND (:trangThaiDH IS NULL OR dh.TrangThaiDH = :trangThaiDH)
                  AND (:trangThaiTT IS NULL OR dh.TrangThaiTT = :trangThaiTT)
                  AND (:tuNgay IS NULL OR dh.NgayDat >= :tuNgay)
                  AND (:denNgay IS NULL OR dh.NgayDat < :denNgay)
            ) AS T
            """;

    private final NamedParameterJdbcTemplate namedJdbc;
    private final JdbcTemplate jdbc;

    public DonHangRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public List<DonHangListItemResponse> timKiem(
            String siteNguon, String trangThaiDH, String trangThaiTT,
            LocalDateTime tuNgay, LocalDateTime denNgay,
            int page, int size
    ) {
        MapSqlParameterSource params = buildFilterParams(siteNguon, trangThaiDH, trangThaiTT, tuNgay, denNgay)
                .addValue("offset", (long) page * size)
                .addValue("size", size);
        return namedJdbc.query(SQL_LIST, params, listItemRowMapper());
    }

    public long demTong(
            String siteNguon, String trangThaiDH, String trangThaiTT,
            LocalDateTime tuNgay, LocalDateTime denNgay
    ) {
        MapSqlParameterSource params = buildFilterParams(siteNguon, trangThaiDH, trangThaiTT, tuNgay, denNgay);
        Long count = namedJdbc.queryForObject(SQL_COUNT, params, Long.class);
        return count == null ? 0L : count;
    }

    public Optional<DonHangDetailResponse> layChiTiet(String maDonHang, String siteTable) {
        String sql = """
                SELECT dh.MaDonHang, dh.MaND, dh.NgayDat, dh.HoTenNguoiNhan, dh.SoDienThoaiNhan,
                       dh.DiaChiGiao, dh.MaKhuVucXuLi, dh.TongTien, dh.PhuongThucTT,
                       dh.TrangThaiTT, dh.TrangThaiDH, dh.GhiChu
                FROM %s.[store_management].dbo.DonHang dh
                WHERE dh.MaDonHang = ?
                """.formatted(siteTable);

        List<ChiTietDonHangResponse> chiTiet = layChiTietSanPham(maDonHang, siteTable);
        String siteNguon = siteTable.replace("[", "").replace("]", "");

        List<DonHangDetailResponse> result = jdbc.query(sql, (rs, rowNum) -> new DonHangDetailResponse(
                siteNguon,
                rs.getString("MaDonHang"),
                rs.getString("MaND"),
                toLocalDateTime(rs.getObject("NgayDat")),
                rs.getString("HoTenNguoiNhan"),
                rs.getString("SoDienThoaiNhan"),
                rs.getString("DiaChiGiao"),
                rs.getString("MaKhuVucXuLi"),
                nonNullBigDecimal(rs.getBigDecimal("TongTien")),
                rs.getString("PhuongThucTT"),
                rs.getString("TrangThaiTT"),
                rs.getString("TrangThaiDH"),
                rs.getString("GhiChu"),
                chiTiet
        ), maDonHang);

        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    public List<ChiTietDonHangResponse> layChiTietSanPham(String maDonHang, String siteTable) {
        String sql = """
                SELECT ctdh.MaCTDH, ctdh.MaSP, sp.TenSP, ctdh.SoLuong, ctdh.DonGia, ctdh.ThanhTien
                FROM %s.[store_management].dbo.ChiTietDonHang ctdh
                JOIN %s.[store_management].dbo.SanPham_Core sp ON ctdh.MaSP = sp.MaSP
                WHERE ctdh.MaDonHang = ?
                """.formatted(siteTable, siteTable);

        return jdbc.query(sql, (rs, rowNum) -> new ChiTietDonHangResponse(
                rs.getString("MaCTDH"),
                rs.getString("MaSP"),
                rs.getString("TenSP"),
                rs.getInt("SoLuong"),
                nonNullBigDecimal(rs.getBigDecimal("DonGia")),
                nonNullBigDecimal(rs.getBigDecimal("ThanhTien"))
        ), maDonHang);
    }

    public int capNhatTrangThaiDH(String maDonHang, String siteTable, String trangThaiDH) {
        String sql = "UPDATE %s.[store_management].dbo.DonHang SET TrangThaiDH = ? WHERE MaDonHang = ?"
                .formatted(siteTable);
        return jdbc.update(sql, trangThaiDH, maDonHang);
    }

    public int capNhatTrangThaiTT(String maDonHang, String siteTable, String trangThaiTT) {
        String sql = "UPDATE %s.[store_management].dbo.DonHang SET TrangThaiTT = ? WHERE MaDonHang = ?"
                .formatted(siteTable);
        return jdbc.update(sql, trangThaiTT, maDonHang);
    }

    private MapSqlParameterSource buildFilterParams(
            String siteNguon, String trangThaiDH, String trangThaiTT,
            LocalDateTime tuNgay, LocalDateTime denNgay
    ) {
        return new MapSqlParameterSource()
                .addValue("siteNguon", siteNguon)
                .addValue("trangThaiDH", trangThaiDH)
                .addValue("trangThaiTT", trangThaiTT)
                .addValue("tuNgay", tuNgay)
                .addValue("denNgay", denNgay);
    }

    private RowMapper<DonHangListItemResponse> listItemRowMapper() {
        return (rs, rowNum) -> new DonHangListItemResponse(
                rs.getString("SiteNguon"),
                rs.getString("MaDonHang"),
                rs.getString("MaND"),
                toLocalDateTime(rs.getObject("NgayDat")),
                rs.getString("HoTenNguoiNhan"),
                rs.getString("SoDienThoaiNhan"),
                rs.getString("MaKhuVucXuLi"),
                nonNullBigDecimal(rs.getBigDecimal("TongTien")),
                rs.getString("PhuongThucTT"),
                rs.getString("TrangThaiTT"),
                rs.getString("TrangThaiDH")
        );
    }

    private BigDecimal nonNullBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
