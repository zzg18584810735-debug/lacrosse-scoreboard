package com.edus.security.sensitive;

import com.edus.common.exception.BusinessException;
import com.edus.common.utils.WebUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 敏感操作二次验证切面
 * 对标注了@SensitiveOperation注解的方法进行二次验证
 *
 * @author edus
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SensitiveOperationAspect {

    /**
     * 环绕通知，拦截@SensitiveOperation注解的方法
     */
    @Around("@annotation(com.edus.security.sensitive.SensitiveOperation)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SensitiveOperation sensitiveOperation = method.getAnnotation(SensitiveOperation.class);

        // 获取操作信息
        String operation = sensitiveOperation.operation();
        String clientIP = WebUtils.getClientIP();
        String userAgent = WebUtils.getUserAgent();
        String requestId = UUID.randomUUID().toString();

        // 记录敏感操作尝试
        log.info("敏感操作尝试 - 请求ID: {}, 操作: {}, IP: {}, UserAgent: {}",
                requestId, operation, clientIP, userAgent);

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 执行二次验证
        performVerification(args, sensitiveOperation, requestId);

        // 验证通过，执行方法
        try {
            Object result = joinPoint.proceed();

            // 记录操作成功
            log.info("敏感操作执行成功 - 请求ID: {}, 操作: {}", requestId, operation);
            return result;

        } catch (Exception e) {
            // 记录操作失败
            log.error("敏感操作执行失败 - 请求ID: {}, 操作: {}, 异常: {}",
                    requestId, operation, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 执行二次验证
     *
     * @param args 方法参数
     * @param sensitiveOperation 敏感操作注解
     * @param requestId 请求ID
     */
    private void performVerification(Object[] args, SensitiveOperation sensitiveOperation, String requestId) {
        // 检查是否需要操作原因
        if (sensitiveOperation.requireReason()) {
            checkOperationReason(args, sensitiveOperation);
        }

        // 根据验证类型执行相应的验证
        SensitiveOperation.VerifyType verifyType = sensitiveOperation.verifyType();

        switch (verifyType) {
            case PASSWORD:
                verifyPassword(args, sensitiveOperation, requestId);
                break;
            case SMS_CODE:
                verifySmsCode(args, sensitiveOperation, requestId);
                break;
            case EMAIL_CODE:
                verifyEmailCode(args, sensitiveOperation, requestId);
                break;
            case TOTP_CODE:
                verifyTotpCode(args, sensitiveOperation, requestId);
                break;
            case REASON_CONFIRM:
                // 原因验证已在前面处理
                break;
            case CUSTOM:
                verifyCustom(args, sensitiveOperation, requestId);
                break;
            default:
                log.warn("未知的验证类型: {}", verifyType);
        }
    }

    /**
     * 检查操作原因
     */
    private void checkOperationReason(Object[] args, SensitiveOperation sensitiveOperation) {
        String reason = extractReason(args);

        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(400, "敏感操作需要提供操作原因");
        }

        if (reason.length() < 5) {
            throw new BusinessException(400, "操作原因不能少于5个字符");
        }

        log.info("操作原因验证通过: {}", reason);
    }

    /**
     * 密码验证
     */
    private void verifyPassword(Object[] args, SensitiveOperation sensitiveOperation, String requestId) {
        String password = extractPassword(args);

        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException(400, sensitiveOperation.message());
        }

        // 这里应该调用实际的密码验证逻辑
        // 暂时只做基本验证
        if (password.length() < 6) {
            throw new BusinessException(400, "密码长度不能少于6位");
        }

        log.info("密码验证通过 - 请求ID: {}", requestId);
    }

    /**
     * 短信验证码验证
     */
    private void verifySmsCode(Object[] args, SensitiveOperation sensitiveOperation, String requestId) {
        String smsCode = extractSmsCode(args);

        if (smsCode == null || smsCode.trim().isEmpty()) {
            throw new BusinessException(400, "请输入短信验证码");
        }

        // 这里应该调用实际的短信验证码验证逻辑
        // 暂时只做格式验证
        if (!smsCode.matches("\\d{6}")) {
            throw new BusinessException(400, "短信验证码格式错误");
        }

        log.info("短信验证码验证通过 - 请求ID: {}", requestId);
    }

    /**
     * 邮箱验证码验证
     */
    private void verifyEmailCode(Object[] args, SensitiveOperation sensitiveOperation, String requestId) {
        String emailCode = extractEmailCode(args);

        if (emailCode == null || emailCode.trim().isEmpty()) {
            throw new BusinessException(400, "请输入邮箱验证码");
        }

        // 这里应该调用实际的邮箱验证码验证逻辑
        if (emailCode.length() < 6) {
            throw new BusinessException(400, "邮箱验证码格式错误");
        }

        log.info("邮箱验证码验证通过 - 请求ID: {}", requestId);
    }

    /**
     * TOTP验证码验证
     */
    private void verifyTotpCode(Object[] args, SensitiveOperation sensitiveOperation, String requestId) {
        String totpCode = extractTotpCode(args);

        if (totpCode == null || totpCode.trim().isEmpty()) {
            throw new BusinessException(400, "请输入双因素验证码");
        }

        // 这里应该调用实际的TOTP验证逻辑
        if (!totpCode.matches("\\d{6}")) {
            throw new BusinessException(400, "双因素验证码格式错误");
        }

        log.info("TOTP验证码验证通过 - 请求ID: {}", requestId);
    }

    /**
     * 自定义验证
     */
    private void verifyCustom(Object[] args, SensitiveOperation sensitiveOperation, String requestId) {
        // 自定义验证逻辑，可以根据实际需求实现
        log.info("执行自定义验证 - 请求ID: {}", requestId);
    }

    /**
     * 从参数中提取操作原因
     */
    private String extractReason(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                String strValue = (String) arg;
                // 这里可以根据参数名或位置来确定是否为原因参数
                // 暂时假设是第3个参数
                if (strValue.length() > 5 && !strValue.matches("\\d+")) {
                    return strValue;
                }
            }
        }
        return null;
    }

    /**
     * 从参数中提取密码
     */
    private String extractPassword(Object[] args) {
        // 简单实现：从参数中提取可能的密码字段
        for (Object arg : args) {
            if (arg instanceof String) {
                String strValue = (String) arg;
                if (strValue.length() >= 6) {
                    return strValue;
                }
            }
        }
        return null;
    }

    /**
     * 从参数中提取短信验证码
     */
    private String extractSmsCode(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                String strValue = (String) arg;
                if (strValue.matches("\\d{6}")) {
                    return strValue;
                }
            }
        }
        return null;
    }

    /**
     * 从参数中提取邮箱验证码
     */
    private String extractEmailCode(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                String strValue = (String) arg;
                if (strValue.length() >= 6) {
                    return strValue;
                }
            }
        }
        return null;
    }

    /**
     * 从参数中提取TOTP验证码
     */
    private String extractTotpCode(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String) {
                String strValue = (String) arg;
                if (strValue.matches("\\d{6}")) {
                    return strValue;
                }
            }
        }
        return null;
    }
}