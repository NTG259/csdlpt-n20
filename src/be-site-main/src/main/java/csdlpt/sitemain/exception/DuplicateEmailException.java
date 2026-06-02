package csdlpt.sitemain.exception;

import csdlpt.sitemain.common.ErrorCodes;
import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        this("Email đã tồn tại trong hệ thống");
    }

    public DuplicateEmailException(String message) {
        super(ErrorCodes.DUPLICATE_EMAIL, HttpStatus.CONFLICT, message);
    }
}
