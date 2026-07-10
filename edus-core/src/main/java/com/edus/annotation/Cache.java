package com.edus.annotation;

import java.lang.annotation.*;

/**
 * 简单缓存注解
 * 用于方法级别的缓存，简化缓存操作
 *
 * <h3>使用示例：</h3>
 *
 * <h4>1. 基础用法 - 使用方法名作为key</h4>
 * <pre>
 * {@code
 * @Cache(expire = 300)  // 缓存5分钟
 * public User getUserById(Long id) {
 *     return userMapper.selectById(id);
 * }
 * }
 * </pre>
 *
 * <h4>2. 自定义key</h4>
 * <pre>
 * {@code
 * @Cache(key = "user:{1}", expire = 600)  // key格式：user:123
 * public User getUserById(Long id) {
 *     return userMapper.selectById(id);
 * }
 * }
 * </pre>
 *
 * <h4>3. 多参数key</h4>
 * <pre>
 * {@code
 * @Cache(key = "order:{1}:{2}", expire = 1800)  // key格式：order:userId:orderId
 * public Order getOrder(Long userId, Long orderId) {
 *     return orderMapper.selectById(userId, orderId);
 * }
 * }
 * </pre>
 *
 * <h4>4. 不设置过期时间</h4>
 * <pre>
 * {@code
 * @Cache(key = "config:system")  // 永久缓存
 * public SystemConfig getSystemConfig() {
 *     return configMapper.getSystemConfig();
 * }
 * }
 * </pre>
 *
 * <h3>key参数说明：</h3>
 * <ul>
 *   <li>{0} - 第一个参数</li>
 *   <li>{1} - 第二个参数</li>
 *   <li>{n} - 第n+1个参数</li>
 *   <li>默认使用方法名作为key</li>
 * </ul>
 *
 * @author edus
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {

    /**
     * 缓存key，支持参数占位符
     * 例如：user:{1} 表示使用第一个参数作为user:后的值
     */
    String key() default "";

    /**
     * 过期时间（秒）
     * 默认3600秒（1小时），-1表示永不过期
     */
    long expire() default 3600;

    /**
     * 缓存条件，支持SpEL表达式
     * 例如：#id > 0 表示只缓存id大于0的情况
     */
    String condition() default "";
}