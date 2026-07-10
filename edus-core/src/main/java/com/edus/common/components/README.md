# Components 包说明

本包用于存放项目中的基础设施组件，为业务层提供通用能力。

## 已实现的组件

### CacheManager - 缓存管理器
- **功能**: 统一管理Redis缓存操作，提供简洁的缓存操作接口
- **支持**: String、Hash、List、Set等多种数据结构
- **特性**: 统一key管理、异常处理、日志记录、过期时间管理

## 未来可以扩展的组件

- **FileManager**: 文件管理组件
- **EmailManager**: 邮件发送组件  
- **SmsManager**: 短信发送组件
- **QueueManager**: 消息队列管理组件
- **SearchManager**: 搜索引擎管理组件
- **LockManager**: 分布式锁管理组件

## 使用原则

1. **基础设施组件**: 通用能力，不包含具体业务逻辑
2. **可复用性**: 可在多个模块中复用
3. **独立性**: 不依赖具体的业务模块
4. **稳定性**: 提供稳定的API，便于维护

## 包结构说明

```
components/
├── CacheManager.java       # 缓存管理器
├── FileManager.java        # 文件管理器(计划)
├── EmailManager.java       # 邮件管理器(计划)
└── README.md              # 本说明文件
```

## 注意事项

- 组件类使用 `@Component` 注解
- 组件应该是无状态的，线程安全的
- 组件方法应该包含完善的异常处理
- 组件应该有清晰的JavaDoc文档