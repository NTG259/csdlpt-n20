package csdlpt.sitemain.exception;

import csdlpt.sitemain.common.ErrorCodes;
import csdlpt.sitemain.common.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex
    ) {
        List<String> details = Stream.concat(
                ex.getBindingResult().getFieldErrors().stream().map(this::toFieldErrorDetail),
                ex.getBindingResult().getGlobalErrors().stream().map(this::toGlobalErrorDetail)
        ).toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu đầu vào không hợp lệ",
                ErrorCodes.VALIDATION_ERROR,
                details
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> simplifyPropertyPath(violation.getPropertyPath().toString())
                        + ": "
                        + defaultMessage(violation.getMessage()))
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu đầu vào không hợp lệ",
                ErrorCodes.VALIDATION_ERROR,
                details
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        return buildResponse(ex.getHttpStatus(), ex.getMessage(), ex.getErrorCode(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex
    ) {
        String rootMessage = extractRootMessage(ex);
        String normalized = rootMessage.toLowerCase(Locale.ROOT);

        log.warn("Data integrity violation: {}", rootMessage);

        if (normalized.contains("email")) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "Email đã tồn tại trong hệ thống",
                    ErrorCodes.DUPLICATE_EMAIL,
                    List.of()
            );
        }

        if (normalized.contains("so_dien_thoai")
                || normalized.contains("sodienthoai")
                || normalized.contains("phone")) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "Số điện thoại đã tồn tại trong hệ thống",
                    ErrorCodes.DUPLICATE_PHONE,
                    List.of()
            );
        }

        return buildResponse(
                HttpStatus.CONFLICT,
                "Dữ liệu trùng hoặc vi phạm ràng buộc toàn vẹn",
                ErrorCodes.VALIDATION_ERROR,
                List.of()
        );
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(Exception ex) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access denied",
                ErrorCodes.ACCESS_DENIED,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã xảy ra lỗi nội bộ. Vui lòng thử lại sau.",
                ErrorCodes.INTERNAL_ERROR,
                List.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus httpStatus,
            String message,
            String errorCode,
            List<String> details
    ) {
        return ResponseEntity.status(httpStatus)
                .body(ErrorResponse.of(message, errorCode, details));
    }

    private String toFieldErrorDetail(FieldError error) {
        return error.getField() + ": " + defaultMessage(error.getDefaultMessage());
    }

    private String toGlobalErrorDetail(ObjectError error) {
        return error.getObjectName() + ": " + defaultMessage(error.getDefaultMessage());
    }

    private String simplifyPropertyPath(String propertyPath) {
        int lastDotIndex = propertyPath.lastIndexOf('.');
        if (lastDotIndex >= 0 && lastDotIndex < propertyPath.length() - 1) {
            return propertyPath.substring(lastDotIndex + 1);
        }
        return propertyPath;
    }

    private String defaultMessage(String message) {
        return message == null || message.isBlank() ? "Giá trị không hợp lệ" : message;
    }

    private String extractRootMessage(DataIntegrityViolationException ex) {
        Throwable mostSpecificCause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (mostSpecificCause != null && mostSpecificCause.getMessage() != null) {
            return mostSpecificCause.getMessage();
        }
        return ex.getMessage() == null ? "" : ex.getMessage();
    }
}
