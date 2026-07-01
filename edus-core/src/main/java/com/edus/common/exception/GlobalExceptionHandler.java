package com.edus.common.exception;

import com.edus.common.enums.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 - 配合 Result 工具类使用
 *
 * @author edus
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public com.edus.common.utils.Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return com.edus.common.utils.Result.error(e.getCode(), e.getMessage(), null);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public com.edus.common.utils.Result<Void> handleValidationException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            message = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else {
            BindException ex = (BindException) e;
            message = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }
        log.error("参数校验异常: {}", message);
        return com.edus.common.utils.Result.error(StatusCode.VALIDATION_ERROR, message, null);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public com.edus.common.utils.Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return com.edus.common.utils.Result.error(StatusCode.INTERNAL_SERVER_ERROR, e.getMessage(), null);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public com.edus.common.utils.Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return com.edus.common.utils.Result.error(StatusCode.SYSTEM_ERROR, "系统异常，请稍后重试", null);
    }
}