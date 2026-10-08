# Apifox 测试指南

本文档用于测试 edus-server 的登录、用户、监控、限流、幂等、XSS、SQL 安全等示例能力。

## 1. 准备环境

### 1.1 初始化数据库

先在 MySQL 中执行：

```sql
source db/user_demo.sql;
source db/operation_log.sql;
```

也可以把两个 SQL 文件内容复制到数据库工具中执行。

确认数据：

```sql
SELECT * FROM users;
SELECT * FROM demo;
SELECT * FROM operation_logs ORDER BY id DESC LIMIT 10;
```

### 1.2 启动服务

默认 dev 环境：

```bash
mvn -pl edus-core spring-boot:run
```

服务基础地址：

```text
http://localhost:8080/api
```

注意：部分示例 Controller 自己又带了 `/api` 前缀，所以完整路径会出现 `/api/api/...`。

## 2. Apifox 环境变量

在 Apifox 新建环境，例如 `edus-local`。

建议配置：

| 变量名 | 值 |
| --- | --- |
| `baseUrl` | `http://localhost:8080/api` |
| `token` | 留空，登录后自动写入 |
| `monitorToken` | dev 填 `edus-dev-monitor-token`，prod 填 `edus.monitor.access-token` 对应值 |

后续请求统一使用：

```text
{{baseUrl}}
```

## 3. 登录接口

测试用户的登录明文密码是 `123456`，数据库 `users.password` 字段存储的是 Hutool BCrypt 哈希。

### 3.1 请求

```http
POST {{baseUrl}}/auth/login
Content-Type: application/json
```

Body:

```json
{
  "username": "admin",
  "password": "123456"
}
```

说明：创建用户时请求里传明文密码，服务端会使用 Hutool BCrypt 加密后入库，响应中不会返回密码。

### 3.2 预期响应

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxx",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "admin"
  }
}
```

### 3.3 Apifox 提取 token

登录成功后，需要把响应中的 `data.token` 保存到 Apifox 环境变量 `token`，这样后面的接口才能直接使用。

操作步骤：

1. 打开登录接口 `POST {{baseUrl}}/auth/login`。
2. 切到「后置操作」。
3. 添加「自定义脚本」。
4. 粘贴下面脚本。
5. 发送登录请求。
6. 打开右上角当前环境 `edus-local`，确认 `token` 已经有值。

```javascript
const json = pm.response.json();

pm.test("登录成功", function () {
  pm.expect(json.code).to.eql(200);
  pm.expect(json.data).to.have.property("token");
});

if (json?.data?.token) {
  pm.environment.set("token", json.data.token);
  pm.environment.set("tokenType", json.data.tokenType || "Bearer");
}
```

保存成功后，Apifox 环境变量里会有：

| 变量名 | 示例 |
| --- | --- |
| `token` | `eyJ0eXAiOiJKV1Qi...` |
| `tokenType` | `Bearer` |

如果登录接口返回 401、400、429 等错误，脚本不会写入新 token。此时先检查账号密码、数据库数据、Redis 是否正常。

## 4. 需要登录的请求头

除 `/auth/login` 外，大多数接口需要带登录 token。

### 4.1 单个接口添加 Header

在接口的 Headers 中添加：

```http
Authorization: Bearer {{token}}
```

### 4.2 推荐：项目级统一添加 Header

为了不用每个接口重复配置，可以在 Apifox 项目里设置统一鉴权：

1. 打开 Apifox 左侧项目。
2. 进入「项目设置」或接口目录的「认证/鉴权」。
3. 选择 `Bearer Token`。
4. Token 填：

```text
{{token}}
```

Apifox 会自动发送：

```http
Authorization: Bearer {{token}}
```

如果你不使用 Bearer Token 鉴权类型，也可以在项目级 Headers 里手动加：

| Header | Value |
| --- | --- |
| `Authorization` | `Bearer {{token}}` |

### 4.3 验证 token 是否生效

登录后请求：

```http
GET {{baseUrl}}/auth/userInfo
Authorization: Bearer {{token}}
```

预期：

```json
{
  "code": 200,
  "message": "获取用户信息成功",
  "data": {
    "userId": "1",
    "username": "admin"
  }
}
```

当前项目也兼容直接传原始 token：

```http
Authorization: {{token}}
```

但推荐统一使用：

```http
Authorization: Bearer {{token}}
```

## 5. 用户接口测试

### 5.1 查询用户

```http
GET {{baseUrl}}/users/1
Authorization: Bearer {{token}}
```

预期：返回 `admin` 用户。

### 5.2 按用户名查询

```http
GET {{baseUrl}}/users/username/admin
Authorization: Bearer {{token}}
```

### 5.3 分页查询

```http
GET {{baseUrl}}/users/page?page=1&limit=10&keyword=demo
Authorization: Bearer {{token}}
```

### 5.4 创建用户

```http
POST {{baseUrl}}/users
Authorization: Bearer {{token}}
Content-Type: application/json
```

Body:

```json
{
  "username": "apifox_user",
  "password": "123456",
  "email": "apifox@example.com",
  "phone": "13800009999",
  "status": "ACTIVE"
}
```

## 6. 监控接口测试

dev/test 环境默认允许访问。prod 环境建议额外传：

```http
X-Monitor-Token: {{monitorToken}}
```

### 6.1 总览

```http
GET {{baseUrl}}/monitor/overview
Authorization: Bearer {{token}}
```

### 6.2 SQL 统计

```http
GET {{baseUrl}}/monitor/sql-stats
Authorization: Bearer {{token}}
```

### 6.3 异步线程池状态

```http
GET {{baseUrl}}/monitor/async
Authorization: Bearer {{token}}
```

### 6.4 重置 SQL 统计

```http
POST {{baseUrl}}/monitor/sql-stats/reset
Authorization: Bearer {{token}}
```

## 7. 限流测试

接口：

```http
GET {{baseUrl}}/api/rate-limiter/by-ip
Authorization: Bearer {{token}}
```

说明：`{{baseUrl}}` 已经包含 `/api`，示例 Controller 路径是 `/api/rate-limiter`，所以最终是：

```text
http://localhost:8080/api/api/rate-limiter/by-ip
```

快速连续请求超过 5 次，预期返回：

```json
{
  "code": 429,
  "message": "访问过于频繁，请稍后再试"
}
```

HTTP 状态码也应该是 `429`。

## 8. 幂等测试

接口：

```http
POST {{baseUrl}}/api/idempotent/order/create
Authorization: Bearer {{token}}
Content-Type: application/json
```

Body:

```json
"order-demo"
```

5 秒内重复提交，预期第二次返回：

```json
{
  "code": 409,
  "message": "请求冲突"
}
```

HTTP 状态码也应该是 `409`。

## 9. XSS 测试

接口：

```http
POST {{baseUrl}}/users
Authorization: Bearer {{token}}
Content-Type: application/json
```

Body:

```json
{
  "username": "xss_apifox_user",
  "password": "123456",
  "email": "xss-apifox@example.com",
  "phone": "13800008888",
  "status": "<script>alert(1)</script>"
}
```

预期：

- JSON 请求体能被 XSS wrapper 读取并过滤。
- `password` 字段不会被改写。
- 危险脚本内容会被清理或转义。

## 10. SQL 注入防护测试

`@SqlSafe` 只有标注在接口方法上才生效。

如果你要测试，可以临时在用户分页接口上加：

```java
@SqlSafe
@GetMapping("/page")
public Result<PageUtils> getPage(@RequestParam Map<String, Object> params) {
    PageUtils pageUtils = userService.queryPage(params);
    return Result.ok(pageUtils);
}
```

然后请求：

```http
GET {{baseUrl}}/users/page?page=1&limit=10&keyword=' or 1=1 --
Authorization: Bearer {{token}}
```

预期：

```json
{
  "code": 400,
  "message": "检测到SQL注入攻击，请求被拒绝"
}
```

## 11. 操作日志测试

登录接口带有 `@LogOperation`。

执行登录后查询：

```sql
SELECT * FROM operation_logs ORDER BY id DESC LIMIT 10;
```

如果 Redis、MySQL、异步线程池都正常，应能看到登录操作日志。

## 12. 常见问题

### 12.1 为什么接口路径有两个 api？

因为：

```yaml
server:
  servlet:
    context-path: /api
```

同时示例 Controller 又写了：

```java
@RequestMapping("/api/rate-limiter")
```

所以完整路径是：

```text
/api/api/rate-limiter/...
```

### 12.2 登录失败怎么办？

确认 `users` 表里有：

```sql
SELECT id, username, password, status, deleted FROM users WHERE username = 'admin';
```

测试脚本默认：

```text
username = admin
login password = 123456
stored password = BCrypt hash
status = ACTIVE
deleted = 0
```

### 12.3 token 传 Bearer 还是不传？

推荐：

```http
Authorization: Bearer {{token}}
```

当前项目也兼容：

```http
Authorization: {{token}}
```
