package com.edus.common.utils;

import com.edus.common.enums.StatusCode;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一返回结果类 - 参照人人开发平台设计
 *
 * @author edus
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private int code;

    /**
     * 描述信息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 额外信息
     */
    private Map<String, Object> extra;

    public Result() {
    }

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(StatusCode statusCode) {
        this.code = statusCode.getCode();
        this.message = statusCode.getMessage();
    }

    public Result(StatusCode statusCode, T data) {
        this.code = statusCode.getCode();
        this.message = statusCode.getMessage();
        this.data = data;
    }

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> ok() {
        return new Result<>(StatusCode.SUCCESS);
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(StatusCode.SUCCESS, data);
    }

    /**
     * 成功返回（带数据和自定义消息）
     */
    public static <T> Result<T> ok(T data, String message) {
        Result<T> result = new Result<>(StatusCode.SUCCESS, data);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回（默认错误）
     */
    public static <T> Result<T> error() {
        return new Result<>(StatusCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * 失败返回（带状态码）
     */
    public static <T> Result<T> error(StatusCode statusCode) {
        return new Result<>(statusCode);
    }

    /**
     * 失败返回（带自定义消息）
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>(StatusCode.INTERNAL_SERVER_ERROR);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回（带状态码和自定义消息）
     */
    public static <T> Result<T> error(StatusCode statusCode, String message) {
        Result<T> result = new Result<>(statusCode);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回（带状态码和数据）
     */
    public static <T> Result<T> error(StatusCode statusCode, T data) {
        return new Result<>(statusCode, data);
    }

    /**
     * 失败返回（完整参数）
     */
    public static <T> Result<T> error(int code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 失败返回（带状态码、消息和数据）
     */
    public static <T> Result<T> error(StatusCode statusCode, String message, T data) {
        Result<T> result = new Result<>(statusCode, data);
        result.setMessage(message);
        return result;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code == StatusCode.SUCCESS.getCode();
    }

    /**
     * 判断是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * 设置额外信息
     */
    public Result<T> setExtra(String key, Object value) {
        if (this.extra == null) {
            this.extra = new HashMap<>();
        }
        this.extra.put(key, value);
        return this;
    }

    /**
     * 获取额外信息
     */
    public Object getExtra(String key) {
        return this.extra != null ? this.extra.get(key) : null;
    }

    /**
     * 获取所有额外信息
     */
    public Map<String, Object> getExtras() {
        return this.extra;
    }
}