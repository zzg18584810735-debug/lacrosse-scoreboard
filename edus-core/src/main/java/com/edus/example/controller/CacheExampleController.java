package com.edus.example.controller;

import com.edus.aop.annotation.Cache;
import com.edus.common.components.CacheManager;
import com.edus.common.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存使用示例控制器
 * 演示CacheManager和@Cache注解的使用方法
 *
 * @author edus
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheExampleController {

    private final CacheManager cacheManager;

    // ==================== 基础缓存操作示例 ====================

    /**
     * 示例1：基础缓存操作
     */
    @PostMapping("/basic/{key}")
    public Result<String> basicCache(@PathVariable String key, @RequestBody String value) {
        // 设置缓存（默认1小时过期）
        cacheManager.set(key, value);

        // 获取缓存
        String cachedValue = cacheManager.get(key);

        return Result.ok("缓存设置成功，值为：" + cachedValue);
    }

    /**
     * 示例2：设置带过期时间的缓存
     */
    @PostMapping("/expire/{key}")
    public Result<String> expireCache(@PathVariable String key, @RequestBody String value) {
        // 设置缓存，5分钟过期
        cacheManager.set(key, value, 300);

        // 获取剩余过期时间
        long expireTime = cacheManager.getExpire(key);

        return Result.ok("缓存设置成功，剩余过期时间：" + expireTime + "秒");
    }

    /**
     * 示例3：删除缓存
     */
    @DeleteMapping("/{key}")
    public Result<Boolean> deleteCache(@PathVariable String key) {
        // 删除缓存
        boolean deleted = cacheManager.delete(key);

        // 检查是否还存在
        boolean exists = cacheManager.exists(key);

        return Result.ok("缓存删除：" + deleted + "，是否存在：" + exists);
    }

    // ==================== Hash操作示例 ====================

    /**
     * 示例4：Hash缓存操作
     */
    @PostMapping("/hash/{key}")
    public Result<String> hashCache(@PathVariable String key) {
        String hashKey = "user:" + key;

        // 设置Hash字段
        cacheManager.hSet(hashKey, "name", "张三");
        cacheManager.hSet(hashKey, "age", "25");
        cacheManager.hSet(hashKey, "city", "北京");

        // 获取Hash字段
        String name = cacheManager.hGet(hashKey, "name");

        // 获取整个Hash
        Map<Object, Object> userMap = cacheManager.hGetAll(hashKey);

        return Result.ok("Hash缓存操作，用户名：" + name + "，完整信息：" + userMap);
    }

    /**
     * 示例5：批量操作
     */
    @PostMapping("/batch")
    public Result<Long> batchOperation() {
        // 批量设置缓存
        for (int i = 0; i < 10; i++) {
            cacheManager.set("batch:key:" + i, "value:" + i);
        }

        // 模糊查询key
        Set<String> keys = cacheManager.keys("batch:key:*");

        // 批量删除
        long deletedCount = cacheManager.delete(keys);

        return Result.ok("批量删除了" + deletedCount + "个缓存");
    }

    // ==================== 注解方式缓存示例 ====================

    /**
     * 示例6：使用@Cache注解 - 基础用法
     */
    @Cache(key = "user:{1}", expire = 600)
    @GetMapping("/user/{id}")
    public Result<String> getUserById(@PathVariable Long id) {
        log.info("查询用户信息，id={}", id);
        // 模拟数据库查询
        String userInfo = "用户信息-" + id;
        return Result.ok(userInfo);
    }

    /**
     * 示例7：使用@Cache注解 - 自定义过期时间
     */
    @Cache(key = "config:system", expire = -1)  // 永久缓存
    @GetMapping("/config")
    public Result<String> getSystemConfig() {
        log.info("获取系统配置");
        // 模拟配置查询
        return Result.ok("系统配置信息");
    }

    /**
     * 示例8：使用@Cache注解 - 带条件缓存
     */
    @Cache(key = "order:{1}:{2}", expire = 1800)
    @GetMapping("/order/{userId}/{orderId}")
    public Result<String> getOrder(@PathVariable Long userId, @PathVariable Long orderId) {
        log.info("查询订单，用户ID={}，订单ID={}", userId, orderId);
        // 模拟订单查询
        return Result.ok("订单信息-" + userId + "-" + orderId);
    }

    // ==================== 实际业务场景示例 ====================

    /**
     * 示例9：缓存用户Session
     */
    @PostMapping("/session/{userId}")
    public Result<String> cacheUserSession(@PathVariable Long userId) {
        String sessionKey = "session:user:" + userId;
        String sessionData = "token:abc123,loginTime:" + System.currentTimeMillis();

        // 缓存用户会话，30分钟过期
        cacheManager.set(sessionKey, sessionData, 1800);

        return Result.ok("用户会话已缓存");
    }

    /**
     * 示例10：缓存热点数据
     */
    @GetMapping("/hot-data")
    public Result<List<String>> getHotData() {
        String cacheKey = "hot:data:list";

        // 尝试从缓存获取
        String cachedData = cacheManager.get(cacheKey);
        if (cachedData != null) {
            return Result.ok(List.of("从缓存获取：" + cachedData));
        }

        // 模拟数据库查询
        List<String> hotData = List.of("热点数据1", "热点数据2", "热点数据3");

        // 缓存10分钟
        cacheManager.set(cacheKey, String.join(",", hotData), 600);

        return Result.ok(hotData);
    }

    /**
     * 示例11：缓存计数器
     */
    @PostMapping("/counter/{key}")
    public Result<Long> incrementCounter(@PathVariable String key) {
        String counterKey = "counter:" + key;

        // 获取当前计数
        String currentStr = cacheManager.get(counterKey);
        long current = currentStr != null ? Long.parseLong(currentStr) : 0;

        // 增加计数
        current++;
        cacheManager.set(counterKey, String.valueOf(current), 3600); // 1小时过期

        return Result.ok("计数器值：" + current);
    }

    /**
     * 示例12：缓存统计信息
     */
    @GetMapping("/stats")
    public Result<CacheManager.CacheStats> getCacheStats() {
        CacheManager.CacheStats stats = cacheManager.getStats();
        return Result.ok(stats);
    }

    /**
     * 示例13：清空所有缓存（谨慎使用）
     */
    @DeleteMapping("/clear-all")
    public Result<Long> clearAllCache() {
        long clearedCount = cacheManager.clearAll();
        return Result.ok("已清空" + clearedCount + "个缓存");
    }

    // ==================== List操作示例 ====================

    /**
     * 示例14：List缓存操作
     */
    @PostMapping("/list/{key}")
    public Result<List<String>> listCache(@PathVariable String key) {
        String listKey = "list:" + key;

        // 向List添加元素
        cacheManager.lPush(listKey, "元素1", "元素2", "元素3");

        // 获取List元素
        List<String> elements = cacheManager.lRange(listKey, 0, -1);

        return Result.ok(elements);
    }

    // ==================== Set操作示例 ====================

    /**
     * 示例15：Set缓存操作
     */
    @PostMapping("/set/{key}")
    public Result<Set<String>> setCache(@PathVariable String key) {
        String setKey = "set:" + key;

        // 向Set添加元素
        cacheManager.sAdd(setKey, "用户1", "用户2", "用户3");

        // 检查成员是否存在
        boolean isMember = cacheManager.sIsMember(setKey, "用户1");

        // 获取所有成员
        Set<String> members = cacheManager.sMembers(setKey);

        return Result.ok("是否包含用户1：" + isMember + "，所有成员：" + members);
    }
}
