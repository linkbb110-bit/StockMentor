package com.stockmentor.common.exception;

import com.stockmentor.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Pattern EMAIL_UNIQUE_CONSTRAINT = Pattern.compile(
            "(?<![A-Za-z0-9_])uk_sys_user_email(?![A-Za-z0-9_])"
    );
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return response(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        return response(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        return response(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception) {
        return response(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(
            DuplicateKeyException exception
    ) {
        return response(
                ErrorCode.USER_EMAIL_ALREADY_EXISTS,
                ErrorCode.USER_EMAIL_ALREADY_EXISTS.message()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception
    ) {
        if (causeChainContainsEmailUniqueConstraint(exception)) {
            return response(
                    ErrorCode.USER_EMAIL_ALREADY_EXISTS,
                    ErrorCode.USER_EMAIL_ALREADY_EXISTS.message()
            );
        }

        log.error("Unexpected data integrity violation");
        return response(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unexpected exception");
        return response(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message());
    }

    private boolean causeChainContainsEmailUniqueConstraint(Throwable exception) {
        Set<Throwable> visited =
                Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = exception;
        while (current != null && visited.add(current)) {
            String message = current.getMessage();
            if (message != null
                    && EMAIL_UNIQUE_CONSTRAINT.matcher(message).find()) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<ApiResponse<Void>> response(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.failure(errorCode.name(), message));
    }
}
