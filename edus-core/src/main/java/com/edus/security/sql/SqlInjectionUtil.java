package com.edus.security.sql;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL注入防护工具类
 * 提供SQL注入攻击检测和防护功能
 *
 * @author edus
 */
@Slf4j
public class SqlInjectionUtil {

    // SQL注入攻击模式
    private static final List<Pattern> SQL_INJECTION_PATTERNS = new ArrayList<>();

    static {
        // SQL关键字注入
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(')|(--)|(\\|)|(\")", Pattern.CASE_INSENSITIVE));
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(\\b(union|select|insert|update|delete|drop|alter|create|exec|execute)\\b)", Pattern.CASE_INSENSITIVE));

        // SQL注释符
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(\\/\\*|\\*\\/|--)", Pattern.CASE_INSENSITIVE));

        // SQL函数注入
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(\\b(concat|char|ascii|substring|mid|len|waitfor|delay)\\b)", Pattern.CASE_INSENSITIVE));

        // 条件注入
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(\\b(where|or|and)\\s+\\d+\\s*=\\s*\\d+)", Pattern.CASE_INSENSITIVE));

        // 堆叠查询
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(;\\s*(select|insert|update|delete|drop|alter|create))", Pattern.CASE_INSENSITIVE));

        // 时间盲注
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(waitfor\\s+delay|sleep\\s*\\()", Pattern.CASE_INSENSITIVE));

        // 布尔盲注
        SQL_INJECTION_PATTERNS.add(Pattern.compile("(and\\s+\\d+\\s*=\\s*\\d+|and\\s+\\d+\\s*!=\\s*\\d+)", Pattern.CASE_INSENSITIVE));
    }

    /**
     * 检查字符串是否包含SQL注入攻击
     *
     * @param input 待检查的输入
     * @return 是否包含SQL注入攻击
     */
    public static boolean containsSqlInjection(String input) {
        if (StrUtil.isBlank(input)) {
            return false;
        }

        // 检查所有SQL注入模式
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("检测到SQL注入攻击尝试: {}, 匹配模式: {}", input, pattern);
                return true;
            }
        }

        return false;
    }

    /**
     * 清理SQL注入攻击字符
     * 移除或转义危险的SQL字符
     *
     * @param input 原始输入
     * @return 清理后的安全字符串
     */
    public static String clean(String input) {
        if (StrUtil.isBlank(input)) {
            return input;
        }

        String cleanInput = input;

        // 移除SQL注释符
        cleanInput = cleanInput.replaceAll("--", "")
                              .replaceAll("/\\*", "")
                              .replaceAll("\\*/", "");

        // 转义单引号
        cleanInput = cleanInput.replace("'", "''");

        // 移除分号（防止堆叠查询）
        cleanInput = cleanInput.replace(";", "");

        // 如果内容被修改，记录日志
        if (!input.equals(cleanInput)) {
            log.warn("检测到SQL注入攻击尝试，已进行清理。原始内容: {}, 清理后: {}", input, cleanInput);
        }

        return cleanInput;
    }

    /**
     * 检查多个参数是否包含SQL注入
     *
     * @param params 参数数组
     * @return 是否包含SQL注入攻击
     */
    public static boolean containsSqlInjection(String... params) {
        if (params == null || params.length == 0) {
            return false;
        }

        for (String param : params) {
            if (containsSqlInjection(param)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查对象是否包含SQL注入
     * 检查对象的所有字符串属性
     *
     * @param obj 待检查对象
     * @return 是否包含SQL注入攻击
     */
    public static boolean containsSqlInjection(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof String) {
            return containsSqlInjection((String) obj);
        }

        // 对于其他对象类型，这里可以实现更复杂的检查逻辑
        // 暂时返回false
        return false;
    }

    /**
     * 安全的like查询参数处理
     * 对LIKE查询的参数进行安全处理，防止通配符注入
     *
     * @param input 原始输入
     * @return 安全的LIKE查询参数
     */
    public static String safeLikeParameter(String input) {
        if (StrUtil.isBlank(input)) {
            return input;
        }

        // 转义LIKE通配符
        return input.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    /**
     * 检查是否是危险的SQL函数调用
     *
     * @param input 待检查输入
     * @return 是否包含危险SQL函数
     */
    public static boolean containsDangerousSqlFunction(String input) {
        if (StrUtil.isBlank(input)) {
            return false;
        }

        // 危险SQL函数列表
        String[] dangerousFunctions = {
            "exec(", "execute(", "eval(", "script:",
            "waitfor delay", "sleep(", "benchmark(",
            "load_file(", "into outfile", "dumpfile"
        };

        String lowerInput = input.toLowerCase();
        for (String function : dangerousFunctions) {
            if (lowerInput.contains(function)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取SQL注入攻击的详细描述
     *
     * @param input 输入字符串
     * @return 攻击类型描述
     */
    public static String getSqlInjectionType(String input) {
        if (!containsSqlInjection(input)) {
            return "无SQL注入攻击";
        }

        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("union select")) {
            return "联合查询注入";
        } else if (lowerInput.contains("or ") && lowerInput.contains("=")) {
            return "布尔盲注";
        } else if (lowerInput.contains("waitfor delay") || lowerInput.contains("sleep")) {
            return "时间盲注";
        } else if (lowerInput.contains("--") || lowerInput.contains("/*")) {
            return "注释符注入";
        } else if (lowerInput.contains("exec(") || lowerInput.contains("execute(")) {
            return "命令执行注入";
        } else {
            return "未知SQL注入类型";
        }
    }
}