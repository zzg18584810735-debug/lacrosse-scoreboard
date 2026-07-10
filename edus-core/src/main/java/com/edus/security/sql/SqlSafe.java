package com.edus.security.sql;

import java.lang.annotation.*;

/**
 * SQL安全检查注解
 * 标注在需要进行SQL注入检查的接口或业务方法上。
 *
 * <h3>作用</h3>
 * <ul>
 *   <li>在方法执行前检查入参中是否包含疑似SQL注入内容。</li>
 *   <li>发现危险内容时记录安全日志。</li>
 *   <li>严格模式下直接拒绝请求，避免恶意参数继续进入业务层或Mapper层。</li>
 * </ul>
 *
 * <h3>适用场景</h3>
 * <ul>
 *   <li>搜索、筛选、排序等用户可控查询参数。</li>
 *   <li>需要拼接LIKE查询的关键字参数。</li>
 *   <li>开放接口、后台管理查询接口、报表查询接口。</li>
 * </ul>
 *
 * <h3>基础用法</h3>
 * <pre>
 * {@code
 * @SqlSafe
 * @GetMapping("/users/search")
 * public Result<?> searchUsers(@RequestParam String keyword) {
 *     return Result.ok(userService.search(keyword));
 * }
 * }
 * </pre>
 *
 * <h3>自定义错误提示</h3>
 * <pre>
 * {@code
 * @SqlSafe(message = "搜索参数包含非法字符")
 * @GetMapping("/orders")
 * public Result<?> listOrders(@RequestParam String keyword) {
 *     return Result.ok(orderService.list(keyword));
 * }
 * }
 * </pre>
 *
 * <h3>非严格模式</h3>
 * <p>
 * 非严格模式只记录告警日志，不阻断方法执行。一般只建议在灰度观察期使用，
 * 生产接口建议保持默认严格模式。
 * </p>
 * <pre>
 * {@code
 * @SqlSafe(strict = false)
 * @GetMapping("/debug/search")
 * public Result<?> debugSearch(@RequestParam String keyword) {
 *     return Result.ok(service.search(keyword));
 * }
 * }
 * </pre>
 *
 * <h3>当前实现说明</h3>
 * <ul>
 *   <li>当前切面拦截方法级 {@code @SqlSafe} 注解。</li>
 *   <li>当前会检查 {@code String} 参数、数组参数中的字符串、集合参数中的字符串。</li>
 *   <li>复杂DTO对象中的字符串字段暂未递归检查，如需检查DTO字段，建议在Controller层拆出关键参数或后续增强切面。</li>
 *   <li>{@link #paramNames()} 为预留配置项，当前切面暂未按参数名过滤。</li>
 * </ul>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>该注解是输入安全检查，不替代MyBatis/MyBatis-Plus的参数绑定。</li>
 *   <li>SQL查询仍应优先使用 {@code #{}} 参数绑定，避免使用字符串拼接SQL。</li>
 *   <li>LIKE查询参数建议配合 {@link SqlInjectionUtil#safeLikeParameter(String)} 转义通配符。</li>
 *   <li>该注解可能误判包含SQL关键字的正常文本，必要时可调整检测规则或在特定接口关闭严格模式。</li>
 * </ul>
 *
 * @author edus
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SqlSafe {

    /**
     * 是否启用严格模式
     * 严格模式下，检测到SQL注入会抛出异常并阻断请求。
     * 非严格模式只记录告警日志，不阻断执行。
     */
    boolean strict() default true;

    /**
     * 需要检查的参数名数组
     * 如果为空，则检查所有字符串参数。
     *
     * <p>注意：当前切面暂未实现按参数名过滤，该字段为后续增强预留。</p>
     */
    String[] paramNames() default {};

    /**
     * 严格模式下检测到SQL注入时返回的错误消息。
     */
    String message() default "检测到SQL注入攻击，请求被拒绝";

    /**
     * 检测到疑似SQL注入时是否记录告警日志。
     */
    boolean logWarning() default true;
}
