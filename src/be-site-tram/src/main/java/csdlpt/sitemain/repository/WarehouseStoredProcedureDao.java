package csdlpt.sitemain.repository;

import csdlpt.sitemain.dto.response.WarehouseActionResponse;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WarehouseStoredProcedureDao {

    private final JdbcTemplate jdbcTemplate;

    public WarehouseStoredProcedureDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public WarehouseActionResponse xacNhanXuatNoiBo(UUID maPhieuXuat) {
        CallableStatementCreator creator = connection -> {
            CallableStatement statement = connection.prepareCall("{call dbo.sp_XacNhanXuat_NoiBo(?)}");
            statement.setObject(1, maPhieuXuat);
            return statement;
        };
        CallableStatementCallback<WarehouseActionResponse> callback = statement -> {
            statement.execute();
            return readActionResult(statement, "exported");
        };
        return jdbcTemplate.execute(creator, callback);
    }

    public WarehouseActionResponse xacNhanNhapNoiBo(UUID maPhieuNhap, UUID maNhanVienNhap) {
        CallableStatementCreator creator = connection -> {
            CallableStatement statement = connection.prepareCall("{call dbo.sp_XacNhanNhap_NoiBo(?, ?)}");
            statement.setObject(1, maPhieuNhap);
            statement.setObject(2, maNhanVienNhap);
            return statement;
        };
        CallableStatementCallback<WarehouseActionResponse> callback = statement -> {
            statement.execute();
            return readActionResult(statement, "imported");
        };
        return jdbcTemplate.execute(creator, callback);
    }

    public WarehouseActionResponse taoPhieuXuatGiaoKhach(UUID maDonHang, String maKhoXuat) {
        CallableStatementCreator creator = connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call dbo.sp_TaoPhieuXuat_GiaoKhach_KhiDuHang(?, ?, ?)}");
            statement.setObject(1, maDonHang);
            statement.setString(2, maKhoXuat);
            statement.registerOutParameter(3, microsoft.sql.Types.GUID);
            return statement;
        };
        CallableStatementCallback<WarehouseActionResponse> callback = statement -> {
            statement.execute();
            WarehouseActionResponse result = readActionResult(statement, "waiting_export");
            UUID maPhieuXuat = result.maPhieuXuat();
            if (maPhieuXuat == null) {
                maPhieuXuat = toUuid(statement.getObject(3));
            }
            return new WarehouseActionResponse(
                    maPhieuXuat,
                    null,
                    maDonHang,
                    maKhoXuat,
                    null,
                    "waiting_export",
                    result.message()
            );
        };
        return jdbcTemplate.execute(creator, callback);
    }

    public WarehouseActionResponse xacNhanXuatGiaoKhach(UUID maPhieuXuat) {
        CallableStatementCreator creator = connection -> {
            CallableStatement statement = connection.prepareCall("{call dbo.sp_XacNhanXuat_GiaoKhach(?)}");
            statement.setObject(1, maPhieuXuat);
            return statement;
        };
        CallableStatementCallback<WarehouseActionResponse> callback = statement -> {
            statement.execute();
            return readActionResult(statement, "exported");
        };
        return jdbcTemplate.execute(creator, callback);
    }

    private WarehouseActionResponse readActionResult(CallableStatement statement, String trangThaiMoi)
            throws SQLException {
        ResultSet rs = statement.getResultSet();
        if (rs != null && rs.next()) {
            return new WarehouseActionResponse(
                    getUuidIfPresent(rs, "MaPhieuXuat"),
                    getUuidIfPresent(rs, "MaPhieuNhap"),
                    getUuidIfPresent(rs, "MaDonHang"),
                    getStringIfPresent(rs, "MaKhoXuat"),
                    getStringIfPresent(rs, "MaKhoNhap"),
                    trangThaiMoi,
                    getStringIfPresent(rs, "ThongBao")
            );
        }
        return new WarehouseActionResponse(
                null,
                null,
                null,
                null,
                null,
                trangThaiMoi,
                "Thao tac kho thanh cong"
        );
    }

    private UUID getUuidIfPresent(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return toUuid(rs.getObject(column));
    }

    private String getStringIfPresent(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return rs.getString(column);
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        int count = rs.getMetaData().getColumnCount();
        for (int index = 1; index <= count; index++) {
            if (column.equalsIgnoreCase(rs.getMetaData().getColumnLabel(index))) {
                return true;
            }
        }
        return false;
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof byte[] bytes) {
            return UUID.nameUUIDFromBytes(bytes);
        }
        return UUID.fromString(value.toString());
    }
}
