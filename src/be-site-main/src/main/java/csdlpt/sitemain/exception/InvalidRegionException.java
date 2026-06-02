package csdlpt.sitemain.exception;

import csdlpt.sitemain.common.ErrorCodes;
import org.springframework.http.HttpStatus;

public class InvalidRegionException extends BusinessException {

    public InvalidRegionException() {
        this("Mã khu vực không hợp lệ");
    }

    public InvalidRegionException(String message) {
        super(ErrorCodes.INVALID_REGION, HttpStatus.BAD_REQUEST, message);
    }
}
