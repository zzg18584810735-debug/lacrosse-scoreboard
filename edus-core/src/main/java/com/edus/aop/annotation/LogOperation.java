package com.edus.aop.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标记需要记录操作日志的方法，自动记录用户操作行为
 *
 * <h3>功能说明：</h3>
 * <ul>
 *   <li>自动记录用户信息、请求信息、响应结果等</li>
 *   <li>异步保存日志，不影响接口响应速度</li>
 *   <li>自动记录执行时长和异常信息</li>
 *   <li>支持敏感参数过滤，避免记录密码等敏感信息</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 *
 * <h4>1. 基础用法 - 记录用户登录</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "LOGIN",
 *     module = "用户管理",
 *     description = "用户登录系统"
 * )
 * @PostMapping("/login")
 * public Result login(@RequestBody LoginDto dto) {
 *     return Result.ok(token);
 * }
 * }
 * </pre>
 *
 * <h4>2. 记录数据创建</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "CREATE",
 *     module = "用户管理",
 *     description = "创建新用户"
 * )
 * @PostMapping("/user")
 * public Result createUser(@RequestBody UserDto dto) {
 *     userService.save(dto);
 *     return Result.ok();
 * }
 * }
 * </pre>
 *
 * <h4>3. 记录数据更新</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "UPDATE",
 *     module = "用户管理",
 *     description = "更新用户信息"
 * )
 * @PutMapping("/user/{id}")
 * public Result updateUser(@PathVariable Long id, @RequestBody UserDto dto) {
 *     userService.updateById(id, dto);
 *     return Result.ok();
 * }
 * }
 * </pre>
 *
 * <h4>4. 记录数据删除</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "DELETE",
 *     module = "用户管理",
 *     description = "删除用户"
 * )
 * @DeleteMapping("/user/{id}")
 * public Result deleteUser(@PathVariable Long id) {
 *     userService.removeById(id);
 *     return Result.ok();
 * }
 * }
 * </pre>
 *
 * <h4>5. 不记录请求参数（敏感操作）</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "UPDATE",
 *     module = "用户管理",
 *     description = "修改用户密码",
 *     logParams = false  // 不记录请求参数，避免密码泄露
 * )
 * @PutMapping("/user/password")
 * public Result updatePassword(@RequestBody PasswordDto dto) {
 *     userService.updatePassword(dto);
 *     return Result.ok();
 * }
 * }
 * </pre>
 *
 * <h4>6. 记录响应结果</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "EXPORT",
 *     module = "数据管理",
 *     description = "导出用户数据",
 *     logResponse = true  // 记录导出结果
 * )
 * @GetMapping("/user/export")
 * public Result exportUsers() {
 *     return Result.ok(userService.export());
 * }
 * }
 * </pre>
 *
 * <h4>7. 查询操作记录</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "QUERY",
 *     module = "用户管理",
 *     description = "查询用户列表"
 * )
 * @GetMapping("/user/list")
 * public Result getUserList() {
 *     return Result.ok(userService.list());
 * }
 * }
 * </pre>
 *
 * <h4>8. 批量操作记录</h4>
 * <pre>
 * {@code
 * @LogOperation(
 *     operationType = "BATCH_DELETE",
 *     module = "用户管理",
 *     description = "批量删除用户"
 * )
 * @DeleteMapping("/user/batch")
 * public Result batchDeleteUsers(@RequestBody List<Long> ids) {
 *     userService.removeByIds(ids);
 *     return Result.ok();
 * }
 * }
 * </pre>
 *
 * <h3>常用操作类型：</h3>
 * <ul>
 *   <li><b>LOGIN</b> - 用户登录/登出</li>
 *   <li><b>CREATE</b> - 创建数据</li>
 *   <li><b>UPDATE</b> - 更新数据</li>
 *   <li><b>DELETE</b> - 删除数据</li>
 *   <li><b>QUERY</b> - 查询数据</li>
 *   <li><b>EXPORT</b> - 导出数据</li>
 *   <li><b>IMPORT</b> - 导入数据</li>
 *   <li><b>BATCH_CREATE</b> - 批量创建</li>
 *   <li><b>BATCH_UPDATE</b> - 批量更新</li>
 *   <li><b>BATCH_DELETE</b> - 批量删除</li>
 * </ul>
 *
 * <h3>常用模块名称：</h3>
 * <ul>
 *   <li>用户管理</li>
 *   <li>角色管理</li>
 *   <li>权限管理</li>
 *   <li>系统管理</li>
 *   <li>日志管理</li>
 *   <li>业务管理</li>
 * </ul>
 *
 * <h3>自动记录的信息：</h3>
 * <ul>
 *   <li>用户ID和用户名（从UserContext获取）</li>
 *   <li>请求方法和URL</li>
 *   <li>客户端IP地址</li>
 *   <li>User-Agent信息</li>
 *   <li>请求参数（可选）</li>
 *   <li>响应结果（可选）</li>
 *   <li>执行时长</li>
 *   <li>操作状态（成功/失败）</li>
 *   <li>错误信息（失败时）</li>
 *   <li>操作时间</li>
 * </ul>
 *
 * <h3>参数说明：</h3>
 * <ul>
 *   <li><b>operationType</b>: 操作类型，如LOGIN/CREATE/UPDATE/DELETE等</li>
 *   <li><b>module</b>: 操作模块，如用户管理/系统管理等</li>
 *   <li><b>description</b>: 操作描述，详细描述做了什么操作</li>
 *   <li><b>logParams</b>: 是否记录请求参数，默认true（敏感操作建议设为false）</li>
 *   <li><b>logResponse</b>: 是否记录响应结果，默认false（大数据响应不建议开启）</li>
 * </ul>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>确保UserContext中有正确的用户信息</li>
 *   <li>敏感操作（如修改密码）建议设置logParams=false</li>
 *   <li>大数据响应建议设置logResponse=false，避免数据库存储压力</li>
 *   <li>日志采用异步保存，不影响接口性能</li>
 *   <li>请求参数和响应结果会自动截断，避免过长数据</li>
 * </ul>
 *
 * <h3>数据存储：</h3>
 * <ul>
 *   <li>请求参数最多存储2000字符</li>
 *   <li>响应结果最多存储2000字符</li>
 *   <li>错误信息最多存储1000字符</li>
 *   <li>操作时间精确到秒</li>
 * </ul>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 操作类型
     * 如：LOGIN/CREATE/UPDATE/DELETE/EXPORT/IMPORT等
     */
    String operationType() default "";

    /**
     * 操作模块
     * 如：用户管理/系统管理/权限管理等
     */
    String module() default "";

    /**
     * 操作描述
     * 描述具体做了什么操作
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean logResponse() default false;
}