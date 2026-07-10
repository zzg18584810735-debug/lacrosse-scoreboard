package com.edus.security.sensitive;

import java.lang.annotation.*;

/**
 * 敏感操作注解
 * 标注需要二次验证的敏感操作方法
 *
 * 使用示例：
 * <pre>
 * {@code
 * @SensitiveOperation(
 *     operation = "删除用户",
 *     requireReason = true,
 *     verifyType = VerifyType.PASSWORD
 * )
 * public void deleteUser(Long userId, String reason, String password) {
 *     // 方法实现
 * }
 * }
 * </pre>
 *
 * @author edus
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveOperation {

    /**
     * 操作描述
     * 用于日志记录和用户提示
     */
    String operation() default "敏感操作";

    /**
     * 是否需要操作原因
     * 要求用户提供操作理由
     */
    boolean requireReason() default true;

    /**
     * 验证类型
     */
    VerifyType verifyType() default VerifyType.PASSWORD;

    /**
     * 是否强制验证
     * true: 必须通过验证才能执行
     * false: 验证失败给出警告但允许执行
     */
    boolean mandatory() default true;

    /**
     * 验证超时时间（秒）
     * 用户进行二次验证的超时时间
     */
    int timeout() default 300;

    /**
     * 自定义验证消息
     */
    String message() default "请进行二次验证以确认此敏感操作";

    /**
     * 验证类型枚举
     */
    enum VerifyType {
        /**
         * 密码验证
         */
        PASSWORD,
        /**
         * 短信验证码
         */
        SMS_CODE,
        /**
         * 邮箱验证码
         */
        EMAIL_CODE,
        /**
         * TOTP验证码（Google Authenticator等）
         */
        TOTP_CODE,
        /**
         * 操作原因确认
         */
        REASON_CONFIRM,
        /**
         * 自定义验证
         */
        CUSTOM
    }
}