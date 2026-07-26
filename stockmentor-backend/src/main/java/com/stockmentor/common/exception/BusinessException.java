package com.stockmentor.common.exception;

import java.util.Objects;

public final class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(defaultMessage(errorCode));
        this.errorCode = requiredErrorCode(errorCode);
    }

    public BusinessException(ErrorCode errorCode, ClientMessage clientMessage) {
        super(clientMessageValue(clientMessage));
        this.errorCode = requiredErrorCode(errorCode);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static String defaultMessage(ErrorCode errorCode) {
        return requiredErrorCode(errorCode).message();
    }

    private static String clientMessageValue(ClientMessage clientMessage) {
        return Objects.requireNonNull(clientMessage, "clientMessage must not be null").value();
    }

    private static ErrorCode requiredErrorCode(ErrorCode errorCode) {
        return Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public enum ClientMessage {
        REQUEST_REJECTED("当前请求无法处理");

        private final String value;

        ClientMessage(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
