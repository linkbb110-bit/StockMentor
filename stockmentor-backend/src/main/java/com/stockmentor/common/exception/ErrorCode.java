package com.stockmentor.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "请求参数不合法"),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "邮箱或密码错误"),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录"),
    AUTH_USER_DISABLED(HttpStatus.UNAUTHORIZED, "用户不可用"),
    AUTH_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "用户不存在"),
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "该邮箱已注册"),
    USER_NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "昵称格式不合法"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT(HttpStatus.CONFLICT, "资源冲突"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "没有权限执行该操作"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
