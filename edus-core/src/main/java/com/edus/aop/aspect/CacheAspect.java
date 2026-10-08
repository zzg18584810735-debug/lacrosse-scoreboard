package com.edus.aop.aspect;

import com.edus.aop.annotation.Cache;
import com.edus.common.components.CacheManager;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 缓存切面
 * 处理@Cache注解的方法调用
 *
 * @author edus
 */
@Slf4j
@Aspect
@Component
public class CacheAspect {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    public CacheAspect(CacheManager cacheManager, ObjectMapper objectMapper) {
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(cacheAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Cache cacheAnnotation) throws Throwable {
        // 生成缓存key
        String cacheKey = generateCacheKey(joinPoint, cacheAnnotation);
        Method method = getMethod(joinPoint);

        // 检查缓存条件
        if (!evaluateCondition(joinPoint, cacheAnnotation.condition())) {
            log.debug("缓存条件不满足，直接执行方法：key={}", cacheKey);
            return joinPoint.proceed();
        }

        // 尝试从缓存获取
        String cachedValue = cacheManager.get(cacheKey);
        if (cachedValue != null) {
            try {
                log.debug("缓存命中：key={}", cacheKey);
                return deserializeObject(cachedValue, method);
            } catch (Exception e) {
                log.warn("缓存反序列化失败，删除缓存并回源执行：key={}", cacheKey, e);
                cacheManager.delete(cacheKey);
            }
        }

        // 执行方法
        Object result = joinPoint.proceed();

        // 存入缓存
        if (result != null) {
            String serializedValue = serializeObject(result);
            if (serializedValue != null) {
                cacheManager.set(cacheKey, serializedValue, cacheAnnotation.expire());
                log.debug("缓存设置：key={}, expire={}秒", cacheKey, cacheAnnotation.expire());
            }
        }

        return result;
    }

    /**
     * 获取被拦截方法
     */
    private Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod();
    }

    /**
     * 生成缓存key
     */
    private String generateCacheKey(ProceedingJoinPoint joinPoint, Cache cacheAnnotation) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        String keyTemplate = cacheAnnotation.key();

        // 如果没有自定义key，使用类名:方法名
        if (keyTemplate.isEmpty()) {
            keyTemplate = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
            // 如果有参数，添加参数hash
            if (args.length > 0) {
                keyTemplate += ":" + generateArgsHash(args);
            }
            return keyTemplate;
        }

        // 处理自定义key中的参数占位符
        String key = keyTemplate;
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length && i < args.length; i++) {
            String placeholder = "{" + i + "}";
            if (key.contains(placeholder)) {
                String paramValue = args[i] != null ? args[i].toString() : "null";
                key = key.replace(placeholder, paramValue);
            }
        }

        return key;
    }

    /**
     * 生成参数hash（用于默认key生成）
     */
    private String generateArgsHash(Object[] args) {
        if (args == null || args.length == 0) {
            return "empty";
        }

        StringBuilder hash = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                hash.append(arg.hashCode());
            } else {
                hash.append("null");
            }
        }
        return hash.toString();
    }

    /**
     * 评估缓存条件
     */
    private boolean evaluateCondition(ProceedingJoinPoint joinPoint, String condition) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        // 简单的条件判断，支持基本的参数比较
        // 实际项目中可以使用SpEL表达式
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        try {
            // 简单的条件解析，如 #id > 0
            if (condition.startsWith("#")) {
                String[] parts = condition.split(" ");
                if (parts.length >= 2) {
                    String paramName = parts[0].substring(1); // 去掉#
                    String operator = parts[1];
                    String value = parts.length > 2 ? parts[2] : "0";

                    // 找到对应参数的值
                    for (int i = 0; i < parameters.length; i++) {
                        if (parameters[i].getName().equals(paramName) && i < args.length) {
                            Object paramValue = args[i];
                            if (paramValue instanceof Number) {
                                long paramNum = ((Number) paramValue).longValue();
                                long compareNum = Long.parseLong(value);
                                return switch (operator) {
                                    case ">" -> paramNum > compareNum;
                                    case ">=" -> paramNum >= compareNum;
                                    case "<" -> paramNum < compareNum;
                                    case "<=" -> paramNum <= compareNum;
                                    case "==" -> paramNum == compareNum;
                                    case "!=" -> paramNum != compareNum;
                                    default -> true;
                                };
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("条件评估失败，默认返回true：condition={}", condition, e);
        }

        return true;
    }

    /**
     * 序列化对象
     */
    private String serializeObject(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象序列化失败", e);
            return null;
        }
    }

    /**
     * 反序列化对象
     */
    private Object deserializeObject(String str, Method method) throws Exception {
        if (str == null) {
            return null;
        }

        JavaType returnType = objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
        return objectMapper.readValue(str, returnType);
    }
}
