package com.edus.common.utils;

import com.edus.common.enums.StatusCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok() {
        return Result.<T>builder()
                .code(StatusCode.SUCCESS.getCode())
                .message(StatusCode.SUCCESS.getMessage())
                .build();
    }

    public static <T> Result<T> ok(T data) {
        return Result.<T>builder()
                .code(StatusCode.SUCCESS.getCode())
                .message(StatusCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }
    public static <T> Result<T> ok(String message) {
        return Result.<T>builder()
                .code(StatusCode.SUCCESS.getCode())
                .message(message)
                .build();
    }

    public static <T> Result<T> ok(String message, T data) {
        return Result.<T>builder()
                .code(StatusCode.SUCCESS.getCode())
                .message(message)
                .data(data)
                .build();
    }

    public static <T> Result<T> error(StatusCode StatusCode) {
        return Result.<T>builder()
                .code(StatusCode.getCode())
                .message(StatusCode.getMessage())
                .build();
    }

    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public static <T> Result<T> error(String message) {
        return Result.<T>builder()
                .code(StatusCode.INTERNAL_SERVER_ERROR.getCode())
                .message(message)
                .build();
    }
}