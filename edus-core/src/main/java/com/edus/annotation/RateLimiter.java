package com.edus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 基于Redis ZSet + Lua脚本实现滑动窗口限流
 *
 * <h3>功能说明：</h3>
 * <ul>
 *   <li>支持多种限流策略：全局限流、基于IP限流、基于用户限流</li>
 *   <li>使用Redis ZSet + Lua脚本实现原子性滑动窗口限流操作</li>
 *   <li>自动返回429状态码当触发限流时</li>
 *   <li>限流数据自动过期，无需手动清理</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 *
 * <h4>1. 基础用法 - 全局限流</h4>
 * <pre>
 * // 所有用户在60秒内总共只能访问100次
 * {@code
 * @RateLimiter(timeout = 60, limit = 100)
 * @GetMapping("/api/data")
 * public Result getData() {
 *     return Result.ok(data);
 * }
 * }
 * </pre>
 *
 * <h4>2. 基于IP限流</h4>
 * <pre>
 * // 同一个IP在10秒内只能访问5次
 * {@code
 * @RateLimiter(timeout = 10, limit = 5, limitType = RateLimiterType.IP)
 * @GetMapping("/sensitive-api")
 * public Result sensitiveApi() {
 *     return Result.ok(data);
 * }
 * }
 * </pre>
 *
 * <h4>3. 基于用户限流</h4>
 * <pre>
 * // 同一个用户在30秒内只能访问10次（需要登录）
 * {@code
 * @RateLimiter(timeout = 30, limit = 10, limitType = RateLimiterType.USER)
 * @GetMapping("/user-api")
 * public Result userApi() {
 *     return Result.ok(data);
 * }
 * }
 * </pre>
 *
 * <h4>4. 自定义限流Key</h4>
 * <pre>
 * // 使用自定义的限流key前缀
 * {@code
 * @RateLimiter(key = "custom_api", timeout = 60, limit = 20)
 * @GetMapping("/custom-key")
 * public Result customKeyLimiter() {
 *     return Result.ok(data);
 * }
 * }
 * </pre>
 *
 * <h4>5. 登录接口限流 - 防止暴力破解</h4>
 * <pre>
 * // 同一IP在5分钟内只能尝试3次登录
 * {@code
 * @RateLimiter(key = "login", timeout = 300, limit = 3, limitType = RateLimiterType.IP)
 * @PostMapping("/login")
 * public Result login(@RequestBody LoginDto dto) {
 *     return Result.ok(token);
 * }
 * }
 * </pre>
 *
 * <h4>6. 高频接口限流</h4>
 * <pre>
 * // 同一用户在1秒内只能访问10次
 * {@code
 * @RateLimiter(timeout = 1, limit = 10, limitType = RateLimiterType.USER)
 * @GetMapping("/high-frequency")
 * public Result highFrequencyLimiter() {
 *     return Result.ok(data);
 * }
 * }
 * </pre>
 *
 * <h3>参数说明：</h3>
 * <ul>
 *   <li><b>key</b>: 限流key的前缀，默认使用"类名:方法名:标识符"格式</li>
 *   <li><b>timeout</b>: 滑动限流时间窗口（秒），默认60秒</li>
 *   <li><b>limit</b>: 在任意timeout滑动窗口内最多允许的请求次数，默认100次</li>
 *   <li><b>limitType</b>: 限流类型，默认全局限流（DEFAULT）</li>
 * </ul>
 *
 * <h3>Redis数据格式：</h3>
 * <pre>
 * Key格式: rate_limit:{类名}:{方法名}:{标识符}
 * 示例:
 *   - rate_limit:UserController:login:192.168.1.100
 *   - rate_limit:OrderController:create:12345
 * </pre>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>确保Redis服务正常运行</li>
 *   <li>USER类型限流需要用户先登录</li>
 *   <li>限流触发时会抛出BusinessException，返回429状态码</li>
 *   <li>合理设置timeout和limit参数，避免过于严格或宽松</li>
 * </ul>
 *
 * @author edus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * 限流key的前缀，默认使用方法全路径
     */
    String key() default "";

    /**
     * 滑动限流时间窗口（秒）
     * 在任意连续窗口内，请求次数不能超过limit
     */
    int timeout() default 60;

    /**
     * 限流次数
     * 在任意timeout滑动窗口内，最多允许limit次请求
     */
    int limit() default 100;

    /**
     * 限流类型
     */
    RateLimiterType limitType() default RateLimiterType.DEFAULT;

    /**
     * 限流类型枚举
     */
    enum RateLimiterType {
        /**
         * 默认策略，全局限流
         */
        DEFAULT,

        /**
         * 根据请求者IP进行限流
         */
        IP,

        /**
         * 根据用户进行限流（需要用户登录）
         */
        USER
    }
}
