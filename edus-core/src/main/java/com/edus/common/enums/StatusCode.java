package com.edus.common.enums;

import lombok.Getter;

/**
 * 状态码枚举 - 参照HTTP状态码设计
 *
 * @author edus
 */
@Getter
public enum StatusCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 创建成功
     */
    CREATED(201, "创建成功"),

    /**
     * 请求失败
     */
    BAD_REQUEST(400, "请求参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 请求方法不支持
     */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /**
     * 请求超时
     */
    REQUEST_TIMEOUT(408, "请求超时"),

    /**
     * 请求冲突
     */
    CONFLICT(409, "请求冲突"),

    /**
     * 内部服务器错误
     */
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    /**
     * 用户名或密码错误
     */
    INVALID_CREDENTIALS(1001, "用户名或密码错误"),

    /**
     * 用户不存在
     */
    USER_NOT_FOUND(1002, "用户不存在"),

    /**
     * 用户已存在
     */
    USER_ALREADY_EXISTS(1003, "用户已存在"),

    /**
     * 数据已存在
     */
    DATA_ALREADY_EXISTS(1004, "数据已存在"),

    /**
     * 数据不存在
     */
    DATA_NOT_EXISTS(1005, "数据不存在"),

    /**
     * 参数校验失败
     */
    VALIDATION_ERROR(1006, "参数校验失败"),

    /**
     * 业务逻辑异常
     */
    BUSINESS_ERROR(1007, "业务逻辑异常"),

    /**
     * 系统异常
     */
    SYSTEM_ERROR(1008, "系统异常"),

    /**
     * 数据库异常
     */
    DATABASE_ERROR(1009, "数据库异常"),

    /**
     * 网络异常
     */
    NETWORK_ERROR(1010, "网络异常"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_ERROR(1011, "文件上传失败"),

    /**
     * 文件下载失败
     */
    FILE_DOWNLOAD_ERROR(1012, "文件下载失败"),

    /**
     * 权限不足
     */
    INSUFFICIENT_PERMISSIONS(1013, "权限不足");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 描述信息
     */
    private final String message;

    StatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据状态码获取枚举
     */
    public static StatusCode valueOf(int code) {
        for (StatusCode status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}