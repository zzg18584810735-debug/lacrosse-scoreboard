package com.edus.security;

import com.edus.security.sql.SqlInjectionUtil;
import com.edus.security.xss.XssUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 安全工具类
 * 整合XSS防护和SQL注入防护功能
 *
 * @author edus
 */
@Slf4j
public class SecurityUtil {

    /**
     * 综合安全检查
     * 对输入进行XSS和SQL注入检查
     *
     * @param input 待检查的输入
     * @return 是否安全
     */
    public static boolean isSafe(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        // 检查XSS攻击
        if (XssUtil.containsXss(input)) {
            log.warn("检测到XSS攻击: {}", input);
            return false;
        }

        // 检查SQL注入
        if (SqlInjectionUtil.containsSqlInjection(input)) {
            log.warn("检测到SQL注入: {}", input);
            return false;
        }

        return true;
    }

    /**
     * 清理不安全的输入
     * 移除XSS和SQL注入攻击字符
     *
     * @param input 原始输入
     * @return 清理后的安全输入
     */
    public static String clean(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // 先清理SQL注入
        String cleaned = SqlInjectionUtil.clean(input);

        // 再清理XSS
        cleaned = XssUtil.clean(cleaned);

        return cleaned;
    }

    /**
     * 验证并清理输入
     * 如果不安全则抛出异常
     *
     * @param input 待验证的输入
     * @param fieldName 字段名（用于错误消息）
     * @throws RuntimeException 如果输入不安全
     */
    public static void validateAndClean(StringBuilder input, String fieldName) {
        if (input == null) {
            return;
        }

        String original = input.toString();
        String cleaned = clean(original);

        if (!original.equals(cleaned)) {
            log.warn("字段 {} 包含不安全内容，已自动清理。原始: {}, 清理后: {}",
                    fieldName, original, cleaned);
            input.replace(0, input.length(), cleaned);
        }
    }

    /**
     * 严格模式安全检查
     * 用于敏感操作的输入验证
     *
     * @param input 待检查的输入
     * @return 是否安全
     */
    public static boolean isStrictSafe(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        // 使用XSSUtil的严格检查
        if (!XssUtil.isStrictSafe(input)) {
            log.warn("严格模式XSS检查失败: {}", input);
            return false;
        }

        // 使用SQL注入检查
        if (SqlInjectionUtil.containsSqlInjection(input)) {
            log.warn("严格模式SQL注入检查失败: {}", input);
            return false;
        }

        return true;
    }

    /**
     * 获取安全威胁类型
     *
     * @param input 输入字符串
     * @return 威胁类型描述
     */
    public static String getThreatType(String input) {
        if (XssUtil.containsXss(input)) {
            return "XSS攻击";
        }

        if (SqlInjectionUtil.containsSqlInjection(input)) {
            return "SQL注入: " + SqlInjectionUtil.getSqlInjectionType(input);
        }

        return "无威胁";
    }

    /**
     * 批量安全检查
     * 检查多个输入项是否安全
     *
     * @param inputs 输入数组
     * @return 是否全部安全
     */
    public static boolean areAllSafe(String... inputs) {
        if (inputs == null || inputs.length == 0) {
            return true;
        }

        for (String input : inputs) {
            if (!isSafe(input)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 批量清理输入
     *
     * @param inputs 输入数组
     * @return 清理后的数组
     */
    public static String[] cleanAll(String... inputs) {
        if (inputs == null || inputs.length == 0) {
            return inputs;
        }

        String[] cleaned = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            cleaned[i] = clean(inputs[i]);
        }

        return cleaned;
    }
}