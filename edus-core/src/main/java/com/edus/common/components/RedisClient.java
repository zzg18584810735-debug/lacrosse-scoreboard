package com.edus.common.components;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用操作工具。
 * <p>
 * 本工具使用 {@link StringRedisTemplate}，因此 key、value、Hash 字段以及集合元素均按字符串存储。
 * 它不会自动为 key 添加前缀；业务缓存请继续优先使用 {@code CacheManager}，以保留其 {@code cache:}
 * 命名空间和默认过期策略。
 * </p>
 *
 * @author edus
 */
@Component
public class RedisClient {

    private final StringRedisTemplate redisTemplate;

    public RedisClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入字符串值，不设置过期时间。
     *
     * @param key Redis key
     * @param value 要存储的字符串值
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(requireKey(key), value);
    }

    /**
     * 写入字符串值并设置过期时间。
     *
     * @param key Redis key
     * @param value 要存储的字符串值
     * @param timeout 过期时长，必须大于 0
     */
    public void set(String key, String value, Duration timeout) {
        redisTemplate.opsForValue().set(requireKey(key), value, requirePositive(timeout));
    }

    /**
     * 仅当 key 不存在时写入值，常用于简单分布式锁或幂等标记。
     *
     * @param key Redis key
     * @param value 要存储的字符串值
     * @param timeout 过期时长，必须大于 0；锁场景必须设置过期时间
     * @return 写入成功返回 {@code true}，key 已存在返回 {@code false}
     */
    public boolean setIfAbsent(String key, String value, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(requireKey(key), value, requirePositive(timeout)));
    }

    /**
     * 仅当 key 不存在时写入值，不设置过期时间。
     *
     * @param key Redis key
     * @param value 要存储的字符串值
     * @return 写入成功返回 {@code true}，key 已存在返回 {@code false}
     */
    public boolean setIfAbsent(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(requireKey(key), value));
    }

    /**
     * 获取字符串值。
     *
     * @param key Redis key
     * @return key 不存在时返回 {@code null}
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(requireKey(key));
    }

    /**
     * 删除一个或多个 key。
     *
     * @param keys 待删除的 key
     * @return 实际删除的 key 数量
     */
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        keys.forEach(this::requireKey);
        Long deleted = redisTemplate.delete(keys);
        return deleted == null ? 0 : deleted;
    }

    /**
     * 删除单个 key。
     *
     * @param key Redis key
     * @return key 存在且删除成功时返回 {@code true}
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(requireKey(key)));
    }

    /**
     * 判断 key 是否存在。
     *
     * @param key Redis key
     * @return key 存在返回 {@code true}
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(requireKey(key)));
    }

    /**
     * 设置 key 的过期时间。
     *
     * @param key Redis key
     * @param timeout 过期时长，必须大于 0
     * @return key 存在且设置成功时返回 {@code true}
     */
    public boolean expire(String key, Duration timeout) {
        return Boolean.TRUE.equals(redisTemplate.expire(requireKey(key), requirePositive(timeout)));
    }

    /**
     * 获取 key 的剩余有效期。
     *
     * @param key Redis key
     * @return 剩余秒数；{@code -1} 表示永不过期，{@code -2} 表示 key 不存在
     */
    public long getExpireSeconds(String key) {
        Long seconds = redisTemplate.getExpire(requireKey(key), TimeUnit.SECONDS);
        return seconds == null ? -2 : seconds;
    }

    /**
     * 对数值字符串执行原子递增。
     *
     * @param key Redis key
     * @param delta 递增值，可为负数
     * @return 递增后的值
     */
    public long increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(requireKey(key), delta);
        return value == null ? 0 : value;
    }

    /**
     * 写入 Hash 字段。
     *
     * @param key Hash key
     * @param field 字段名
     * @param value 字段值
     */
    public void hashPut(String key, String field, String value) {
        redisTemplate.opsForHash().put(requireKey(key), requireKey(field), value);
    }

    /**
     * 批量写入 Hash 字段。
     *
     * @param key Hash key
     * @param values 字段和值
     */
    public void hashPutAll(String key, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        values.forEach((field, value) -> requireKey(field));
        redisTemplate.opsForHash().putAll(requireKey(key), values);
    }

    /**
     * 获取 Hash 字段值。
     *
     * @param key Hash key
     * @param field 字段名
     * @return 字段不存在时返回 {@code null}
     */
    public String hashGet(String key, String field) {
        Object value = redisTemplate.opsForHash().get(requireKey(key), requireKey(field));
        return value == null ? null : value.toString();
    }

    /**
     * 获取整个 Hash。
     *
     * @param key Hash key
     * @return 字段和值的映射；key 不存在时返回空 Map
     */
    public Map<Object, Object> hashEntries(String key) {
        return redisTemplate.opsForHash().entries(requireKey(key));
    }

    /**
     * 删除 Hash 字段。
     *
     * @param key Hash key
     * @param fields 待删除字段
     * @return 实际删除字段数
     */
    public long hashDelete(String key, String... fields) {
        if (fields == null || fields.length == 0) {
            return 0;
        }
        for (String field : fields) {
            requireKey(field);
        }
        Long deleted = redisTemplate.opsForHash().delete(requireKey(key), (Object[]) fields);
        return deleted == null ? 0 : deleted;
    }

    /**
     * 从 List 左侧推入元素。
     *
     * @param key List key
     * @param value 元素值
     * @return 推入后的 List 长度
     */
    public long leftPush(String key, String value) {
        Long size = redisTemplate.opsForList().leftPush(requireKey(key), value);
        return size == null ? 0 : size;
    }

    /**
     * 从 List 左侧批量推入元素。
     *
     * @param key List key
     * @param values 元素值
     * @return 推入后的 List 长度
     */
    public long leftPushAll(String key, String... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        Long size = redisTemplate.opsForList().leftPushAll(requireKey(key), values);
        return size == null ? 0 : size;
    }

    /**
     * 从 List 右侧批量推入元素。
     *
     * @param key List key
     * @param values 元素值
     * @return 推入后的 List 长度
     */
    public long rightPushAll(String key, String... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        Long size = redisTemplate.opsForList().rightPushAll(requireKey(key), values);
        return size == null ? 0 : size;
    }

    /**
     * 从 List 右侧弹出元素。
     *
     * @param key List key
     * @return List 为空或不存在时返回 {@code null}
     */
    public String rightPop(String key) {
        return redisTemplate.opsForList().rightPop(requireKey(key));
    }

    /**
     * 获取 List 指定范围的元素，Redis 下标两端均包含。
     *
     * @param key List key
     * @param start 起始下标
     * @param end 结束下标，使用 {@code -1} 表示最后一个元素
     * @return 元素列表
     */
    public List<String> listRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(requireKey(key), start, end);
    }

    /**
     * 向 Set 添加一个或多个元素。
     *
     * @param key Set key
     * @param values 元素值
     * @return 实际新增元素数量
     */
    public long setAdd(String key, String... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        Long added = redisTemplate.opsForSet().add(requireKey(key), values);
        return added == null ? 0 : added;
    }

    /**
     * 获取 Set 全部元素。
     *
     * @param key Set key
     * @return key 不存在时返回空 Set
     */
    public Set<String> setMembers(String key) {
        return redisTemplate.opsForSet().members(requireKey(key));
    }

    /**
     * 判断 Set 是否包含元素。
     *
     * @param key Set key
     * @param value 元素值
     * @return 包含时返回 {@code true}
     */
    public boolean setIsMember(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(requireKey(key), value));
    }

    /**
     * 按模式查询 key。Redis 的 {@code KEYS} 会阻塞 Redis，生产环境仅限小数据集或运维场景使用。
     *
     * @param pattern Redis glob 模式，例如 {@code cache:*}
     * @return 匹配的 key 集合
     */
    public Set<String> keys(String pattern) {
        return redisTemplate.keys(requireKey(pattern));
    }

    /**
     * 检查并规范化 Redis key/Hash field，避免误操作空 key。
     */
    private String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Redis key 或 field 不能为空");
        }
        return key;
    }

    /**
     * 校验过期时间，禁止写入立即过期或无意义的时长。
     */
    private Duration requirePositive(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Redis 过期时间必须大于 0");
        }
        return timeout;
    }
}
