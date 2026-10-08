-- 用户表和示例数据
-- 对应实体：com.edus.entity.User -> users
-- 测试用户明文密码均为：123456
-- 密码字段存储 Hutool BCrypt 哈希，登录时使用 BCrypt.checkpw 校验。

CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

INSERT INTO `users` (`id`, `username`, `password`, `email`, `phone`, `status`, `created_at`, `updated_at`, `deleted`)
VALUES
  (1, 'admin', '$2a$10$e6/M5Tzy.MAbKNxAQsj8r.xwfl0pc/SSecf4yyuDWm/XJ28tppnEq', 'admin@example.com', '13800000001', 'ACTIVE', NOW(), NOW(), 0),
  (2, 'teacher_demo', '$2a$10$e6/M5Tzy.MAbKNxAQsj8r.xwfl0pc/SSecf4yyuDWm/XJ28tppnEq', 'teacher@example.com', '13800000002', 'ACTIVE', NOW(), NOW(), 0),
  (3, 'student_demo', '$2a$10$e6/M5Tzy.MAbKNxAQsj8r.xwfl0pc/SSecf4yyuDWm/XJ28tppnEq', 'student@example.com', '13800000003', 'ACTIVE', NOW(), NOW(), 0),
  (4, 'disabled_demo', '$2a$10$e6/M5Tzy.MAbKNxAQsj8r.xwfl0pc/SSecf4yyuDWm/XJ28tppnEq', 'disabled@example.com', '13800000004', 'DISABLED', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `email` = VALUES(`email`),
  `phone` = VALUES(`phone`),
  `status` = VALUES(`status`),
  `updated_at` = NOW(),
  `deleted` = VALUES(`deleted`);

-- 通用演示表：可用于缓存、分页、SQL安全、操作日志等示例接口测试
CREATE TABLE IF NOT EXISTS `demo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) NOT NULL COMMENT '演示编码',
  `name` varchar(128) NOT NULL COMMENT '演示名称',
  `category` varchar(64) DEFAULT NULL COMMENT '分类',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演示表';

INSERT INTO `demo` (`id`, `code`, `name`, `category`, `description`, `sort_order`, `status`, `created_at`, `updated_at`, `deleted`)
VALUES
  (1, 'DEMO_CACHE_001', '缓存示例数据', 'cache', '用于测试缓存抽象层和缓存注解。', 10, 'ACTIVE', NOW(), NOW(), 0),
  (2, 'DEMO_RATE_001', '限流示例数据', 'rate-limit', '用于测试接口限流访问效果。', 20, 'ACTIVE', NOW(), NOW(), 0),
  (3, 'DEMO_IDEMPOTENT_001', '幂等示例数据', 'idempotent', '用于测试重复提交和幂等控制。', 30, 'ACTIVE', NOW(), NOW(), 0),
  (4, 'DEMO_SQL_SAFE_001', 'SQL安全示例数据', 'security', '用于测试SQL注入防护和参数校验。', 40, 'ACTIVE', NOW(), NOW(), 0),
  (5, 'DEMO_DISABLED_001', '禁用示例数据', 'basic', '用于测试状态筛选。', 50, 'DISABLED', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `category` = VALUES(`category`),
  `description` = VALUES(`description`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`),
  `updated_at` = NOW(),
  `deleted` = VALUES(`deleted`);
