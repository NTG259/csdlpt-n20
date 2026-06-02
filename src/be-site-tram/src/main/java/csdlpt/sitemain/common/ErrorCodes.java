package csdlpt.sitemain.common;

public final class ErrorCodes {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String DUPLICATE_EMAIL = "DUPLICATE_EMAIL";
    public static final String DUPLICATE_PHONE = "DUPLICATE_PHONE";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String INVALID_REGION = "INVALID_REGION";
    public static final String CART_EMPTY = "CART_EMPTY";
    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String PRODUCT_INVALID = "PRODUCT_INVALID";
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String INVALID_ORDER_STATE = "INVALID_ORDER_STATE";
    public static final String SLIP_NOT_FOUND = "SLIP_NOT_FOUND";
    public static final String PAYMENT_NOT_SUPPORTED = "PAYMENT_NOT_SUPPORTED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
        throw new UnsupportedOperationException("Utility class");
    }
}
