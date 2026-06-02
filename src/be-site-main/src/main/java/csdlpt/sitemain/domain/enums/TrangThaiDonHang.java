package csdlpt.sitemain.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TrangThaiDonHang {
    PENDING("pending"),
    PROCESSING("processing"),
    SHIPPING("shipping"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String dbValue;

    TrangThaiDonHang(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static TrangThaiDonHang fromValue(String value) {
        if (value == null) return null;
        for (TrangThaiDonHang t : values()) {
            if (t.dbValue.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + value
                + ". Các giá trị hợp lệ: pending, processing, shipping, completed, cancelled");
    }
}
