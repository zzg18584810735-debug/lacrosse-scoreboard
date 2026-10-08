
# edus-server 项目开发规范指南
本规范结合项目实际技术栈、目录结构与配置要求制定，所有开发人员需严格遵守，保证代码质量、可维护性与安全性。

## 一、用户工作环境信息
- **工作目录**：`D:\ProgramFiles\JetBrains\IdeaProjects\edus-server`
- **操作系统**：Windows 10
- **项目作者**：Administrator

## 二、项目目录结构
```
edus-server
├── .agents
├── .codebuddy
│   └── db
│       └── lucene
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
        │   │           ├── annotation       # 自定义注解
        │   │           ├── aspect            # AOP切面
        │   │           ├── common            # 通用模块
        │   │           │   ├── components    # 通用组件
        │   │           │   ├── enums         # 枚举类
        │   │           │   ├── exception     # 全局异常
        │   │           │   ├── page          # 分页封装
        │   │           │   └── utils         # 工具类
        │   │           ├── config            # 配置类
        │   │           ├── controller        # 控制器层
        │   │           │   └── example       # 示例接口
        │   │           ├── dto               # 数据传输对象
        │   │           │   ├── req           # 请求DTO
        │   │           │   └── res           # 响应DTO/VO
        │   │           ├── entity            # 数据库实体（DO）
        │   │           ├── filter            # 过滤器
        │   │           ├── interceptor       # 拦截器
        │   │           ├── mapper            # MyBatis-Plus Mapper接口
        │   │           ├── monitor           # 监控模块
        │   │           ├── security          # 安全模块
        │   │           │   ├── monitor       # 监控权限校验
        │   │           │   ├── sensitive     # 敏感数据加密
        │   │           │   ├── sql           # SQL注入防护
        │   │           │   └── xss           # XSS攻击防护
        │   │           └── service            # 服务层
        │   │               └── auth           # 认证授权子模块
        │   └── resources
        │       ├── static
        │       │   └── images                # 静态资源
        │       └── application.yml            # 全局配置文件
        └── test
            └── java
                └── com
                    └── edus                  # 测试类目录
```

## 三、技术栈与构建工具
1. **构建工具**：Maven（多模块项目，父模块`edus-server`为pom打包，子模块`edus-core`为jar打包）
2. **JDK版本**：17（项目强制要求，与pom.xml配置一致，禁止随意升级JDK版本）
3. **主框架**：Spring Boot 3.5.11
4. **ORM框架**：MyBatis-Plus 3.5.7
5. **核心依赖**：
   - Spring Boot Web：处理HTTP请求
   - Spring Boot Validation：参数校验
   - MySQL Connector J：MySQL驱动（runtime作用域）
   - Lombok：简化实体类代码
   - Hutool 5.8.25：通用工具类库
   - Spring Boot Redis：缓存支持
   - Spring Boot AOP：面向切面编程
   - Spring Boot Test：单元测试支持

## 四、依赖管理通用规则
1. 核心依赖版本统一在父模块`pom.xml`的`properties`中管理，禁止随意升级生产环境依赖版本，升级需经过技术评审。
2. 新增依赖前需评估必要性，避免引入冗余依赖，防止项目体积膨胀与安全风险。
3. 测试相关依赖的`scope`必须设置为`test`，禁止引入到生产环境。
4. Lombok仅用于实体类、DTO、VO等简单数据对象的`getter`/`setter`/构造方法生成，禁止在复杂业务逻辑、工具类中滥用。
5. 统一使用Hutool工具类库实现通用工具逻辑，禁止重复造轮子。

## 五、分层架构规范
项目严格遵循分层架构设计，各层职责与约束如下：
| 层级 | 对应包路径 | 职责说明 | 开发约束 |
|------|------------|----------|----------|
| Controller层 | `com.edus.controller`及子包 | 处理HTTP请求与响应，定义API接口 | 1. 不得直接访问数据库，必须通过Service层调用<br>2. 请求参数使用`req`包下的DTO接收，响应结果使用`res`包下的VO返回<br>3. 接口需添加`@Valid`校验，统一返回封装结果 |
| Service层 | `com.edus.service`及子包 | 实现业务逻辑、事务管理、数据校验 | 1. 必须通过Mapper层访问数据库<br>2. `@Transactional`注解仅加在Service层方法上，禁止加在其他层<br>3. 业务接口与实现分离，实现类统一放在接口所在包下的`impl`子包<br>4. 禁止返回Entity对象，需转换为DTO/VO返回 |
| Mapper层 | `com.edus.mapper` | 数据库访问与持久化操作 | 1. 继承MyBatis-Plus的`BaseMapper`接口<br>2. XML映射文件统一放在`resources/mapper`目录下，与Mapper接口路径对应<br>3. 复杂查询使用MyBatis-Plus注解，避免手动拼接SQL<br>4. 使用关联查询、`@TableField`等特性避免N+1查询问题 |
| Entity层 | `com.edus.entity` | 映射数据库表结构，对应DO（数据库实体对象） | 1. 不得直接返回给前端，需转换为DTO/VO<br>2. 统一使用Lombok注解简化代码<br>3. 逻辑删除字段统一为`deleted`，类型为Integer，0表示未删除，1表示已删除 |
| 通用层 | `com.edus.common` | 存放通用组件、枚举、全局异常、分页、工具类 | 1. 工具类统一为静态方法，禁止存在实例成员<br>2. 全局异常统一处理，禁止在Controller中直接抛异常 |
| 安全层 | `com.edus.security` | 实现XSS防护、SQL注入防护、敏感数据加密、接口权限校验 | 1. 所有接口需经过安全校验，禁止绕过安全拦截器<br>2. 敏感数据（如密码、身份证号）需加密存储 |
| 横切逻辑层 | `com.edus.aspect`/`interceptor`/`filter` | 实现日志记录、性能监控、请求拦截等横切逻辑 | 1. 横切逻辑统一放在对应包下，禁止散落在业务代码中<br>2. 切面优先级需合理配置，避免影响业务逻辑执行 |
| 监控模块 | `com.edus.monitor` | 实现系统性能监控、慢SQL监控、接口监控 | 1. 监控接口需添加权限校验，仅允许内部网络或携带正确Token访问 |

## 六、安全与性能规范
1. **输入校验**：统一使用`jakarta.validation.constraints`下的注解（如`@NotBlank`、`@Size`等），配合`@Valid`注解实现参数校验，校验异常统一由全局异常处理。
2. **SQL安全**：禁止手动拼接SQL字符串，防止SQL注入；MyBatis-Plus已开启SQL日志，慢SQL阈值1000ms，超过阈值会打印警告日志。
3. **事务管理**：`@Transactional`注解仅用于Service层方法，禁止在循环中频繁提交事务，事务传播行为需根据业务场景合理选择。
4. **缓存规范**：使用Redis作为缓存，连接池配置已统一，缓存key需添加业务前缀避免冲突，缓存数据需设置过期时间防止内存泄漏。
5. **数据源配置**：使用HikariCP连接池，已配置连接泄露检测、超时时间等参数，禁止修改通用配置，环境特定配置在对应profile文件中覆盖。
6. **性能监控**：已开启慢SQL监控、性能监控，异步任务超时警告阈值30秒，超过阈值需优化。
7. **接口安全**：监控接口需在请求头携带`X-Monitor-Token`，生产环境默认关闭dev profile的监控访问，仅允许内部网络访问。

## 七、代码风格规范
1. **命名规范**：
   | 类型 | 命名规则 | 示例 |
   |------|----------|------|
   | 类名 | 大驼峰UpperCamelCase | `UserServiceImpl`、`UserDTO` |
   | 方法/变量 | 小驼峰lowerCamelCase | `saveUser`、`userName` |
   | 常量 | 全大写下划线分隔UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS`、`REDIS_KEY_PREFIX` |
   | 包名 | 全小写，多级用`.`分隔 | `com.edus.controller` |
2. **注释规范**：所有类、方法、字段必须添加中文Javadoc注释，复杂业务逻辑需添加行内注释说明，禁止使用英文注释。
3. **类型后缀规范（阿里巴巴风格）**：
   | 后缀 | 用途 | 示例 |
   |------|------|------|
   | DTO | 数据传输对象，用于Controller和Service层交互 | `UserReqDTO`、`UserResDTO` |
   | DO | 数据库实体对象，与数据库表结构一一对应，放在entity包 | `UserDO` |
   | BO | 业务逻辑封装对象，用于Service层内部业务处理 | `UserBO` |
   | VO | 视图展示对象，用于返回给前端 | `UserVO` |
   | Query | 查询参数封装对象，用于复杂查询条件封装 | `UserQuery` |
4. **实体类简化**：实体类、DTO、VO等简单对象统一使用Lombok的`@Data`、`@NoArgsConstructor`、`@AllArgsConstructor`注解，禁止手动编写`getter`/`setter`/构造方法。

## 八、日志规范
1. 统一使用SLF4J日志框架，通过`@Slf4j`注解生成日志对象，禁止使用`System.out.println`输出日志。
2. 日志级别规范：错误日志用`error`，业务关键节点用`info`，调试信息用`debug`，生产环境默认关闭`debug`日志。
3. 慢SQL、异常栈、接口请求参数等必须打印日志，便于问题排查。

## 九、测试规范
1. 核心业务逻辑必须编写单元测试，测试类放在`test/java/com/edus`目录下，与对应业务类路径一致。
2. 单元测试需覆盖正常场景、异常场景，保证核心逻辑的正确性。
3. 集成测试需模拟真实环境，避免依赖真实数据库/缓存，使用`@SpringBootTest`注解启动测试上下文。

## 十、编码原则总结
| 原则 | 说明 |
|------|------|
| SOLID | 高内聚、低耦合，增强可维护性与可扩展性 |
| DRY | 避免重复代码，提高复用性 |
| KISS | 保持代码简洁易懂 |
| YAGNI | 不实现当前不需要的功能 |
| OWASP | 防范常见安全漏洞，如SQL注入、XSS等 |

---
*本规范由Administrator于2026-07-14制定，所有新增代码需符合本规范，存量代码逐步重构适配。*
