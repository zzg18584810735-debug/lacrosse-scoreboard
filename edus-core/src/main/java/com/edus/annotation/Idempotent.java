package com.edus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解
 * 用于防止接口重复提交，确保同一个请求多次执行结果一致
 *
 * <h3>功能说明：</h3>
 * <ul>
 *   <li>基于Redis实现请求去重</li>
 *   <li>支持自动去重和手动Token两种模式</li>
 *   <li>可配置防重时间窗口</li>
 *   <li>支持自定义幂等Key</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 *
 * <h4>1. 基础用法 - 基于方法路径自动幂等</h4>
 * <pre>
 * {@code
 * @Idempotent(timeout = 5, message = "订单创建处理中，请勿重复提交")
 * @PostMapping("/order/create")
 * public Result createOrder(@RequestBody OrderDto dto) {
 *     // 5秒内同一个用户不能重复创建订单
 *     return orderService.create(dto);
 * }
 * }
 * </pre>
 *
 * <h4>2. 基于业务ID的幂等</h4>
 * <pre>
 * {@code
 * @Idempotent(key = "#orderId", timeout = 10)
 * @PutMapping("/order/{orderId}")
 * public Result updateOrder(@PathVariable String orderId, @RequestBody OrderDto dto) {
 *     // 基于订单ID的幂等，同一订单10秒内只能更新一次
 *     return orderService.update(orderId, dto);
 * }
 * }
 * </pre>
 *
 * <h4>3. 基于用户ID的幂等</h4>
 * <pre>
 * {@code
 * @Idempotent(keyType = IdempotentKeyType.USER, timeout = 3)
 * @PostMapping("/payment/pay")
 * public Result pay(@RequestBody PaymentDto dto) {
 *     // 3秒内同一用户只能发起一次支付请求
 *     return paymentService.pay(dto);
 * }
 * }
 * </pre>
 *
 * <h4>4. 严格防重模式（更长超时时间）</h4>
 * <pre>
 * {@code
 * @Idempotent(timeout = 60, message = "敏感操作处理中，请勿重复提交")
 * @PostMapping("/user/delete")
 * public Result deleteUser(@RequestBody Long userId) {
 *     // 删除用户操作，60秒内防重
 *     return userService.delete(userId);
 * }
 * }
 * </pre>
 *
 * <h4>5. 高频接口短时间防重</h4>
 * <pre>
 * {@code
 * @Idempotent(timeout = 1, keyType = IdempotentKeyType.USER)
 * @PostMapping("/comment/add")
 * public Result addComment(@RequestBody CommentDto dto) {
 *     // 同一用户1秒内只能添加一条评论
 *     return commentService.add(dto);
 * }
 * }
 * </pre>
 *
 * <h3>参数说明：</h3>
 * <ul>
 *   <li><b>key</b>: 幂等Key，支持SpEL表达式，默认基于请求路径和用户ID</li>
 *   <li><b>timeout</b>: 防重时间窗口（秒），默认5秒</li>
 *   <li><b>keyType</b>: Key类型，DEFAULT/USER/IP/CUSTOM</li>
 *   <li><b>message</b>: 重复提交时的提示信息</li>
 * </ul>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>确保Redis服务正常运行</li>
 *   <li>USER类型幂等需要用户先登录</li>
 *   <li>合理设置timeout时间，避免过长或过短</li>
 *   <li>对于写操作建议使用，读操作无需幂等</li>
 * </ul>
 *
 * @author edus
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等Key，支持SpEL表达式
     * 留空则使用默认Key生成策略（请求路径 + 用户ID/IP）
     */
    String key() default "";

    /**
     * 防重时间窗口（秒）
     * 在这个时间内，相同的请求只能执行一次
     */
    int timeout() default 5;

    /**
     * Key类型
     */
    IdempotentKeyType keyType() default IdempotentKeyType.DEFAULT;

    /**
     * 重复提交时的提示信息
     */
    String message() default "操作处理中，请勿重复提交";

    /**
     * 幂等Key类型枚举
     */
    enum IdempotentKeyType {
        /**
         * 默认策略，基于请求路径 + 用户ID/IP
         */
        DEFAULT,

        /**
         * 基于用户ID（需要用户登录）
         */
        USER,

        /**
         * 基于请求者IP
         */
        IP,

        /**
         * 自定义Key，配合key参数使用
         */
        CUSTOM
    }
}