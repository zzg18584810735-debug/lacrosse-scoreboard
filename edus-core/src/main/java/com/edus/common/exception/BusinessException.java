package com.edus.common.exception;

import com.edus.common.enums.StatusCode;
import lombok.Getter;

/**
 * 业务异常类
 *
 * @author edus
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 状态码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = StatusCode.BUSINESS_ERROR.getCode();
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(StatusCode statusCode) {
        super(statusCode.getMessage());
        this.code = statusCode.getCode();
        this.message = statusCode.getMessage();
    }

    public BusinessException(StatusCode statusCode, String message) {
        super(message);
        this.code = statusCode.getCode();
        this.message = message;
    }

    /**
     * 用户不存在异常
     */
    public static BusinessException userNotFound() {
        return new BusinessException(StatusCode.USER_NOT_FOUND);
    }

    /**
     * 用户已存在异常
     */
    public static BusinessException userAlreadyExists() {
        return new BusinessException(StatusCode.USER_ALREADY_EXISTS);
    }

    /**
     * 数据不存在异常
     */
    public static BusinessException dataNotFound() {
        return new BusinessException(StatusCode.DATA_NOT_EXISTS);
    }

    /**
     * 数据已存在异常
     */
    public static BusinessException dataAlreadyExists() {
        return new BusinessException(StatusCode.DATA_ALREADY_EXISTS);
    }

    /**
     * 参数校验失败异常
     */
    public static BusinessException validationError(String message) {
        return new BusinessException(StatusCode.VALIDATION_ERROR, message);
    }

    /**
     * 权限不足异常
     */
    public static BusinessException insufficientPermissions() {
        return new BusinessException(StatusCode.INSUFFICIENT_PERMISSIONS);
    }
}