package csdlpt.sitemain.repository;

import csdlpt.sitemain.dto.response.ChiTietNhapResponse;
import csdlpt.sitemain.dto.response.ChiTietXuatResponse;
import csdlpt.sitemain.dto.response.PhieuNhapDetailResponse;
import csdlpt.sitemain.dto.response.PhieuNhapSummaryResponse;
import csdlpt.sitemain.dto.response.PhieuXuatDetailResponse;
import csdlpt.sitemain.dto.response.PhieuXuatSummaryResponse;
import csdlpt.sitemain.dto.response.ReadyToShipOrderResponse;
import csdlpt.sitemain.dto.response.WarehouseContextResponse;
import csdlpt.sitemain.dto.response.WarehouseDashboardResponse;
import csdlpt.sitemain.dto.response.WarehouseTonKhoResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WarehouseQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public WarehouseQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<WarehouseContextResponse> findWarehouseContext(UUID maNhanVien) {
        String sql = """
                SELECT
                    nd.MaND,
                    nd.HoTen,
                    nd.VaiTro,
                    nd.MaKhoPhuTrach,
                    k.TenKho,
                    COALESCE(k.MaKhuVuc, nd.MaKV) AS MaKV
                FROM NguoiDung nd
                LEFT JOIN Kho k
                    ON k.MaKho = nd.MaKhoPhuTrach
                WHERE nd.MaND = :maNhanVien
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maNhanVien", maNhanVien);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new WarehouseContextResponse(
                getUuid(rs, "MaND"),
                rs.getString("HoTen"),
                rs.getString("VaiTro"),
                rs.getString("MaKhoPhuTrach"),
                rs.getString("TenKho"),
                rs.getString("MaKV")
        )).stream().findFirst();
    }

    public WarehouseDashboardResponse getDashboard(
            String maKho,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho);

        long waitingExportInternal = count("""
                SELECT COUNT(*)
                FROM PhieuXuatKho px
                WHERE px.MaKhoXuat = :maKho
                  AND px.MaKhoNhan IS NOT NULL
                  AND px.TrangThaiXuat = 'waiting_export'
                """, params);

        long waitingImportInternal = count("""
                SELECT COUNT(*)
                FROM PhieuNhapKho pn
                WHERE pn.MaKhoNhap = :maKho
                  AND pn.TrangThaiNhap = 'waiting_import'
                """, params);

        long readyToShipOrders = countReadyToShip(maKho, null, fromDate, toDate);

        long waitingCustomerExport = count("""
                SELECT COUNT(*)
                FROM PhieuXuatKho px
                WHERE px.MaKhoXuat = :maKho
                  AND px.MaKhoNhan IS NULL
                  AND px.TrangThaiXuat = 'waiting_export'
                """, params);

        long lowStockProducts = count("""
                SELECT COUNT(*)
                FROM TonKho tk
                WHERE tk.MaKho = :maKho
                  AND (tk.SoLuongTon - tk.SoLuongDatHang) <= 5
                """, params);

        return new WarehouseDashboardResponse(
                maKho,
                waitingExportInternal,
                waitingImportInternal,
                readyToShipOrders,
                waitingCustomerExport,
                lowStockProducts
        );
    }

    public Page<PhieuXuatSummaryResponse> findPhieuXuat(
            String maKho,
            String loai,
            String trangThaiXuat,
            String trangThaiNhan,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho);
        StringBuilder where = new StringBuilder(" WHERE px.MaKhoXuat = :maKho ");
        applyPhieuXuatFilters(where, params, loai, trangThaiXuat, trangThaiNhan, maDonHang, fromDate, toDate);

        long total = count("SELECT COUNT(*) FROM PhieuXuatKho px " + where, params);
        addPaging(params, pageable);

        String sql = """
                SELECT
                    px.MaPhieuXuat,
                    px.MaDonHang,
                    px.MaKhoXuat,
                    kx.TenKho AS TenKhoXuat,
                    px.MaKhoNhan,
                    kn.TenKho AS TenKhoNhan,
                    CASE WHEN px.MaKhoNhan IS NULL THEN 'giao_khach' ELSE 'noi_bo' END AS LoaiPhieu,
                    px.TrangThaiXuat,
                    px.TrangThaiNhan,
                    px.NgayTao,
                    COUNT(ctxk.MaCTXK) AS SoDongHang,
                    COALESCE(SUM(ctxk.SoLuongXuat), 0) AS TongSoLuong
                FROM PhieuXuatKho px
                INNER JOIN Kho kx
                    ON kx.MaKho = px.MaKhoXuat
                LEFT JOIN Kho kn
                    ON kn.MaKho = px.MaKhoNhan
                LEFT JOIN ChiTietXuatKho ctxk
                    ON ctxk.MaPhieuXuat = px.MaPhieuXuat
                """
                + where + """
                GROUP BY
                    px.MaPhieuXuat,
                    px.MaDonHang,
                    px.MaKhoXuat,
                    kx.TenKho,
                    px.MaKhoNhan,
                    kn.TenKho,
                    px.TrangThaiXuat,
                    px.TrangThaiNhan,
                    px.NgayTao
                ORDER BY px.NgayTao DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;

        List<PhieuXuatSummaryResponse> items = jdbcTemplate.query(sql, params, this::mapPhieuXuatSummary);
        return new PageImpl<>(items, pageable, total);
    }

    public Optional<PhieuXuatDetailResponse> findPhieuXuatDetail(String maKho, UUID maPhieuXuat) {
        String headerSql = """
                SELECT
                    px.MaPhieuXuat,
                    px.MaDonHang,
                    px.MaKhoXuat,
                    kx.TenKho AS TenKhoXuat,
                    px.MaKhoNhan,
                    kn.TenKho AS TenKhoNhan,
                    CASE WHEN px.MaKhoNhan IS NULL THEN 'giao_khach' ELSE 'noi_bo' END AS LoaiPhieu,
                    px.TrangThaiXuat,
                    px.TrangThaiNhan,
                    px.NgayTao
                FROM PhieuXuatKho px
                INNER JOIN Kho kx
                    ON kx.MaKho = px.MaKhoXuat
                LEFT JOIN Kho kn
                    ON kn.MaKho = px.MaKhoNhan
                WHERE px.MaPhieuXuat = :maPhieuXuat
                  AND px.MaKhoXuat = :maKho
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho)
                .addValue("maPhieuXuat", maPhieuXuat);

        return jdbcTemplate.query(headerSql, params, (rs, rowNum) -> {
            List<ChiTietXuatResponse> items = findChiTietXuat(maPhieuXuat);
            return new PhieuXuatDetailResponse(
                    getUuid(rs, "MaPhieuXuat"),
                    getUuid(rs, "MaDonHang"),
                    rs.getString("MaKhoXuat"),
                    rs.getString("TenKhoXuat"),
                    rs.getString("MaKhoNhan"),
                    rs.getString("TenKhoNhan"),
                    rs.getString("LoaiPhieu"),
                    rs.getString("TrangThaiXuat"),
                    rs.getString("TrangThaiNhan"),
                    toLocalDateTime(rs, "NgayTao"),
                    items
            );
        }).stream().findFirst();
    }

    public Page<PhieuNhapSummaryResponse> findPhieuNhap(
            String maKho,
            String trangThaiNhap,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho);
        StringBuilder where = new StringBuilder(" WHERE pn.MaKhoNhap = :maKho ");
        applyPhieuNhapFilters(where, params, trangThaiNhap, maDonHang, fromDate, toDate);

        long total = count("SELECT COUNT(*) FROM PhieuNhapKho pn " + where, params);
        addPaging(params, pageable);

        String sql = """
                SELECT
                    pn.MaPhieuNhap,
                    pn.MaDonHang,
                    pn.MaKhoXuat,
                    kx.TenKho AS TenKhoXuat,
                    pn.MaKhoNhap,
                    kn.TenKho AS TenKhoNhap,
                    pn.TrangThaiNhap,
                    pn.NgayNhap,
                    COUNT(ctpn.MaCTPN) AS SoDongHang,
                    COALESCE(SUM(ctpn.SoLuong), 0) AS TongSoLuong,
                    COALESCE(src.TrangThaiXuat, CASE WHEN kx.MaKho IS NULL THEN 'remote' END) AS SourceExportStatus
                FROM PhieuNhapKho pn
                LEFT JOIN Kho kx
                    ON kx.MaKho = pn.MaKhoXuat
                INNER JOIN Kho kn
                    ON kn.MaKho = pn.MaKhoNhap
                LEFT JOIN ChiTietPhieuNhap ctpn
                    ON ctpn.MaPhieuNhap = pn.MaPhieuNhap
                OUTER APPLY (
                    SELECT TOP 1 px.TrangThaiXuat
                    FROM PhieuXuatKho px
                    WHERE px.MaDonHang = pn.MaDonHang
                      AND px.MaKhoXuat = pn.MaKhoXuat
                      AND px.MaKhoNhan = pn.MaKhoNhap
                    ORDER BY px.NgayTao DESC
                ) src
                """
                + where + """
                GROUP BY
                    pn.MaPhieuNhap,
                    pn.MaDonHang,
                    pn.MaKhoXuat,
                    kx.MaKho,
                    kx.TenKho,
                    pn.MaKhoNhap,
                    kn.TenKho,
                    pn.TrangThaiNhap,
                    pn.NgayNhap,
                    src.TrangThaiXuat
                ORDER BY pn.NgayNhap DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;

        List<PhieuNhapSummaryResponse> items = jdbcTemplate.query(sql, params, this::mapPhieuNhapSummary);
        return new PageImpl<>(items, pageable, total);
    }

    public Optional<PhieuNhapDetailResponse> findPhieuNhapDetail(String maKho, UUID maPhieuNhap) {
        String headerSql = """
                SELECT
                    pn.MaPhieuNhap,
                    pn.MaDonHang,
                    pn.MaKhoXuat,
                    kx.TenKho AS TenKhoXuat,
                    pn.MaKhoNhap,
                    kn.TenKho AS TenKhoNhap,
                    pn.TrangThaiNhap,
                    pn.NgayNhap,
                    pn.MaNhanVienNhap,
                    COALESCE(src.TrangThaiXuat, CASE WHEN kx.MaKho IS NULL THEN 'remote' END) AS SourceExportStatus
                FROM PhieuNhapKho pn
                LEFT JOIN Kho kx
                    ON kx.MaKho = pn.MaKhoXuat
                INNER JOIN Kho kn
                    ON kn.MaKho = pn.MaKhoNhap
                OUTER APPLY (
                    SELECT TOP 1 px.TrangThaiXuat
                    FROM PhieuXuatKho px
                    WHERE px.MaDonHang = pn.MaDonHang
                      AND px.MaKhoXuat = pn.MaKhoXuat
                      AND px.MaKhoNhan = pn.MaKhoNhap
                    ORDER BY px.NgayTao DESC
                ) src
                WHERE pn.MaPhieuNhap = :maPhieuNhap
                  AND pn.MaKhoNhap = :maKho
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho)
                .addValue("maPhieuNhap", maPhieuNhap);

        return jdbcTemplate.query(headerSql, params, (rs, rowNum) -> {
            List<ChiTietNhapResponse> items = findChiTietNhap(maPhieuNhap);
            return new PhieuNhapDetailResponse(
                    getUuid(rs, "MaPhieuNhap"),
                    getUuid(rs, "MaDonHang"),
                    rs.getString("MaKhoXuat"),
                    rs.getString("TenKhoXuat"),
                    rs.getString("MaKhoNhap"),
                    rs.getString("TenKhoNhap"),
                    rs.getString("TrangThaiNhap"),
                    toLocalDateTime(rs, "NgayNhap"),
                    getUuid(rs, "MaNhanVienNhap"),
                    rs.getString("SourceExportStatus"),
                    items
            );
        }).stream().findFirst();
    }

    public Page<ReadyToShipOrderResponse> findReadyToShipOrders(
            String maKho,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho);
        String where = buildReadyToShipWhere(params, maDonHang, fromDate, toDate);
        long total = count("SELECT COUNT(*) FROM DonHang dh " + where, params);
        addPaging(params, pageable);

        String sql = """
                SELECT
                    dh.MaDonHang,
                    dh.NgayDat,
                    dh.TrangThaiDH,
                    k.MaKho AS MaKhoXuat,
                    k.TenKho AS TenKhoXuat,
                    pxg.MaPhieuXuat AS MaPhieuXuatGiaoKhach,
                    COUNT(ctdh.MaCTDH) AS SoDongHang,
                    COALESCE(SUM(ctdh.SoLuong), 0) AS TongSoLuong
                FROM DonHang dh
                INNER JOIN ChiTietDonHang ctdh
                    ON ctdh.MaDonHang = dh.MaDonHang
                INNER JOIN Kho k
                    ON k.MaKho = :maKho
                OUTER APPLY (
                    SELECT TOP 1 px.MaPhieuXuat
                    FROM PhieuXuatKho px
                    WHERE px.MaDonHang = dh.MaDonHang
                      AND px.MaKhoXuat = :maKho
                      AND px.MaKhoNhan IS NULL
                      AND px.TrangThaiXuat <> 'cancelled'
                    ORDER BY px.NgayTao DESC
                ) pxg
                """
                + where + """
                GROUP BY
                    dh.MaDonHang,
                    dh.NgayDat,
                    dh.TrangThaiDH,
                    k.MaKho,
                    k.TenKho,
                    pxg.MaPhieuXuat
                ORDER BY dh.NgayDat DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;

        List<ReadyToShipOrderResponse> items = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            UUID maPhieuXuat = getUuid(rs, "MaPhieuXuatGiaoKhach");
            return new ReadyToShipOrderResponse(
                    getUuid(rs, "MaDonHang"),
                    toLocalDateTime(rs, "NgayDat"),
                    rs.getString("TrangThaiDH"),
                    rs.getString("MaKhoXuat"),
                    rs.getString("TenKhoXuat"),
                    maPhieuXuat != null,
                    maPhieuXuat,
                    rs.getLong("SoDongHang"),
                    rs.getLong("TongSoLuong")
            );
        });
        return new PageImpl<>(items, pageable, total);
    }

    public boolean isReadyToShip(String maKho, UUID maDonHang) {
        return countReadyToShip(maKho, maDonHang, null, null) > 0;
    }

    public Page<WarehouseTonKhoResponse> findTonKho(
            String maKho,
            String q,
            Boolean onlyReserved,
            Boolean onlyLowStock,
            Pageable pageable
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho);
        StringBuilder where = new StringBuilder(" WHERE tk.MaKho = :maKho ");
        if (hasText(q)) {
            where.append(" AND (tk.MaSP LIKE :q OR sp.TenSP LIKE :q) ");
            params.addValue("q", "%" + q.trim() + "%");
        }
        if (Boolean.TRUE.equals(onlyReserved)) {
            where.append(" AND tk.SoLuongDatHang > 0 ");
        }
        if (Boolean.TRUE.equals(onlyLowStock)) {
            where.append(" AND (tk.SoLuongTon - tk.SoLuongDatHang) <= 5 ");
        }

        long total = count("""
                SELECT COUNT(*)
                FROM TonKho tk
                INNER JOIN SanPham_Core sp
                    ON sp.MaSP = tk.MaSP
                """
                + where, params);
        addPaging(params, pageable);

        String sql = """
                SELECT
                    tk.MaKho,
                    tk.MaSP,
                    sp.TenSP,
                    tk.SoLuongTon,
                    tk.SoLuongDatHang,
                    (tk.SoLuongTon - tk.SoLuongDatHang) AS SoLuongKhaDung,
                    tk.NgayCapNhat
                FROM TonKho tk
                INNER JOIN SanPham_Core sp
                    ON sp.MaSP = tk.MaSP
                """
                + where + """
                ORDER BY sp.TenSP ASC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;

        List<WarehouseTonKhoResponse> items = jdbcTemplate.query(sql, params, (rs, rowNum) -> new WarehouseTonKhoResponse(
                rs.getString("MaKho"),
                rs.getString("MaSP"),
                rs.getString("TenSP"),
                rs.getInt("SoLuongTon"),
                rs.getInt("SoLuongDatHang"),
                rs.getInt("SoLuongKhaDung"),
                toLocalDateTime(rs, "NgayCapNhat")
        ));
        return new PageImpl<>(items, pageable, total);
    }

    private List<ChiTietXuatResponse> findChiTietXuat(UUID maPhieuXuat) {
        String sql = """
                SELECT
                    ctxk.MaCTXK,
                    ctxk.MaCTDH,
                    ctxk.MaSP,
                    sp.TenSP,
                    ctxk.SoLuongXuat,
                    tk.SoLuongTon,
                    tk.SoLuongDatHang
                FROM ChiTietXuatKho ctxk
                INNER JOIN PhieuXuatKho px
                    ON px.MaPhieuXuat = ctxk.MaPhieuXuat
                INNER JOIN SanPham_Core sp
                    ON sp.MaSP = ctxk.MaSP
                LEFT JOIN TonKho tk
                    ON tk.MaKho = px.MaKhoXuat
                   AND tk.MaSP = ctxk.MaSP
                WHERE ctxk.MaPhieuXuat = :maPhieuXuat
                ORDER BY sp.TenSP ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maPhieuXuat", maPhieuXuat);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ChiTietXuatResponse(
                getUuid(rs, "MaCTXK"),
                getUuid(rs, "MaCTDH"),
                rs.getString("MaSP"),
                rs.getString("TenSP"),
                rs.getInt("SoLuongXuat"),
                getNullableInt(rs, "SoLuongTon"),
                getNullableInt(rs, "SoLuongDatHang")
        ));
    }

    private List<ChiTietNhapResponse> findChiTietNhap(UUID maPhieuNhap) {
        String sql = """
                SELECT
                    ctpn.MaCTPN,
                    ctpn.MaSP,
                    sp.TenSP,
                    ctpn.SoLuong,
                    ctpn.DonGiaNhap
                FROM ChiTietPhieuNhap ctpn
                INNER JOIN SanPham_Core sp
                    ON sp.MaSP = ctpn.MaSP
                WHERE ctpn.MaPhieuNhap = :maPhieuNhap
                ORDER BY sp.TenSP ASC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maPhieuNhap", maPhieuNhap);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ChiTietNhapResponse(
                getUuid(rs, "MaCTPN"),
                rs.getString("MaSP"),
                rs.getString("TenSP"),
                rs.getInt("SoLuong"),
                rs.getBigDecimal("DonGiaNhap")
        ));
    }

    private PhieuXuatSummaryResponse mapPhieuXuatSummary(ResultSet rs, int rowNum) throws SQLException {
        return new PhieuXuatSummaryResponse(
                getUuid(rs, "MaPhieuXuat"),
                getUuid(rs, "MaDonHang"),
                rs.getString("MaKhoXuat"),
                rs.getString("TenKhoXuat"),
                rs.getString("MaKhoNhan"),
                rs.getString("TenKhoNhan"),
                rs.getString("LoaiPhieu"),
                rs.getString("TrangThaiXuat"),
                rs.getString("TrangThaiNhan"),
                toLocalDateTime(rs, "NgayTao"),
                rs.getLong("SoDongHang"),
                rs.getLong("TongSoLuong")
        );
    }

    private PhieuNhapSummaryResponse mapPhieuNhapSummary(ResultSet rs, int rowNum) throws SQLException {
        return new PhieuNhapSummaryResponse(
                getUuid(rs, "MaPhieuNhap"),
                getUuid(rs, "MaDonHang"),
                rs.getString("MaKhoXuat"),
                rs.getString("TenKhoXuat"),
                rs.getString("MaKhoNhap"),
                rs.getString("TenKhoNhap"),
                rs.getString("TrangThaiNhap"),
                toLocalDateTime(rs, "NgayNhap"),
                rs.getLong("SoDongHang"),
                rs.getLong("TongSoLuong"),
                rs.getString("SourceExportStatus")
        );
    }

    private void applyPhieuXuatFilters(
            StringBuilder where,
            MapSqlParameterSource params,
            String loai,
            String trangThaiXuat,
            String trangThaiNhan,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        if ("noi_bo".equalsIgnoreCase(loai)) {
            where.append(" AND px.MaKhoNhan IS NOT NULL ");
        } else if ("giao_khach".equalsIgnoreCase(loai)) {
            where.append(" AND px.MaKhoNhan IS NULL ");
        }
        if (hasText(trangThaiXuat)) {
            where.append(" AND px.TrangThaiXuat = :trangThaiXuat ");
            params.addValue("trangThaiXuat", trangThaiXuat.trim());
        }
        if (hasText(trangThaiNhan)) {
            where.append(" AND px.TrangThaiNhan = :trangThaiNhan ");
            params.addValue("trangThaiNhan", trangThaiNhan.trim());
        }
        if (maDonHang != null) {
            where.append(" AND px.MaDonHang = :maDonHang ");
            params.addValue("maDonHang", maDonHang);
        }
        addDateFilter(where, params, "px.NgayTao", fromDate, toDate);
    }

    private void applyPhieuNhapFilters(
            StringBuilder where,
            MapSqlParameterSource params,
            String trangThaiNhap,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        if (hasText(trangThaiNhap)) {
            where.append(" AND pn.TrangThaiNhap = :trangThaiNhap ");
            params.addValue("trangThaiNhap", trangThaiNhap.trim());
        }
        if (maDonHang != null) {
            where.append(" AND pn.MaDonHang = :maDonHang ");
            params.addValue("maDonHang", maDonHang);
        }
        addDateFilter(where, params, "pn.NgayNhap", fromDate, toDate);
    }

    private String buildReadyToShipWhere(
            MapSqlParameterSource params,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        StringBuilder where = new StringBuilder("""
                WHERE dh.TrangThaiDH = 'processing'
                  AND NOT EXISTS (
                        SELECT 1
                        FROM PhieuNhapKho pn
                        WHERE pn.MaDonHang = dh.MaDonHang
                          AND pn.TrangThaiNhap <> 'imported'
                  )
                  AND NOT EXISTS (
                        SELECT 1
                        FROM ChiTietDonHang ctdh2
                        LEFT JOIN TonKho tk
                            ON tk.MaKho = :maKho
                           AND tk.MaSP = ctdh2.MaSP
                        WHERE ctdh2.MaDonHang = dh.MaDonHang
                          AND (
                                tk.MaSP IS NULL
                                OR tk.SoLuongTon < ctdh2.SoLuong
                                OR tk.SoLuongDatHang < ctdh2.SoLuong
                          )
                  )
                """);
        if (maDonHang != null) {
            where.append(" AND dh.MaDonHang = :maDonHang ");
            params.addValue("maDonHang", maDonHang);
        }
        addDateFilter(where, params, "dh.NgayDat", fromDate, toDate);
        return where.toString();
    }

    private long countReadyToShip(
            String maKho,
            UUID maDonHang,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maKho", maKho);
        String where = buildReadyToShipWhere(params, maDonHang, fromDate, toDate);
        return count("SELECT COUNT(*) FROM DonHang dh " + where, params);
    }

    private void addDateFilter(
            StringBuilder where,
            MapSqlParameterSource params,
            String column,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        if (fromDate != null) {
            where.append(" AND ").append(column).append(" >= :fromDate ");
            params.addValue("fromDate", fromDate);
        }
        if (toDate != null) {
            where.append(" AND ").append(column).append(" <= :toDate ");
            params.addValue("toDate", toDate);
        }
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long result = jdbcTemplate.queryForObject(sql, params, Long.class);
        return result == null ? 0L : result;
    }

    private void addPaging(MapSqlParameterSource params, Pageable pageable) {
        params.addValue("offset", pageable.getOffset());
        params.addValue("limit", pageable.getPageSize());
    }

    private static UUID getUuid(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
