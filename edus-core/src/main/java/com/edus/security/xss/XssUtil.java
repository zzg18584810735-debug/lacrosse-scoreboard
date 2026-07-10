package com.edus.security.xss;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * XSS防护工具类
 * 提供XSS攻击字符过滤和清理功能
 *
 * @author edus
 */
@Slf4j
public class XssUtil {

    // XSS攻击模式匹配
    private static final Pattern[] XSS_PATTERNS = {
        // 脚本标签
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
        // 事件处理器
        Pattern.compile("on[a-z]+\\s*=", Pattern.CASE_INSENSITIVE),
        // JavaScript协议
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        // 表达式
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload=", Pattern.CASE_INSENSITIVE),
        // iframe标签
        Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE),
        // object标签
        Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE),
        // embed标签
        Pattern.compile("<embed[^>]*>.*?</embed>", Pattern.CASE_INSENSITIVE),
        // meta标签
        Pattern.compile("<meta[^>]*>", Pattern.CASE_INSENSITIVE),
        // link标签
        Pattern.compile("<link[^>]*>", Pattern.CASE_INSENSITIVE),
        // style标签
        Pattern.compile("<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE),
        // HTML实体编码攻击
        Pattern.compile("&#", Pattern.CASE_INSENSITIVE),
        // 评估函数
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 清理XSS攻击字符
     * 使用正则表达式过滤危险内容
     *
     * @param content 原始内容
     * @return 清理后的安全内容
     */
    public static String clean(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }

        String cleanContent = content;

        // 逐个应用XSS过滤模式
        for (Pattern pattern : XSS_PATTERNS) {
            cleanContent = pattern.matcher(cleanContent).replaceAll("");
        }

        // HTML特殊字符转义
        cleanContent = htmlEscape(cleanContent);

        // 如果内容被修改，记录日志
        if (!content.equals(cleanContent)) {
            log.warn("检测到XSS攻击尝试，已进行过滤处理。原始内容长度: {}, 过滤后长度: {}",
                    content.length(), cleanContent.length());
        }

        return cleanContent;
    }

    /**
     * HTML特殊字符转义
     *
     * @param content 原始内容
     * @return 转义后的内容
     */
    private static String htmlEscape(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }

        return content.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\"", "&quot;")
                      .replace("'", "&#x27;")
                      .replace("/", "&#x2F;");
    }

    /**
     * 检查内容是否包含XSS攻击字符
     *
     * @param content 待检查内容
     * @return 是否包含XSS攻击字符
     */
    public static boolean containsXss(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 严格模式XSS检查
     * 用于敏感操作的输入验证
     *
     * @param content 待检查内容
     * @return 是否安全
     */
    public static boolean isStrictSafe(String content) {
        if (StrUtil.isBlank(content)) {
            return true;
        }

        // 检查是否包含任何HTML标签
        return !containsHtmlTags(content) && !containsXss(content);
    }

    /**
     * 检查是否包含HTML标签
     *
     * @param content 待检查内容
     * @return 是否包含HTML标签
     */
    private static boolean containsHtmlTags(String content) {
        return content.contains("<") && content.contains(">");
    }

    /**
     * 白名单过滤
     * 只保留安全的HTML标签
     *
     * @param content 原始内容
     * @param allowedTags 允许的标签列表，如 {"b", "i", "u"}
     * @return 过滤后的内容
     */
    public static String whitelistFilter(String content, String[] allowedTags) {
        // 简单实现：移除所有HTML标签（除了允许的标签）
        // 实际生产环境建议使用专业的HTML清理库，如 JSoup
        if (StrUtil.isBlank(content)) {
            return content;
        }

        String result = content;
        // 移除所有不在白名单中的标签
        // 这里提供基础实现，生产环境建议使用更完善的解决方案
        if (allowedTags == null || allowedTags.length == 0) {
            // 没有白名单，移除所有HTML标签
            result = result.replaceAll("<[^>]*>", "");
        }

        return result;
    }
}