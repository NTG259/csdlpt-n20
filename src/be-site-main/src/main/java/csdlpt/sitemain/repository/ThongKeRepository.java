package csdlpt.sitemain.repository;

import csdlpt.sitemain.dto.request.ThongKeDoanhThuFilter;
import csdlpt.sitemain.dto.response.DoanhThuTheoKhoResponse;
import csdlpt.sitemain.dto.response.DoanhThuTheoThangResponse;
import csdlpt.sitemain.dto.response.DoanhThuTheoVungResponse;
import csdlpt.sitemain.dto.response.DoanhThuToanHeThongResponse;
import csdlpt.sitemain.dto.response.DonHangNhieuKhoResponse;
import csdlpt.sitemain.dto.response.SanPhamBanChayResponse;
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

    private static final String SQL_DOANH_THU_THEO_THANG = """
            WITH DoanhThuTheoKho AS (
                SELECT
                    'SITE_BAC' AS SiteNguon,
                    YEAR(dh.NgayDat) AS Nam,
                    MONTH(dh.NgayDat) AS Thang,
                    pxk.MaKhoXuat AS MaKho,
                    k.TenKho,
                    SUM(ctx.SoLuongXuat * ctdh.DonGia) AS DoanhThu
                FROM [SITE_BAC].[store_management].dbo.PhieuXuatKho pxk
                JOIN [SITE_BAC].[store_management].dbo.ChiTietXuatKho ctx ON pxk.MaPhieuXuat = ctx.MaPhieuXuat
                JOIN [SITE_BAC].[store_management].dbo.Kho k ON pxk.MaKhoXuat = k.MaKho
                JOIN [SITE_BAC].[store_management].dbo.DonHang dh ON pxk.MaDonHang = dh.MaDonHang
                JOIN [SITE_BAC].[store_management].dbo.ChiTietDonHang ctdh ON ctx.MaCTDH = ctdh.MaCTDH
                WHERE dh.TrangThaiTT = 'paid' AND dh.TrangThaiDH = 'completed' AND pxk.TrangThaiXuat = 'exported'
                GROUP BY YEAR(dh.NgayDat), MONTH(dh.NgayDat), pxk.MaKhoXuat, k.TenKho

                UNION ALL

                SELECT
                    'SITE_NAM' AS SiteNguon,
                    YEAR(dh.NgayDat) AS Nam,
                    MONTH(dh.NgayDat) AS Thang,
                    pxk.MaKhoXuat AS MaKho,
                    k.TenKho,
                    SUM(ctx.SoLuongXuat * ctdh.DonGia) AS DoanhThu
                FROM [SITE_NAM].[store_management].dbo.PhieuXuatKho pxk
                JOIN [SITE_NAM].[store_management].dbo.ChiTietXuatKho ctx ON pxk.MaPhieuXuat = ctx.MaPhieuXuat
                JOIN [SITE_NAM].[store_management].dbo.Kho k ON pxk.MaKhoXuat = k.MaKho
                JOIN [SITE_NAM].[store_management].dbo.DonHang dh ON pxk.MaDonHang = dh.MaDonHang
                JOIN [SITE_NAM].[store_management].dbo.ChiTietDonHang ctdh ON ctx.MaCTDH = ctdh.MaCTDH
                WHERE dh.TrangThaiTT = 'paid' AND dh.TrangThaiDH = 'completed' AND pxk.TrangThaiXuat = 'exported'
                GROUP BY YEAR(dh.NgayDat), MONTH(dh.NgayDat), pxk.MaKhoXuat, k.TenKho
            )
            SELECT SiteNguon, Nam, Thang, MaKho, TenKho, SUM(DoanhThu) AS DoanhThu
            FROM DoanhThuTheoKho
            GROUP BY SiteNguon, Nam, Thang, MaKho, TenKho

            UNION ALL

            SELECT 'TOAN_HE_THONG' AS SiteNguon, Nam, Thang, NULL AS MaKho, N'Tất cả kho' AS TenKho, SUM(DoanhThu) AS DoanhThu
            FROM DoanhThuTheoKho
            GROUP BY Nam, Thang

            ORDER BY Nam, Thang, SiteNguon, MaKho
            """;

    private static final String SQL_TOP_SAN_PHAM_BAN_CHAY = """
            SELECT TOP 10
                MaSP,
                MAX(TenSP) AS TenSP,
                SUM(SoLuongBan) AS TongSoLuongBan,
                SUM(DoanhThu) AS TongDoanhThu
            FROM (
                SELECT
                    ctdh.MaSP,
                    sp.TenSP,
                    ctdh.SoLuong AS SoLuongBan,
                    ctdh.SoLuong * ctdh.DonGia AS DoanhThu
                FROM [SITE_BAC].[store_management].dbo.DonHang dh
                JOIN [SITE_BAC].[store_management].dbo.ChiTietDonHang ctdh ON dh.MaDonHang = ctdh.MaDonHang
                JOIN [SITE_BAC].[store_management].dbo.SanPham_Core sp ON ctdh.MaSP = sp.MaSP
                WHERE dh.TrangThaiTT = 'paid' AND dh.TrangThaiDH = 'completed'

                UNION ALL

                SELECT
                    ctdh.MaSP,
                    sp.TenSP,
                    ctdh.SoLuong AS SoLuongBan,
                    ctdh.SoLuong * ctdh.DonGia AS DoanhThu
                FROM [SITE_NAM].[store_management].dbo.DonHang dh
                JOIN [SITE_NAM].[store_management].dbo.ChiTietDonHang ctdh ON dh.MaDonHang = ctdh.MaDonHang
                JOIN [SITE_NAM].[store_management].dbo.SanPham_Core sp ON ctdh.MaSP = sp.MaSP
                WHERE dh.TrangThaiTT = 'paid' AND dh.TrangThaiDH = 'completed'
            ) AS T
            GROUP BY MaSP
            ORDER BY TongSoLuongBan DESC, TongDoanhThu DESC
            """;

    private static final String SQL_DON_HANG_NHIEU_KHO = """
            WITH TatCaPhieuXuat AS (
                SELECT 'SITE_BAC' AS SiteNguon, pxk.MaDonHang, pxk.MaKhoXuat
                FROM [SITE_BAC].[store_management].dbo.PhieuXuatKho pxk
                WHERE pxk.TrangThaiXuat = 'exported'

                UNION ALL

                SELECT 'SITE_NAM' AS SiteNguon, pxk.MaDonHang, pxk.MaKhoXuat
                FROM [SITE_NAM].[store_management].dbo.PhieuXuatKho pxk
                WHERE pxk.TrangThaiXuat = 'exported'
            ),
            KhoXuatKhongTrung AS (
                SELECT DISTINCT MaDonHang, MaKhoXuat
                FROM TatCaPhieuXuat
            )
            SELECT
                MaDonHang,
                COUNT(*) AS SoKhoXuat,
                STRING_AGG(MaKhoXuat, ', ') AS DanhSachKhoXuat
            FROM KhoXuatKhongTrung
            GROUP BY MaDonHang
            HAVING COUNT(*) > 1
            ORDER BY SoKhoXuat DESC, MaDonHang
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall thongKeDoanhThuCall;

    public ThongKeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

    public List<DoanhThuTheoThangResponse> thongKeDoanhThuTheoThang() {
        return jdbcTemplate.query(
                SQL_DOANH_THU_THEO_THANG,
                (rs, rowNum) -> new DoanhThuTheoThangResponse(
                        rs.getString("SiteNguon"),
                        rs.getInt("Nam"),
                        rs.getInt("Thang"),
                        rs.getString("MaKho"),
                        rs.getString("TenKho"),
                        nonNullBigDecimal(rs.getBigDecimal("DoanhThu"))
                )
        );
    }

    public List<SanPhamBanChayResponse> topSanPhamBanChay() {
        return jdbcTemplate.query(
                SQL_TOP_SAN_PHAM_BAN_CHAY,
                (rs, rowNum) -> new SanPhamBanChayResponse(
                        rs.getString("MaSP"),
                        rs.getString("TenSP"),
                        rs.getInt("TongSoLuongBan"),
                        nonNullBigDecimal(rs.getBigDecimal("TongDoanhThu"))
                )
        );
    }

    public List<DonHangNhieuKhoResponse> donHangXuatNhieuKho() {
        return jdbcTemplate.query(
                SQL_DON_HANG_NHIEU_KHO,
                (rs, rowNum) -> new DonHangNhieuKhoResponse(
                        rs.getString("MaDonHang"),
                        rs.getInt("SoKhoXuat"),
                        rs.getString("DanhSachKhoXuat")
                )
        );
    }
}
