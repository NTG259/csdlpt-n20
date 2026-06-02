package csdlpt.sitemain.exception;

import csdlpt.sitemain.common.ErrorCodes;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        this("Email hoặc mật khẩu không chính xác");
    }

    public InvalidCredentialsException(String message) {
        super(ErrorCodes.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, message);
    }
}
