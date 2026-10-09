package com.edus.common.components;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Duration;

/**
 * 缓存管理器
 * 统一管理Redis缓存操作，提供简洁的缓存操作接口
 *
 * @author edus
 */
@Slf4j
@Component
public class CacheManager {

    private final RedisClient redisClient;

    // 缓存key前缀
    private static final String CACHE_PREFIX = "cache:";

    // 默认过期时间（秒）
    private static final long DEFAULT_EXPIRE_TIME = 3600; // 1小时

    // 不过期的标记
    private static final long NO_EXPIRE = -1;

    public CacheManager(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    // ==================== 字符串操作 ====================

    /**
     * 设置缓存
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    public void set(String key, String value) {
        set(key, value, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 设置缓存（指定过期时间）
     *
     * @param key              缓存key
     * @param value            缓存值
     * @param expireTimeSeconds 过期时间（秒）
     */
    public void set(String key, String value, long expireTimeSeconds) {
        String fullKey = buildKey(key);
        try {
            if (expireTimeSeconds == NO_EXPIRE) {
                redisClient.set(fullKey, value);
            } else {
                redisClient.set(fullKey, value, Duration.ofSeconds(expireTimeSeconds));
            }
            log.debug("缓存设置成功：key={}, expireTime={}秒", fullKey, expireTimeSeconds);
        } catch (Exception e) {
            log.error("缓存设置失败：key={}", fullKey, e);
        }
    }

    /**
     * 获取缓存
     *
     * @param key 缓存key
     * @return 缓存值，不存在返回null
     */
    public String get(String key) {
        String fullKey = buildKey(key);
        try {
            String value = redisClient.get(fullKey);
            log.debug("缓存获取：key={}, exists={}", fullKey, value != null);
            return value;
        } catch (Exception e) {
            log.error("缓存获取失败：key={}", fullKey, e);
            return null;
        }
    }

    /**
     * 原子性设置缓存（仅当key不存在时）
     *
     * @param key              缓存key
     * @param value            缓存值
     * @param expireTimeSeconds 过期时间（秒）
     * @return 设置成功返回true，key已存在返回false
     */
    public boolean setIfAbsent(String key, String value, long expireTimeSeconds) {
        String fullKey = buildKey(key);
        try {
            boolean result = expireTimeSeconds == NO_EXPIRE
                    ? redisClient.setIfAbsent(fullKey, value)
                    : redisClient.setIfAbsent(fullKey, value, Duration.ofSeconds(expireTimeSeconds));
            log.debug("原子性设置缓存：key={}, success={}", fullKey, result);
            return result;
        } catch (Exception e) {
            log.error("原子性设置缓存失败：key={}", fullKey, e);
            return false;
        }
    }

    /**
     * 原子性设置缓存（仅当key不存在时，使用默认过期时间）
     *
     * @param key   缓存key
     * @param value 缓存值
     * @return 设置成功返回true，key已存在返回false
     */
    public boolean setIfAbsent(String key, String value) {
        return setIfAbsent(key, value, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 删除缓存
     *
     * @param key 缓存key
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        String fullKey = buildKey(key);
        try {
            boolean result = redisClient.delete(fullKey);
            log.debug("缓存删除：key={}, success={}", fullKey, result);
            return result;
        } catch (Exception e) {
            log.error("缓存删除失败：key={}", fullKey, e);
            return false;
        }
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存key集合
     * @return 删除数量
     */
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        List<String> fullKeys = keys.stream()
                .map(this::buildKey)
                .toList();

        try {
            long count = redisClient.delete(fullKeys);
            log.debug("批量删除缓存：count={}", count);
            return count;
        } catch (Exception e) {
            log.error("批量删除缓存失败", e);
            return 0;
        }
    }

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存key
     * @return 是否存在
     */
    public boolean exists(String key) {
        String fullKey = buildKey(key);
        try {
            return redisClient.hasKey(fullKey);
        } catch (Exception e) {
            log.error("检查缓存存在性失败：key={}", fullKey, e);
            return false;
        }
    }

    /**
     * 设置过期时间
     *
     * @param key              缓存key
     * @param expireTimeSeconds 过期时间（秒）
     * @return 是否设置成功
     */
    public boolean expire(String key, long expireTimeSeconds) {
        String fullKey = buildKey(key);
        try {
            return redisClient.expire(fullKey, Duration.ofSeconds(expireTimeSeconds));
        } catch (Exception e) {
            log.error("设置过期时间失败：key={}", fullKey, e);
            return false;
        }
    }

    /**
     * 获取剩余过期时间
     *
     * @param key 缓存key
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示不存在
     */
    public long getExpire(String key) {
        String fullKey = buildKey(key);
        try {
            return redisClient.getExpireSeconds(fullKey);
        } catch (Exception e) {
            log.error("获取过期时间失败：key={}", fullKey, e);
            return -2;
        }
    }

    // ==================== Hash操作 ====================

    /**
     * 设置Hash缓存
     *
     * @param key   缓存key
     * @param field Hash字段
     * @param value Hash值
     */
    public void hSet(String key, String field, String value) {
        hSet(key, field, value, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 设置Hash缓存（指定过期时间）
     *
     * @param key              缓存key
     * @param field            Hash字段
     * @param value            Hash值
     * @param expireTimeSeconds 过期时间（秒）
     */
    public void hSet(String key, String field, String value, long expireTimeSeconds) {
        String fullKey = buildKey(key);
        try {
            redisClient.hashPut(fullKey, field, value);
            if (expireTimeSeconds != NO_EXPIRE) {
                expire(fullKey, expireTimeSeconds);
            }
            log.debug("Hash缓存设置成功：key={}, field={}", fullKey, field);
        } catch (Exception e) {
            log.error("Hash缓存设置失败：key={}, field={}", fullKey, field, e);
        }
    }

    /**
     * 获取Hash缓存
     *
     * @param key   缓存key
     * @param field Hash字段
     * @return Hash值
     */
    public String hGet(String key, String field) {
        String fullKey = buildKey(key);
        try {
            return redisClient.hashGet(fullKey, field);
        } catch (Exception e) {
            log.error("Hash缓存获取失败：key={}, field={}", fullKey, field, e);
            return null;
        }
    }

    /**
     * 删除Hash字段
     *
     * @param key   缓存key
     * @param fields Hash字段集合
     * @return 删除数量
     */
    public long hDelete(String key, Object... fields) {
        String fullKey = buildKey(key);
        try {
            return redisClient.hashDelete(fullKey, java.util.Arrays.copyOf(fields, fields.length, String[].class));
        } catch (Exception e) {
            log.error("Hash字段删除失败：key={}", fullKey, e);
            return 0;
        }
    }

    /**
     * 获取整个Hash
     *
     * @param key 缓存key
     * @return Hash map
     */
    public Map<Object, Object> hGetAll(String key) {
        String fullKey = buildKey(key);
        try {
            return redisClient.hashEntries(fullKey);
        } catch (Exception e) {
            log.error("获取Hash失败：key={}", fullKey, e);
            return Map.of();
        }
    }

    // ==================== List操作 ====================

    /**
     * 向List左侧添加元素
     *
     * @param key   缓存key
     * @param values 元素集合
     * @return List长度
     */
    public long lPush(String key, String... values) {
        String fullKey = buildKey(key);
        try {
            return redisClient.leftPushAll(fullKey, values);
        } catch (Exception e) {
            log.error("List左侧添加失败：key={}", fullKey, e);
            return 0;
        }
    }

    /**
     * 向List右侧添加元素
     *
     * @param key   缓存key
     * @param values 元素集合
     * @return List长度
     */
    public long rPush(String key, String... values) {
        String fullKey = buildKey(key);
        try {
            return redisClient.rightPushAll(fullKey, values);
        } catch (Exception e) {
            log.error("List右侧添加失败：key={}", fullKey, e);
            return 0;
        }
    }

    /**
     * 获取List范围元素
     *
     * @param key   缓存key
     * @param start 开始位置
     * @param end   结束位置
     * @return 元素列表
     */
    public List<String> lRange(String key, long start, long end) {
        String fullKey = buildKey(key);
        try {
            return redisClient.listRange(fullKey, start, end);
        } catch (Exception e) {
            log.error("获取List范围失败：key={}", fullKey, e);
            return List.of();
        }
    }

    // ==================== Set操作 ====================

    /**
     * 向Set添加元素
     *
     * @param key   缓存key
     * @param values 元素集合
     * @return 添加成功数量
     */
    public long sAdd(String key, String... values) {
        String fullKey = buildKey(key);
        try {
            return redisClient.setAdd(fullKey, values);
        } catch (Exception e) {
            log.error("Set添加失败：key={}", fullKey, e);
            return 0;
        }
    }

    /**
     * 获取Set所有元素
     *
     * @param key 缓存key
     * @return 所有元素
     */
    public Set<String> sMembers(String key) {
        String fullKey = buildKey(key);
        try {
            return redisClient.setMembers(fullKey);
        } catch (Exception e) {
            log.error("获取Set元素失败：key={}", fullKey, e);
            return Set.of();
        }
    }

    /**
     * 判断Set中是否包含元素
     *
     * @param key   缓存key
     * @param value 元素
     * @return 是否包含
     */
    public boolean sIsMember(String key, String value) {
        String fullKey = buildKey(key);
        try {
            return redisClient.setIsMember(fullKey, value);
        } catch (Exception e) {
            log.error("检查Set成员失败：key={}", fullKey, e);
            return false;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 构建完整的缓存key
     *
     * @param key 原始key
     * @return 完整key（带前缀）
     */
    private String buildKey(String key) {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("缓存key不能为空");
        }
        if (key.startsWith(CACHE_PREFIX)) {
            return key;
        }
        return CACHE_PREFIX + key;
    }

    /**
     * 模糊查询key
     *
     * @param pattern 匹配模式
     * @return 匹配的key集合
     */
    public Set<String> keys(String pattern) {
        try {
            String fullPattern = pattern.startsWith(CACHE_PREFIX) ? pattern : CACHE_PREFIX + pattern;
            return redisClient.keys(fullPattern);
        } catch (Exception e) {
            log.error("模糊查询key失败：pattern={}", pattern, e);
            return Set.of();
        }
    }

    /**
     * 清空所有缓存（谨慎使用）
     *
     * @return 清空的key数量
     */
    public long clearAll() {
        try {
            Set<String> keys = keys("*");
            if (!keys.isEmpty()) {
                return delete(keys);
            }
            return 0;
        } catch (Exception e) {
            log.error("清空所有缓存失败", e);
            return 0;
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public CacheStats getStats() {
        try {
            Set<String> allKeys = keys("*");
            long totalKeys = allKeys.size();
            long totalMemory = 0; // Redis内存使用情况需要另外获取

            return new CacheStats(totalKeys, totalMemory);
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return new CacheStats(0, 0);
        }
    }

    /**
     * 缓存统计信息
     */
    public record CacheStats(long totalKeys, long totalMemory) {
    }
}
