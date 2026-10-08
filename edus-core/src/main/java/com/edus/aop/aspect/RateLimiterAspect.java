package com.edus.aop.aspect;

import com.edus.aop.annotation.RateLimiter;
import com.edus.common.exception.BusinessException;
import com.edus.common.enums.StatusCode;
import com.edus.common.utils.UserContext;
import com.edus.common.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 限流切面
 * 基于Redis ZSet + Lua脚本实现滑动窗口限流
 *
 * @author edus
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    private final StringRedisTemplate redisTemplate;

    // Lua脚本：基于ZSet的滑动窗口原子限流操作
    private static final String LUA_SCRIPT =
            "local key = KEYS[1]\n" +
                    "local limit = tonumber(ARGV[1])\n" +
                    "local window = tonumber(ARGV[2])\n" +
                    "local now = tonumber(ARGV[3])\n" +
                    "local member = ARGV[4]\n" +
                    "redis.call('zremrangebyscore', key, 0, now - window)\n" +
                    "local current = tonumber(redis.call('zcard', key))\n" +
                    "if current >= limit then\n" +
                    "    return 0\n" +
                    "else\n" +
                    "    redis.call('zadd', key, now, member)\n" +
                    "    redis.call('pexpire', key, window)\n" +
                    "    return 1\n" +
                    "end";

    public RateLimiterAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint joinPoint, RateLimiter rateLimiter) {
        if (rateLimiter.limit() <= 0 || rateLimiter.timeout() <= 0) {
            throw new BusinessException(StatusCode.BAD_REQUEST);
        }

        // 生成限流key
        String limitKey = getCombineKey(rateLimiter, joinPoint);
        long now = System.currentTimeMillis();
        long windowMillis = rateLimiter.timeout() * 1000L;
        String member = now + ":" + UUID.randomUUID();

        // 执行限流检查
        List<String> keys = Collections.singletonList(limitKey);
        RedisScript<Long> redisScript = RedisScript.of(LUA_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                keys,
                String.valueOf(rateLimiter.limit()),
                String.valueOf(windowMillis),
                String.valueOf(now),
                member
        );

        if (result == null || result == 0) {
            log.warn("滑动窗口限流触发：key={}, limit={}, timeout={}秒",
                    limitKey, rateLimiter.limit(), rateLimiter.timeout());
            throw new BusinessException(StatusCode.TOO_MANY_REQUESTS);
        }

        log.debug("滑动窗口限流检查通过：key={}", limitKey);
    }

    /**
     * 生成限流key
     * 根据不同的限流类型生成不同的key
     */
    private String getCombineKey(RateLimiter rateLimiter, JoinPoint joinPoint) {
        StringBuilder key = new StringBuilder("rate_limit:");

        // 自定义key前缀
        if (!rateLimiter.key().isEmpty()) {
            key.append(rateLimiter.key()).append(":");
        } else {
            // 使用类名+方法名作为key
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = method.getDeclaringClass().getSimpleName();
            String methodName = method.getName();
            key.append(className).append(":").append(methodName).append(":");
        }

        // 根据限流类型添加标识
        switch (rateLimiter.limitType()) {
            case IP:
                key.append(WebUtils.getClientIP());
                break;
            case USER:
                key.append(getUserId());
                break;
            case DEFAULT:
            default:
                key.append("global");
                break;
        }

        return key.toString();
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
            log.warn("获取用户信息失败，使用默认标识", e);
        }
        return "anonymous";
    }
}
