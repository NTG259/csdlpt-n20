package csdlpt.sitemain.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TrangThaiThanhToan {
    WAITING_COD("waiting_cod"),
    PAID("paid"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String dbValue;

    TrangThaiThanhToan(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static TrangThaiThanhToan fromValue(String value) {
        if (value == null) return null;
        for (TrangThaiThanhToan t : values()) {
            if (t.dbValue.equalsIgnoreCase(value) || t.name().equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ: " + value
                + ". Các giá trị hợp lệ: waiting_cod, paid, failed, cancelled");
    }
}
