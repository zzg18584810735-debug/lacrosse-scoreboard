package com.edus.common.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户上下文工具类
 * 使用 ThreadLocal 存储当前请求的用户信息（用户）
 */
public class UserContext {

    private static final ThreadLocal<UserDto> THREAD_LOCAL = new ThreadLocal<>();

    public static UserDto get() {
        return THREAD_LOCAL.get();
    }

    public static void set(UserDto value) {
        if (value == null) {
            throw new IllegalArgumentException("UserDto cannot be null");
        }
        THREAD_LOCAL.set(value);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    /**
     * 获取当前家长ID
     */
    public static Long getCurrentUserId() {
        UserDto user = get();
        if (user == null || user.getId() == null || user.getId().trim().isEmpty()) {
            return null;
        }
        try {
            long uid = Long.parseLong(user.getId().trim());
            return uid > 0 ? uid : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取当前家长姓名
     */
    public static String getCurrentUserName() {
        UserDto user = get();
        return (user != null) ? user.getName() : null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        /** 用户ID */
        String id;
        /** 用户名称 */
        String name;
    }
}
