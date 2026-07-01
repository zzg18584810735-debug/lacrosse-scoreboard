package com.edus.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.edus.common.utils.UserContext.UserDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Token工具类
 * 使用hutool的JWT实现，配合UserDto使用
 * 只负责JWT的生成和验证，不涉及Redis操作
 */
public class TokenUtil {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TOKEN_HEADER = "Authorization";
    private static final String SECRET_KEY = "edus-secret-key-2024";
    private static final long EXPIRE_TIME = 7 * 24 * 60 * 60; // 7天

    /**
     * 生成token（仅JWT生成，不存储）
     */
    public static String generateToken(UserDto userDto) {
        if (userDto == null || StrUtil.isBlank(userDto.getId())) {
            throw new IllegalArgumentException("UserDto不能为空");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userDto.getId());
        payload.put("username", userDto.getName());
        payload.put("userDto", JSONUtil.toJsonStr(userDto));
        payload.put("expireTime", System.currentTimeMillis() + EXPIRE_TIME * 1000);

        return JWTUtil.createToken(payload, SECRET_KEY.getBytes());
    }

    /**
     * 验证JWT签名（仅验证JWT有效性，不验证Redis）
     */
    public static boolean verifyJwtSignature(String token) {
        if (StrUtil.isBlank(token)) {
            return false;
        }

        try {
            // 去除Bearer前缀
            if (token.startsWith(TOKEN_PREFIX)) {
                token = token.substring(TOKEN_PREFIX.length());
            }

            return JWTUtil.verify(token, SECRET_KEY.getBytes());

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查token是否过期
     */
    public static boolean isTokenExpired(String token) {
        if (StrUtil.isBlank(token)) {
            return true;
        }

        try {
            if (token.startsWith(TOKEN_PREFIX)) {
                token = token.substring(TOKEN_PREFIX.length());
            }

            JWT jwt = JWTUtil.parseToken(token);
            JWTPayload payload = jwt.getPayload();
            Object expireTime = payload.getClaim("expireTime");

            if (expireTime != null) {
                long expireTimeLong = Long.parseLong(expireTime.toString());
                return System.currentTimeMillis() > expireTimeLong;
            }

        } catch (Exception e) {
            return true;
        }

        return false;
    }

    /**
     * 从token中获取UserDto
     */
    public static UserDto getUserDtoFromToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }

        try {
            // 去除Bearer前缀
            if (token.startsWith(TOKEN_PREFIX)) {
                token = token.substring(TOKEN_PREFIX.length());
            }

            JWT jwt = JWTUtil.parseToken(token);
            JWTPayload payload = jwt.getPayload();
            Object userDtoJson = payload.getClaim("userDto");

            if (userDtoJson != null) {
                return JSONUtil.toBean(userDtoJson.toString(), UserDto.class);
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /**
     * 从token中获取用户ID
     */
    public static String getUserIdFromToken(String token) {
        UserDto userDto = getUserDtoFromToken(token);
        return userDto != null ? userDto.getId() : null;
    }

    /**
     * 从token中获取用户名
     */
    public static String getUsernameFromToken(String token) {
        UserDto userDto = getUserDtoFromToken(token);
        return userDto != null ? userDto.getName() : null;
    }

    /**
     * 获取token前缀
     */
    public static String getTokenPrefix() {
        return TOKEN_PREFIX;
    }

    /**
     * 获取token请求头名称
     */
    public static String getTokenHeader() {
        return TOKEN_HEADER;
    }

    /**
     * 去除Bearer前缀
     */
    public static String removeBearerPrefix(String token) {
        if (StrUtil.isNotBlank(token) && token.startsWith(TOKEN_PREFIX)) {
            return token.substring(TOKEN_PREFIX.length());
        }
        return token;
    }
}