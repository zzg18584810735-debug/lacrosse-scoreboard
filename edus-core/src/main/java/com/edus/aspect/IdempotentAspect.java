package com.edus.aspect;

import com.edus.annotation.Idempotent;
import com.edus.common.components.CacheManager;
import com.edus.common.exception.BusinessException;
import com.edus.common.enums.StatusCode;
import com.edus.common.utils.UserContext;
import com.edus.common.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 幂等性切面
 * 处理@Idempotent注解的方法调用，防止重复提交
 *
 * @author edus
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    private final CacheManager cacheManager;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    // 幂等性Key前缀
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    public IdempotentAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 生成幂等性Key
        String idempotentKey = generateIdempotentKey(joinPoint, idempotent);

        // 尝试设置幂等性Key（如果已存在则返回false）
        boolean isFirstRequest = cacheManager.setIfAbsent(idempotentKey, "1", idempotent.timeout());

        if (!isFirstRequest) {
            log.warn("幂等性检查失败：key={}, message={}", idempotentKey, idempotent.message());
            throw new BusinessException(StatusCode.CONFLICT);
        }

        log.debug("幂等性检查通过：key={}, timeout={}秒", idempotentKey, idempotent.timeout());

        try {
            // 执行业务方法
            return joinPoint.proceed();
        } catch (Exception e) {
            // 如果业务执行失败，删除幂等性Key，允许重试
            log.info("业务执行失败，删除幂等性Key：key={}", idempotentKey);
            cacheManager.delete(idempotentKey);
            throw e;
        }
    }

    /**
     * 生成幂等性Key
     * 根据不同的keyType生成不同的Key
     */
    private String generateIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        StringBuilder key = new StringBuilder(IDEMPOTENT_KEY_PREFIX);

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        key.append(className).append(":").append(methodName).append(":");

        String businessKey = parseBusinessKey(joinPoint, method, idempotent.key());

        // 根据Key类型添加隔离标识
        switch (idempotent.keyType()) {
            case USER:
                key.append("user:").append(getUserId());
                break;
            case IP:
                key.append("ip:").append(WebUtils.getClientIP());
                break;
            case CUSTOM:
                key.append("custom");
                break;
            case DEFAULT:
            default:
                // 默认使用用户ID + IP的混合标识
                key.append("default:").append(getDefaultIdentifier());
                break;
        }

        if (businessKey != null && !businessKey.isBlank()) {
            key.append(":").append(businessKey);
        }

        return key.toString();
    }

    /**
     * 解析业务幂等Key，支持SpEL表达式（如 #orderId、#dto.orderNo）。
     */
    private String parseBusinessKey(ProceedingJoinPoint joinPoint, Method method, String keyExpression) {
        if (keyExpression == null || keyExpression.isBlank()) {
            return null;
        }

        try {
            if (!keyExpression.contains("#")) {
                return keyExpression;
            }

            EvaluationContext context = buildEvaluationContext(joinPoint, method);
            Object value = expressionParser.parseExpression(keyExpression).getValue(context);
            return value != null ? String.valueOf(value) : "null";
        } catch (Exception e) {
            log.warn("解析幂等Key表达式失败：expression={}", keyExpression, e);
            throw BusinessException.validationError("幂等Key表达式解析失败");
        }
    }

    /**
     * 构建SpEL上下文，同时支持参数名、#p0/#a0两种访问方式。
     */
    private EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint, Method method) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
            if (parameterNames != null && i < parameterNames.length) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        return context;
    }

    /**
     * 获取当前用户ID
     */
    private String getUserId() {
        try {
            UserContext.UserDto userDto = UserContext.get();
            if (userDto != null && userDto.getId() != null) {
                return String.valueOf(userDto.getId());
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败，使用IP作为标识", e);
        }
        return WebUtils.getClientIP();
    }

    /**
     * 获取默认标识（用户ID + IP）
     */
    private String getDefaultIdentifier() {
        String userId = "anonymous";
        try {
            UserContext.UserDto userDto = UserContext.get();
            if (userDto != null && userDto.getId() != null) {
                userId = String.valueOf(userDto.getId());
            }
        } catch (Exception e) {
            log.debug("获取用户ID失败，使用默认值", e);
        }

        String clientIp = WebUtils.getClientIP();
        return userId + ":" + clientIp;
    }
}
