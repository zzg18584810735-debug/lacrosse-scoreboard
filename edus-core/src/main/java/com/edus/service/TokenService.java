package com.edus.service;

import com.edus.common.utils.UserContext.UserDto;
import com.edus.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token服务
 * 负责Token的存储和验证
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_KEY_PREFIX = "token:";
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60; // 7天

    /**
     * 存储token到redis
     */
    public void saveToken(String userId, String token) {
        String redisKey = REDIS_KEY_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(redisKey, token, EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 从redis获取token
     */
    public String getToken(String userId) {
        String redisKey = REDIS_KEY_PREFIX + userId;
        return stringRedisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 验证token是否匹配redis中的token
     */
    public boolean validateTokenInRedis(String userId, String token) {
        String storedToken = getToken(userId);
        return token != null && token.equals(storedToken);
    }

    /**
     * 完整的token验证
     * 1. JWT签名验证
     * 2. 过期时间检查
     * 3. Redis中存在性验证
     */
    public boolean validateToken(String token) {
        // 1. 验证JWT签名
        if (!TokenUtil.verifyJwtSignature(token)) {
            return false;
        }

        // 2. 验证是否过期
        if (TokenUtil.isTokenExpired(token)) {
            return false;
        }

        // 3. 验证Redis中是否存在
        String userId = TokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }

        return validateTokenInRedis(userId, TokenUtil.removeBearerPrefix(token));
    }

    /**
     * 验证token并返回UserDto
     */
    public UserDto validateAndGetUser(String token) {
        if (!validateToken(token)) {
            return null;
        }
        return TokenUtil.getUserDtoFromToken(token);
    }

    /**
     * 删除token
     */
    public void removeToken(String userId) {
        String redisKey = REDIS_KEY_PREFIX + userId;
        stringRedisTemplate.delete(redisKey);
    }

    /**
     * 刷新token过期时间
     */
    public boolean refreshExpire(String userId) {
        String redisKey = REDIS_KEY_PREFIX + userId;
        return stringRedisTemplate.expire(redisKey, EXPIRE_TIME, TimeUnit.SECONDS);
    }
}