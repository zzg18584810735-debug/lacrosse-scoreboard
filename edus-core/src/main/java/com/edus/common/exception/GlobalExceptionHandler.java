package com.edus.common.exception;

import com.edus.common.enums.StatusCode;
import com.edus.common.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return ResponseEntity
                .status(resolveHttpStatus(e.getCode()))
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidationException(Exception e) {
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
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Result.error(StatusCode.VALIDATION_ERROR.getCode(), message));
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(StatusCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("系统异常: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(StatusCode.SYSTEM_ERROR));
    }

    /**
     * 将业务状态码映射为HTTP状态码，保留响应体中的业务code。
     */
    private HttpStatus resolveHttpStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null && code >= 400) {
            return status;
        }

        StatusCode statusCode = StatusCode.valueOf(code);
        return switch (statusCode) {
            case BAD_REQUEST, VALIDATION_ERROR, BUSINESS_ERROR, USER_ALREADY_EXISTS, DATA_ALREADY_EXISTS ->
                    HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, LOGIN_EXPIRED, TOKEN_INVALID, SIGNATURE_INVALID, INVALID_CREDENTIALS ->
                    HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, INSUFFICIENT_PERMISSIONS -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, USER_NOT_FOUND, DATA_NOT_EXISTS -> HttpStatus.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case REQUEST_TIMEOUT -> HttpStatus.REQUEST_TIMEOUT;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
            case SERVICE_UNAVAILABLE, NETWORK_ERROR -> HttpStatus.SERVICE_UNAVAILABLE;
            case DATABASE_ERROR, SYSTEM_ERROR, INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
