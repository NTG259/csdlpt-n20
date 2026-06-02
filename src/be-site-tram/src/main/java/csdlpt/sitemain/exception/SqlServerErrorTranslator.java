package csdlpt.sitemain.exception;

import csdlpt.sitemain.common.ErrorCodes;
import java.util.Locale;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;

public final class SqlServerErrorTranslator {

    private SqlServerErrorTranslator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static BusinessException translate(DataAccessException ex) {
        String message = extractMessage(ex);
        String normalized = message.toLowerCase(Locale.ROOT);

        if (normalized.contains("khong du")
                || normalized.contains("khÃ´ng Ä‘á»§")
                || normalized.contains("không đủ")) {
            return business(ErrorCodes.OUT_OF_STOCK, HttpStatus.CONFLICT,
                    "San pham khong du hang trong khu vuc");
        }

        if (normalized.contains("khong ton tai")
                || normalized.contains("khÃ´ng tá»“n táº¡i")
                || normalized.contains("không tồn tại")
                || normalized.contains("ngung kinh doanh")
                || normalized.contains("ngá»«ng kinh doanh")) {
            return business(ErrorCodes.PRODUCT_INVALID, HttpStatus.UNPROCESSABLE_CONTENT,
                    "San pham khong hop le");
        }

        if (normalized.contains("it nhat mot san pham")
                || normalized.contains("Ã­t nháº¥t má»™t sáº£n pháº©m")
                || normalized.contains("danh sach san pham")
                || normalized.contains("danh sÃ¡ch sáº£n pháº©m")) {
            return business(ErrorCodes.CART_EMPTY, HttpStatus.BAD_REQUEST, "Gio hang trong");
        }

        if (normalized.contains("cod")) {
            return business(ErrorCodes.PAYMENT_NOT_SUPPORTED, HttpStatus.BAD_REQUEST,
                    "Chi ho tro thanh toan COD");
        }

        if (normalized.contains("khong tim thay phieu")
                || normalized.contains("khÃ´ng tÃ¬m tháº¥y phiáº¿u")
                || normalized.contains("không tìm thấy phiếu")) {
            return business(ErrorCodes.SLIP_NOT_FOUND, HttpStatus.NOT_FOUND,
                    "Khong tim thay phieu");
        }

        if (normalized.contains("chi duoc xac nhan")
                || normalized.contains("chá»‰ Ä‘Æ°á»£c xÃ¡c nháº­n")
                || normalized.contains("chua exported")
                || normalized.contains("chÆ°a exported")) {
            return business(ErrorCodes.INVALID_ORDER_STATE, HttpStatus.CONFLICT,
                    "Phieu khong o trang thai cho phep thao tac");
        }

        return business(ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                "Loi xu ly don hang");
    }

    private static String extractMessage(DataAccessException ex) {
        Throwable cause = mostSpecificCause(ex);
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return ex.getMessage() == null ? "" : ex.getMessage();
    }

    private static Throwable mostSpecificCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static BusinessException business(String code, HttpStatus status, String message) {
        return new BusinessException(code, status, message);
    }
}
