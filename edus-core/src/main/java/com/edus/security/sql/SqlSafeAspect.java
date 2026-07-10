package com.edus.security.sql;
import com.edus.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * SQL安全检查切面
 * 对标注了@SqlSafe注解的方法进行SQL注入检查
 *
 * @author edus
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SqlSafeAspect {

    /**
     * 环绕通知，拦截@SqlSafe注解的方法
     */
    @Around("@annotation(com.edus.security.sql.SqlSafe)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SqlSafe sqlSafe = method.getAnnotation(SqlSafe.class);

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        // 检查参数是否包含SQL注入
        checkSqlInjection(args, sqlSafe);

        // 继续执行方法
        return joinPoint.proceed();
    }

    /**
     * 检查参数是否包含SQL注入
     *
     * @param args 方法参数
     * @param sqlSafe SqlSafe注解
     */
    private void checkSqlInjection(Object[] args, SqlSafe sqlSafe) {
        if (args == null || args.length == 0) {
            return;
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            // 检查字符串参数
            if (arg instanceof String) {
                String strValue = (String) arg;
                if (SqlInjectionUtil.containsSqlInjection(strValue)) {
                    handleSqlInjection(strValue, sqlSafe);
                }
            }
            // 检查数组参数
            else if (arg instanceof Object[]) {
                Object[] array = (Object[]) arg;
                for (Object item : array) {
                    if (item instanceof String && SqlInjectionUtil.containsSqlInjection((String) item)) {
                        handleSqlInjection((String) item, sqlSafe);
                    }
                }
            }
            // 检查集合参数
            else if (arg instanceof Iterable) {
                Iterable<?> iterable = (Iterable<?>) arg;
                for (Object item : iterable) {
                    if (item instanceof String && SqlInjectionUtil.containsSqlInjection((String) item)) {
                        handleSqlInjection((String) item, sqlSafe);
                    }
                }
            }
        }
    }

    /**
     * 处理检测到的SQL注入
     *
     * @param maliciousContent 恶意内容
     * @param sqlSafe SqlSafe注解
     */
    private void handleSqlInjection(String maliciousContent, SqlSafe sqlSafe) {
        String injectionType = SqlInjectionUtil.getSqlInjectionType(maliciousContent);

        if (sqlSafe.logWarning()) {
            log.warn("检测到SQL注入攻击！类型: {}, 内容: {}, 方法: {}",
                    injectionType, maliciousContent, getCallingMethod());
        }

        if (sqlSafe.strict()) {
            throw new BusinessException(400, sqlSafe.message());
        }
    }

    /**
     * 获取调用方法信息
     */
    private String getCallingMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            return stackTrace[3].toString();
        }
        return "Unknown";
    }
}