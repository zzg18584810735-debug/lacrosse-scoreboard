
# 开发规范指南（edus-server）

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。
本规范基于项目 `edus-server` 的实际工程结构、依赖配置与运行环境生成，**当本规范与通用模板冲突时，以本项目实际配置为准**。

---

## 一、项目基本信息

| 项目项         | 内容                                                                 |
|----------------|----------------------------------------------------------------------|
| 项目名称       | `edus-server`（父工程）/ `edus-core`（核心业务模块）                  |
| 用户工作目录   | `D:\ProgramFiles\JetBrains\IdeaProjects\edus-server`                 |
| 操作系统       | Windows 10                                                           |
| 终端 Shell     | PowerShell                                                           |
| 构建工具       | **Maven**（多模块聚合工程：父 `pom` + `edus-core`）                   |
| 运行 JDK       | **JDK 18.0.2**                                                       |
| 编译目标版本   | **Java 17**（`java.version` / `maven.compiler.source` / `target` = 17）|
| 源码编码       | UTF-8（`project.build.sourceEncoding`）                              |
| 主框架         | Spring Boot **3.5.11**（继承 `spring-boot-starter-parent`）           |
| 持久层框架     | **MyBatis-Plus 3.5.7**（`mybatis-plus-spring-boot3-starter`）         |
| 代码作者       | **Administrator**                                                     |
| 注释语言       | **中文**（第一语言）                                                  |
| 规范生成时间   | 2026-10-08                                                           |

> ⚠️ 说明：本地运行环境为 JDK 18.0.2，但工程编译与发布目标固定为 **Java 17**，请勿在代码中使用 Java 18+ 的语法特性（如未命名模式变量等预览特性）。

---

## 二、项目目录结构

```text
edus-server
    ├── .agents
    ├── .codebuddy
    │   └── db
    │       └── lucene
    ├── .feisuan
    │   └── rules
    ├── data
    │   └── dev
    ├── db
    ├── docs
    └── edus-core
        └── src
            ├── main
            │   ├── java
            │   │   └── com
            │   │       └── edus
            │   │           ├── aop
            │   │           │   ├── annotation
            │   │           │   └── aspect
            │   │           ├── common
            │   │           │   ├── components
            │   │           │   ├── enums
            │   │           │   ├── exception
            │   │           │   ├── page
            │   │           │   └── utils
            │   │           ├── config
            │   │           ├── controller
            │   │           │   └── example
            │   │           ├── dto
            │   │           │   ├── req
            │   │           │   └── res
            │   │           ├── entity
            │   │           ├── filter
            │   │           ├── interceptor
            │   │           ├── mapper
            │   │           ├── monitor
            │   │           ├── security
            │   │           │   ├── monitor
            │   │           │   ├── sensitive
            │   │           │   ├── sql
            │   │           │   └── xss
            │   │           └── service
            │   │               └── auth
            │   └── resources
            │       └── static
            │           └── images
            └── test
                └── java
                    └── com
                        └── edus
```

### 目录职责约定

| 目录                                   | 职责说明                                                     |
|----------------------------------------|--------------------------------------------------------------|
| `com.edus.aop.annotation` / `aspect`   | 自定义注解与切面实现（横切逻辑统一放此处）                   |
| `com.edus.common.components`           | 通用组件（如统一响应、上下文持有者等）                       |
| `com.edus.common.enums`                | 全局枚举定义                                                 |
| `com.edus.common.exception`            | 自定义异常与全局异常处理                                     |
| `com.edus.common.page`                 | 统一分页封装                                                 |
| `com.edus.common.utils`                | 通用工具类（优先基于 Hutool 二次封装）                       |
| `com.edus.config`                      | 各类 `@Configuration` 配置类                                 |
| `com.edus.controller`                  | HTTP 接口层（示例代码置于 `controller/example`）             |
| `com.edus.dto.req` / `dto.res`         | 请求 DTO / 响应 DTO                                          |
| `com.edus.entity`                      | 数据库实体（MyBatis-Plus 别名包：`com.edus.entity`）         |
| `com.edus.filter` / `interceptor`      | 过滤器与拦截器                                               |
| `com.edus.mapper`                      | MyBatis-Plus Mapper 接口                                     |
| `com.edus.monitor`                     | 性能与运行监控                                               |
| `com.edus.security.*`                  | 安全模块：监控鉴权、敏感数据、SQL 注入防护、XSS 防护         |
| `com.edus.service`                     | 业务服务层（按业务域分子包，如 `service.auth`）              |
| `src/main/resources/mapper/**/*.xml`   | MyBatis XML 映射文件（与 Mapper 接口对应）                   |

---

## 三、技术栈与依赖规范

### 3.1 核心依赖清单

| 依赖                                     | 版本 / 说明                                        |
|------------------------------------------|----------------------------------------------------|
| `spring-boot-starter-parent`             | 3.5.11（统一依赖版本管理）                         |
| `spring-boot-starter-web`                | Web / REST 支持                                    |
| `spring-boot-starter-validation`         | JSR-303 参数校验（`jakarta.validation.*`）         |
| `mybatis-plus-spring-boot3-starter`      | 3.5.7（Spring Boot 3 专用 starter）                |
| `mysql-connector-j`                      | `runtime` 作用域                                   |
| `lombok`                                 | `optional=true`，打包时排除                        |
| `hutool-all`                             | 5.8.25                                             |
| `spring-boot-starter-data-redis`         | Lettuce 客户端                                     |
| `spring-boot-starter-aop`                | 切面支持                                           |
| `spring-boot-starter-test`               | `test` 作用域                                      |

### 3.2 依赖使用通用规则

1. **禁止引入 JPA / Hibernate**：本项目持久层统一使用 **MyBatis-Plus**，不存在 `Repository` 层。
2. **禁止擅自新增依赖**：新增依赖必须先确认现有依赖（尤其是 Hutool、Spring Boot Starter 系列）是否已能满足，避免重复造轮子与版本冲突。
3. **版本管理**：Spring 生态依赖由父 POM 统一管理，**不要显式写版本号**；第三方依赖（如 MyBatis-Plus、Hutool）才需显式指定版本。
4. **工具类优先使用 Hutool**：字符串、集合、日期、IO、加解密、Bean 拷贝等操作优先使用 `cn.hutool`（`StrUtil`、`CollUtil`、`DateUtil`、`BeanUtil` 等），避免自写重复工具类；自写工具类统一放在 `com.edus.common.utils`。
5. **缓存与并发控制使用 Redis**：缓存、分布式锁、限流等场景使用 `spring-boot-starter-data-redis`（Lettuce 连接池），不要使用本地 `Map` 做跨实例共享状态。
6. **横切逻辑使用 AOP**：日志、权限、限流、耗时统计等统一切面，注解定义放 `com.edus.aop.annotation`，实现放 `com.edus.aop.aspect`。
7. **测试依赖限定作用域**：单元测试仅使用 `spring-boot-starter-test`，测试类置于 `src/test/java/com/edus` 下。
8. **Lombok 打包排除**：`spring-boot-maven-plugin` 已排除 `lombok`，请勿在运行时依赖 Lombok 类。

---

## 四、分层架构规范

| 层级            | 职责说明                             | 开发约束与注意事项                                                                 |
|-----------------|--------------------------------------|------------------------------------------------------------------------------------|
| **Controller**  | 处理 HTTP 请求与响应，定义 API 接口   | 不得直接访问数据库，必须通过 Service 层调用；入参使用 `dto.req`，出参使用 `dto.res` |
| **Service**     | 实现业务逻辑、事务管理与数据校验      | 必须通过 Mapper 层访问数据库；返回 DTO 而非 Entity（除非必要）                      |
| **Mapper**      | 数据库访问与持久化操作                | 继承 `BaseMapper<T>`；复杂 SQL 写在 `resources/mapper/**/*.xml`；避免 N+1 查询       |
| **Entity**      | 映射数据库表结构                      | 不得直接返回给前端（需转换为 DTO）；包名统一为 `com.edus.entity`                    |
| **DTO**         | 请求/响应数据传输对象                 | 请求对象放 `com.edus.dto.req`，响应对象放 `com.edus.dto.res`                        |

### 接口与实现分离

- 所有接口实现类需放在接口所在包下的 `impl` 子包中（如 `com.edus.service.auth.impl`）。

### MyBatis-Plus 使用规范

- Mapper 接口必须继承 `com.baomidou.mybatisplus.core.mapper.BaseMapper<T>`，并标注 `@Mapper`（或由启动类 `@MapperScan` 统一扫描）。
- XML 映射文件路径固定为 `classpath:mapper/**/*.xml`，与 `com.edus.mapper` 下的接口一一对应。
- 实体别名包固定为 `com.edus.entity`，实体类包名不得更改。
- **逻辑删除统一使用 `deleted` 字段**：已删除 = `1`，未删除 = `0`；查询默认自动过滤，禁止手写 `deleted = 0` 条件。
- 主键策略统一为 `id-type: auto`（数据库自增），不得使用 UUID 主键。
- 字段映射采用下划线转驼峰（`map-underscore-to-camel-case: true`），数据库字段命名使用 `snake_case`，Java 字段使用 `lowerCamelCase`。
- 多表关联与复杂条件优先使用 MyBatis-Plus 条件构造器或 XML 动态 SQL，**禁止字符串拼接 SQL**。
- 批量操作使用 `saveBatch` / `updateBatchById` 等批量方法，禁止在循环中逐条访问数据库。

---

## 五、配置与环境规范

### 5.1 Profile 规范

- 激活方式：环境变量 `SPRING_PROFILES_ACTIVE`（默认 `dev`）。
- `application.yml` 只放**通用配置**，环境差异项（连接池大小、Redis 池大小、数据源地址等）必须在各 profile 文件中覆盖。
- 所有可变参数使用 `${ENV_VAR:default}` 占位符形式，禁止硬编码敏感信息（数据库密码、令牌等）。

### 5.2 服务与路径

- 服务端口：`SERVER_PORT`（默认 `8080`）。
- 统一上下文路径：`SERVER_CONTEXT_PATH`（默认 `/api`），**所有接口均带 `/api` 前缀**。

### 5.3 连接池（HikariCP）规范

| 配置项                     | 值 / 说明                       |
|----------------------------|---------------------------------|
| `auto-commit`              | `false`（禁用自动提交）         |
| `connection-timeout`       | 30000 ms                        |
| `validation-timeout`       | 5000 ms                         |
| `idle-timeout`             | 600000 ms                       |
| `max-lifetime`             | 1800000 ms                      |
| `leak-detection-threshold` | 60000 ms                        |
| `maximum-pool-size` / `minimum-idle` / `pool-name` | 在 profile 中按环境覆盖 |

### 5.4 性能监控与安全基线

- 慢 SQL 阈值：`edus.performance.monitoring.slow-sql-threshold = 1000` ms；SQL 超时告警阈值 1000 ms。
- 监控接口需携带请求头 **`X-Monitor-Token`**（`edus.monitor.access-token`），**各环境必须在 profile 中明确配置**。
- `edus.monitor.allow-dev-profile` 默认 `false`，非必要不得在开发环境放开。
- 内部网络访问控制由 `edus.monitor.allow-internal-network` 控制，生产环境需评估后配置。

---

## 六、安全与性能规范

### 输入校验

- 使用 `@Valid` / `@Validated` 与 JSR-303 校验注解（`@NotBlank`、`@Size`、`@NotNull` 等）。
- 注意：Spring Boot 3.x 中校验注解位于 **`jakarta.validation.constraints.*`**（非 `javax.*`）。
- 请求参数一律通过 `dto.req` 接收并校验，禁止直接使用 `Map` 接收业务参数。
- 统一异常处理走 `com.edus.common.exception`，禁止将异常堆栈直接返回给前端。

### 安全防护

- **禁止手动拼接 SQL 字符串**，防止 SQL 注入；SQL 安全相关能力统一在 `com.edus.security.sql` 中实现。
- XSS 防护统一在 `com.edus.security.xss` 中实现，配合 `com.edus.filter` 生效，不得在业务代码中散落实现。
- 敏感数据（手机号、身份证、密码等）脱敏统一在 `com.edus.security.sensitive` 中实现，日志与响应中禁止输出明文敏感信息。
- 监控类接口必须经过 `com.edus.security.monitor` 的令牌校验。

### 事务管理

- `@Transactional` 注解仅用于 **Service 层**方法。
- 避免在循环中频繁提交事务，影响性能。
- 由于连接池已设置 `auto-commit: false`，未标注事务的写操作需确认提交行为。

---

## 七、代码风格规范

### 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `UserServiceImpl`     |
| 方法/变量  | lowerCamelCase       | `saveUser()`          |
| 常量       | UPPER_SNAKE_CASE     | `MAX_LOGIN_ATTEMPTS`  |
| 包名       | 全小写               | `com.edus.service.auth` |
| 数据库字段 | snake_case           | `create_time`         |

### 注释规范

- **注释语言统一使用中文**（用户第一语言）。
- 所有类、方法、字段需添加 **中文 Javadoc** 注释，说明用途、参数与返回值。
- 作者标识统一使用：

```java
/**
 * 用户业务服务实现
 *
 * @author Administrator
 * @since 2026-10-08
 */
```

- 复杂业务逻辑、SQL 特殊处理、性能权衡点必须写明原因注释，禁止无意义注释（如 `// 设置名称`）。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明                     | 示例         | 本项目存放位置        |
|------|------------------------------|--------------|-----------------------|
| DTO  | 数据传输对象                 | `UserDTO`    | `dto.req` / `dto.res` |
| DO   | 数据库实体对象               | `UserDO`     | `entity`              |
| BO   | 业务逻辑封装对象             | `UserBO`     | `service` 内部        |
| VO   | 视图展示对象                 | `UserVO`     | `dto.res`             |
| Query| 查询参数封装对象             | `UserQuery`  | `dto.req`             |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`

---

## 八、扩展性与日志规范

### 接口优先原则

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中（如 `UserServiceImpl`）。
- 公共能力（分页 `common.page`、枚举 `common.enums`、组件 `common.components`）统一沉淀，禁止在业务模块内重复实现。

### 日志记录

- 使用 `@Slf4j` 注解代替 `System.out.println`。
- MyBatis 日志实现固定为 `org.apache.ibatis.logging.slf4j.Slf4jImpl`，禁止改为 `System.out` 输出。
- 异常日志需记录关键上下文（业务标识、入参摘要），禁止打印敏感信息明文。
- 慢 SQL / 性能日志由 `com.edus.monitor` 与 `edus.performance.monitoring` 配置统一输出。

---

## 九、编码原则总结

| 原则       | 说明                                                       |
|------------|------------------------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性                     |
| **DRY**    | 避免重复代码，优先复用 `common` 模块与 Hutool 工具类        |
| **KISS**   | 保持代码简洁易懂                                           |
| **YAGNI**  | 不实现当前不需要的功能                                     |
| **OWASP**  | 防范常见安全漏洞，如 SQL 注入、XSS、敏感信息泄露等         |

---

## 十、命令与操作约定（PowerShell / Windows）

- 构建：在 `D:\ProgramFiles\JetBrains\IdeaProjects\edus-server` 目录执行 `mvn clean package`（多模块聚合构建）。
- 单独构建核心模块：`mvn -pl edus-core -am clean package`。
- 运行：`mvn -pl edus-core spring-boot:run`，或通过 `SPRING_PROFILES_ACTIVE` 指定环境。
- 路径书写使用 Windows 风格（反斜杠），Shell 命令按 PowerShell 语法编写。
