package com.stockmentor.common.exception;

import java.util.Locale;
import java.util.Objects;

public class BusinessException extends RuntimeException {
    private static final int MAX_SAFE_MESSAGE_LENGTH = 200;

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
        if (candidateMessage == null || candidateMessage.isBlank()
                || candidateMessage.length() > MAX_SAFE_MESSAGE_LENGTH
                || candidateMessage.chars().anyMatch(Character::isISOControl)) {
            return defaultMessage;
        }

        String normalizedMessage = candidateMessage.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("exception")
                || normalizedMessage.contains("jdbc")
                || normalizedMessage.contains("java.sql")
                || normalizedMessage.matches(".*\\bsql\\b.*")) {
            return defaultMessage;
        }
        return candidateMessage;
    }

    private static String defaultMessage(ErrorCode errorCode) {
        return Objects.requireNonNull(errorCode, "errorCode must not be null").message();
    }
}
