package csdlpt.sitemain.common;

public final class ErrorCodes {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String DUPLICATE_EMAIL = "DUPLICATE_EMAIL";
    public static final String DUPLICATE_PHONE = "DUPLICATE_PHONE";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String INVALID_REGION = "INVALID_REGION";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
        throw new UnsupportedOperationException("Utility class");
    }
}
