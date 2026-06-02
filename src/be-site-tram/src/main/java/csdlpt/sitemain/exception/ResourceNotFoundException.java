package csdlpt.sitemain.exception;

import csdlpt.sitemain.common.ErrorCodes;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCodes.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }
}
