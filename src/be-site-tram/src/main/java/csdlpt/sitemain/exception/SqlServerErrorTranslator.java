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

    public static BusinessException translateWarehouse(DataAccessException ex) {
        String message = extractMessage(ex);
        String normalized = message.toLowerCase(Locale.ROOT);

        if (normalized.contains("msdtc")
                || normalized.contains("distributed transaction")
                || normalized.contains("linked server")
                || normalized.contains("ole db")
                || normalized.contains("[link]")) {
            return business(ErrorCodes.DISTRIBUTED_TRANSACTION_ERROR, HttpStatus.SERVICE_UNAVAILABLE,
                    "Loi giao dich phan tan hoac linked server");
        }

        if (normalized.contains("khong tim thay phieu")
                || normalized.contains("khÃƒÂ´ng tÃƒÂ¬m thÃ¡ÂºÂ¥y phiÃ¡ÂºÂ¿u")
                || normalized.contains("khÃ´ng tÃ¬m tháº¥y phiáº¿u")) {
            return business(ErrorCodes.SLIP_NOT_FOUND, HttpStatus.NOT_FOUND,
                    "Khong tim thay phieu");
        }

        if (normalized.contains("chua gom")
                || normalized.contains("chua co du hang")
                || normalized.contains("khong the tao phieu xuat giao khach")
                || normalized.contains("xuat giao khach")
                || normalized.contains("chÆ°a gom")
                || normalized.contains("chÆ°a cÃ³ Ä‘á»§ hÃ ng")) {
            return business(ErrorCodes.ORDER_NOT_READY_TO_SHIP, HttpStatus.CONFLICT,
                    "Don hang chua san sang giao khach");
        }

        if (normalized.contains("khong du")
                || normalized.contains("khÃƒÂ´ng Ã„â€˜Ã¡Â»Â§")
                || normalized.contains("khÃ´ng Ä‘á»§")
                || normalized.contains("tá»“n kho")) {
            return business(ErrorCodes.OUT_OF_STOCK, HttpStatus.CONFLICT,
                    "Ton kho khong du de thao tac");
        }

        if (normalized.contains("chi duoc xac nhan")
                || normalized.contains("chÃ¡Â»â€° Ã„â€˜Ã†Â°Ã¡Â»Â£c xÃƒÂ¡c nhÃ¡ÂºÂ­n")
                || normalized.contains("chua exported")
                || normalized.contains("chÆ°a exported")
                || normalized.contains("waiting_export")
                || normalized.contains("waiting_import")) {
            return business(ErrorCodes.INVALID_SLIP_STATUS, HttpStatus.CONFLICT,
                    "Phieu khong o trang thai cho phep thao tac");
        }

        if (normalized.contains("kho")
                && (normalized.contains("khong hop le")
                || normalized.contains("khÃ´ng há»£p lá»‡")
                || normalized.contains("khÃ´ng há»£p lá»‡"))) {
            return business(ErrorCodes.WAREHOUSE_SCOPE_DENIED, HttpStatus.FORBIDDEN,
                    "Kho khong hop le cho thao tac nay");
        }

        return business(ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                "Loi xu ly kho");
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
