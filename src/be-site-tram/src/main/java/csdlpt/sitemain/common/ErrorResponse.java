package csdlpt.sitemain.common;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        boolean success,
        String message,
        String errorCode,
        List<String> details,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(String message, String errorCode) {
        return new ErrorResponse(false, message, errorCode, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(String message, String errorCode, List<String> details) {
        return new ErrorResponse(false, message, errorCode, details == null ? List.of() : details, LocalDateTime.now());
    }
}
