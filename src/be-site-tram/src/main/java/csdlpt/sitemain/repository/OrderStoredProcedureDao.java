package csdlpt.sitemain.repository;

import com.microsoft.sqlserver.jdbc.SQLServerCallableStatement;
import com.microsoft.sqlserver.jdbc.SQLServerDataTable;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import csdlpt.sitemain.domain.entity.ChiTietGioHang;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderStoredProcedureDao {

    private static final String ORDER_ITEM_TYPE = "dbo.OrderItemType";

    private final JdbcTemplate jdbcTemplate;

    public OrderStoredProcedureDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String chonKhoNhanToiUu(String maKhuVucXuLi, List<ChiTietGioHang> items) {
        CallableStatementCreator creator = connection -> {
            CallableStatement statement = connection.prepareCall("{call dbo.sp_ChonKhoNhan_ToiUu(?, ?, ?)}");
            SQLServerCallableStatement sqlServerStatement = statement.unwrap(SQLServerCallableStatement.class);
            statement.setString(1, maKhuVucXuLi);
            sqlServerStatement.setStructured(2, ORDER_ITEM_TYPE, buildItemsTvp(items));
            statement.registerOutParameter(3, Types.VARCHAR);
            return statement;
        };
        CallableStatementCallback<String> callback = statement -> {
            statement.execute();
            return statement.getString(3);
        };
        return jdbcTemplate.execute(creator, callback);
    }

    public UUID datHang(DatHangParams params, List<ChiTietGioHang> items) {
        CallableStatementCreator creator = connection -> {
            CallableStatement statement = connection.prepareCall(
                    "{call dbo.sp_DatHang_TuSiteBac_ModelB(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
            SQLServerCallableStatement sqlServerStatement = statement.unwrap(SQLServerCallableStatement.class);

            statement.setObject(1, params.maND());
            statement.setString(2, params.hoTenNguoiNhan());
            statement.setString(3, params.soDienThoaiNhan());
            statement.setString(4, params.diaChiGiao());
            statement.setString(5, params.maKhuVucXuLi());
            statement.setString(6, params.maKhoNhan());
            statement.setNull(7, Types.VARCHAR);
            statement.setString(8, params.phuongThucTT());
            sqlServerStatement.setStructured(9, ORDER_ITEM_TYPE, buildItemsTvp(items));
            statement.setString(10, params.ghiChu());
            statement.registerOutParameter(11, microsoft.sql.Types.GUID);
            return statement;
        };
        CallableStatementCallback<UUID> callback = statement -> {
            statement.execute();
            return toUuid(statement.getObject(11));
        };
        return jdbcTemplate.execute(creator, callback);
    }

    private SQLServerDataTable buildItemsTvp(List<ChiTietGioHang> items) throws SQLServerException {
        SQLServerDataTable tvp = new SQLServerDataTable();
        tvp.addColumnMetadata("MaSP", Types.VARCHAR);
        tvp.addColumnMetadata("SoLuong", Types.INTEGER);
        for (ChiTietGioHang item : items) {
            tvp.addRow(item.getSanPham().getMaSP(), item.getSoLuong());
        }
        return tvp;
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text) {
            return UUID.fromString(text);
        }
        throw new IllegalStateException("Stored procedure did not return MaDonHang");
    }

    public record DatHangParams(
            UUID maND,
            String hoTenNguoiNhan,
            String soDienThoaiNhan,
            String diaChiGiao,
            String maKhuVucXuLi,
            String maKhoNhan,
            String phuongThucTT,
            String ghiChu
    ) {
    }
}
