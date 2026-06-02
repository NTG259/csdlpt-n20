package csdlpt.sitemain.exception;

import csdlpt.sitemain.common.ErrorCodes;
import org.springframework.http.HttpStatus;

public class DuplicatePhoneException extends BusinessException {

    public DuplicatePhoneException() {
        this("Số điện thoại đã tồn tại trong hệ thống");
    }

    public DuplicatePhoneException(String message) {
        super(ErrorCodes.DUPLICATE_PHONE, HttpStatus.CONFLICT, message);
    }
}
