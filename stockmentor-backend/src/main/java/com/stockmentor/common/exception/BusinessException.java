package com.stockmentor.common.exception;

import java.util.Objects;

public class BusinessException extends RuntimeException {
    private static final String SAFE_CLIENT_MESSAGE_PATTERN =
            "[\\p{IsHan}0-9 ，。！？：；（）《》【】、-]{1,100}";

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(defaultMessage(errorCode));
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String safeMessage) {
        super(resolveClientMessage(errorCode, safeMessage));
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static String resolveClientMessage(ErrorCode errorCode, String candidateMessage) {
        String defaultMessage = defaultMessage(errorCode);
        if (candidateMessage == null || !candidateMessage.matches(SAFE_CLIENT_MESSAGE_PATTERN)) {
            return defaultMessage;
        }
        return candidateMessage;
    }

    private static String defaultMessage(ErrorCode errorCode) {
        return Objects.requireNonNull(errorCode, "errorCode must not be null").message();
    }
}
