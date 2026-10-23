-- 滚网球人员信息管理系统 - 建表脚本（DDL）
-- 建议版本：MySQL 8.0+
-- 业务枚举（单位类型、各类状态等）取值与 sys_dict_item.item_value 对齐，由数据字典维护。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- CREATE DATABASE IF NOT EXISTS rolling_tennis_db
--    DEFAULT CHARACTER SET utf8mb4
--    COLLATE utf8mb4_0900_ai_ci;

-- USE rolling_tennis_db;

-- 每次执行先删表（按外键依赖从子表到父表）
DROP TABLE IF EXISTS team_player;
DROP TABLE IF EXISTS team_coach_rel;
DROP TABLE IF EXISTS team_movement_log;
DROP TABLE IF EXISTS group_rule;
DROP TABLE IF EXISTS team_info;
DROP TABLE IF EXISTS referee_certificate;
DROP TABLE IF EXISTS athlete_health_report;
DROP TABLE IF EXISTS person_role;
DROP TABLE IF EXISTS person_profile;
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user_unit;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_dict_item;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_unit;

-- 1) 单位表
CREATE TABLE IF NOT EXISTS sys_unit (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                        unit_name VARCHAR(100) NOT NULL COMMENT '单位名称',
                                        parent_id BIGINT NULL COMMENT '上级单位 ID',
                                        contact_name VARCHAR(100) NULL COMMENT '联系人',
                                        contact_phone VARCHAR(50) NULL COMMENT '联系电话（手机）',
                                        contact_email VARCHAR(100) NULL COMMENT '邮箱',
                                        unit_type VARCHAR(32) NULL COMMENT '单位类型，对应字典 unit_type 的 item_value',
                                        address VARCHAR(255) NULL COMMENT '地址',
                                        unit_biz_status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '单位运营状态，对应字典 unit_biz_status',
                                        created_by BIGINT NULL COMMENT '创建人 ID',
                                        updated_by BIGINT NULL COMMENT '更新人 ID',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        KEY idx_unit_parent (parent_id),
                                        KEY idx_unit_biz_status (unit_biz_status),
                                        KEY idx_unit_type (unit_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单位表';

-- 2) 角色表
CREATE TABLE IF NOT EXISTS sys_role (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                        role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
                                        role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
                                        data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据范围:ALL/UNIT/UNIT_AND_CHILD/SELF',
                                        status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1 启用 0 禁用',
                                        remark VARCHAR(255) NULL COMMENT '备注',
                                        created_by BIGINT NULL COMMENT '创建人 ID',
                                        updated_by BIGINT NULL COMMENT '更新人 ID',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        KEY idx_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 2.1) 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                             type_code VARCHAR(64) NOT NULL UNIQUE COMMENT '字典类型编码',
                                             type_name VARCHAR(100) NOT NULL COMMENT '字典类型名称',
                                             status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1 启用 0 禁用',
                                             remark VARCHAR(255) NULL COMMENT '备注',
                                             created_by BIGINT NULL COMMENT '创建人 ID',
                                             updated_by BIGINT NULL COMMENT '更新人 ID',
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 2.2) 字典项表
CREATE TABLE IF NOT EXISTS sys_dict_item (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                             type_code VARCHAR(64) NOT NULL COMMENT '字典类型编码',
                                             item_value VARCHAR(64) NOT NULL COMMENT '字典值',
                                             item_label VARCHAR(100) NOT NULL COMMENT '字典文本',
                                             sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                             status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1 启用 0 禁用',
                                             remark VARCHAR(255) NULL COMMENT '备注',
                                             created_by BIGINT NULL COMMENT '创建人 ID',
                                             updated_by BIGINT NULL COMMENT '更新人 ID',
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             UNIQUE KEY uk_type_value (type_code, item_value),
                                             KEY idx_type_sort (type_code, sort_no),
                                             CONSTRAINT fk_dict_item_type_code FOREIGN KEY (type_code) REFERENCES sys_dict_type(type_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- 3) 系统用户表（登录账号）
CREATE TABLE IF NOT EXISTS sys_user (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                        username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                                        password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
                                        real_name VARCHAR(100) NOT NULL COMMENT '真实姓名',
                                        gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别:0 未知 1 男 2 女（字典 gender_type）',
                                        phone VARCHAR(20) NULL COMMENT '手机号',
                                        email VARCHAR(100) NULL COMMENT '邮箱',
                                        unit_id BIGINT NULL COMMENT '单位 ID',
                                        status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1 启用 0 禁用',
                                        last_login_time DATETIME NULL COMMENT '最近登录时间',
                                        created_by BIGINT NULL COMMENT '创建人 ID',
                                        updated_by BIGINT NULL COMMENT '更新人 ID',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        KEY idx_user_unit_status (unit_id, status),
                                        CONSTRAINT fk_sys_user_unit FOREIGN KEY (unit_id) REFERENCES sys_unit(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 4) 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                             user_id BIGINT NOT NULL COMMENT '用户 ID',
                                             role_id BIGINT NOT NULL COMMENT '角色 ID',
                                             created_by BIGINT NULL COMMENT '创建人 ID',
                                             updated_by BIGINT NULL COMMENT '更新人 ID',
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             UNIQUE KEY uk_user_role (user_id, role_id),
                                             CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                                             CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 4.1) 用户单位关联表（支持用户多单位）
CREATE TABLE IF NOT EXISTS sys_user_unit (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                             user_id BIGINT NOT NULL COMMENT '用户 ID',
                                             unit_id BIGINT NOT NULL COMMENT '单位 ID',
                                             created_by BIGINT NULL COMMENT '创建人 ID',
                                             updated_by BIGINT NULL COMMENT '更新人 ID',
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             UNIQUE KEY uk_user_unit (user_id, unit_id),
                                             CONSTRAINT fk_user_unit_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
                                             CONSTRAINT fk_user_unit_unit FOREIGN KEY (unit_id) REFERENCES sys_unit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户单位关联表';

-- 5) 菜单表（前端路由 / 权限节点）
CREATE TABLE IF NOT EXISTS sys_menu (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                        parent_id BIGINT NULL COMMENT '父菜单 ID，NULL 表示顶级',
                                        menu_code VARCHAR(64) NOT NULL UNIQUE COMMENT '菜单编码，对应权限标识',
                                        menu_name VARCHAR(100) NOT NULL COMMENT '菜单名称',
                                        menu_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型:0 目录 1 菜单 2 按钮',
                                        path VARCHAR(255) NULL COMMENT '路由 path',
                                        component VARCHAR(255) NULL COMMENT '前端组件路径',
                                        permission VARCHAR(128) NULL COMMENT '权限字符串，如 person:list',
                                        icon VARCHAR(100) NULL COMMENT '图标名',
                                        sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
                                        visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见:1 显示 0 隐藏',
                                        status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1 启用 0 禁用',
                                        remark VARCHAR(255) NULL COMMENT '备注',
                                        created_by BIGINT NULL COMMENT '创建人 ID',
                                        updated_by BIGINT NULL COMMENT '更新人 ID',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                        KEY idx_menu_parent_sort_status (parent_id, sort_no, status),
                                        CONSTRAINT fk_sys_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 6) 角色菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                             role_id BIGINT NOT NULL COMMENT '角色 ID',
                                             menu_id BIGINT NOT NULL COMMENT '菜单 ID',
                                             created_by BIGINT NULL COMMENT '创建人 ID',
                                             updated_by BIGINT NULL COMMENT '更新人 ID',
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             UNIQUE KEY uk_role_menu (role_id, menu_id),
                                             CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
                                             CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 7) 文件表（须在人员、球队之前创建，供 logo / 头像 / 证件照外键引用）
CREATE TABLE IF NOT EXISTS sys_file (
                                        id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                        biz_type VARCHAR(50) NOT NULL COMMENT '业务类型:person_profile / team_info 等',
                                        biz_id BIGINT NOT NULL COMMENT '业务 ID',
                                        category VARCHAR(20) NOT NULL DEFAULT 'other' COMMENT '分类:image/doc/video/other',
                                        origin_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
                                        file_ext VARCHAR(20) NULL COMMENT '后缀名',
                                        mime_type VARCHAR(100) NULL COMMENT 'MIME 类型',
                                        file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小 (字节)',
                                        storage_provider VARCHAR(20) NOT NULL DEFAULT 'aliyun_oss' COMMENT '存储提供商',
                                        bucket_name VARCHAR(100) NOT NULL COMMENT 'OSS bucket',
                                        object_key VARCHAR(500) NOT NULL COMMENT 'OSS 对象 key',
                                        file_url VARCHAR(1000) NULL COMMENT '访问 URL(可为空)',
                                        etag VARCHAR(128) NULL COMMENT 'ETag/内容摘要',
                                        status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1 正常 0 已删除',
                                        created_by BIGINT NULL COMMENT '上传人 (系统用户 ID)',
                                        updated_by BIGINT NULL COMMENT '更新人 ID',
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
                                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                        UNIQUE KEY uk_object_key (object_key),
                                        KEY idx_biz (biz_type, biz_id),
                                        KEY idx_status (status),
                                        KEY idx_upload_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';

-- 7.1) 登录日志表
CREATE TABLE IF NOT EXISTS sys_login_log (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                             user_id BIGINT NULL COMMENT '用户 ID（登录失败可为空）',
                                             username VARCHAR(50) NOT NULL COMMENT '登录账号',
                                             login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
                                             login_status VARCHAR(16) NOT NULL COMMENT '登录结果：SUCCESS/FAIL',
                                             fail_reason VARCHAR(255) NULL COMMENT '失败原因',
                                             login_ip VARCHAR(64) NULL COMMENT '登录 IP',
                                             user_agent VARCHAR(512) NULL COMMENT 'User-Agent',
                                             created_by BIGINT NULL COMMENT '创建人 ID',
                                             updated_by BIGINT NULL COMMENT '更新人 ID',
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             KEY idx_login_time (login_time),
                                             KEY idx_username_time (username, login_time),
                                             KEY idx_status_time (login_status, login_time),
                                             CONSTRAINT fk_login_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统登录日志表';

-- 8) 球队表
CREATE TABLE IF NOT EXISTS team_info (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                         team_name VARCHAR(100) NOT NULL COMMENT '球队名称',
                                         unit_id BIGINT NULL COMMENT '所属单位 ID',
                                         logo_file_id BIGINT NULL COMMENT '球队 Logo，对应 sys_file.id',
                                         coach_person_id BIGINT NULL COMMENT '主教练人员主档 ID（person_profile，person_role 含 coach）',
                                         coach_name VARCHAR(100) NULL COMMENT '主教练姓名（可冗余展示）',
                                         contact_phone VARCHAR(20) NULL COMMENT '联系方式',
                                         age_group VARCHAR(16) NULL COMMENT '组别，对应字典 team_age_group',
                                         team_biz_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '球队状态，对应字典 team_biz_status',
                                         founded_date DATE NULL COMMENT '成立日期',
                                         manager_user_id BIGINT NULL COMMENT '负责人用户 ID',
                                         remark VARCHAR(255) NULL COMMENT '备注',
                                         created_by BIGINT NULL COMMENT '创建人 ID',
                                         updated_by BIGINT NULL COMMENT '更新人 ID',
                                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         KEY idx_team_unit_biz_status (unit_id, team_biz_status),
                                         KEY idx_team_manager (manager_user_id),
                                         KEY idx_team_coach_person (coach_person_id),
                                         KEY idx_team_logo_file (logo_file_id),
                                         CONSTRAINT fk_team_unit FOREIGN KEY (unit_id) REFERENCES sys_unit(id) ON DELETE SET NULL,
                                         CONSTRAINT fk_team_manager FOREIGN KEY (manager_user_id) REFERENCES sys_user(id) ON DELETE SET NULL,
                                         CONSTRAINT fk_team_logo_file FOREIGN KEY (logo_file_id) REFERENCES sys_file(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队表';

-- 8.0) 球队组别报名规则（按 team_info.age_group 控制可报名年龄）
CREATE TABLE IF NOT EXISTS group_rule (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                                  age_group VARCHAR(16) NOT NULL COMMENT '组别编码，对应字典 team_age_group.item_value',
                                                  min_age INT NOT NULL COMMENT '最小周岁（含）',
                                                  max_age INT NOT NULL COMMENT '最大周岁（含）',
                                                  gender_limit VARCHAR(16) NOT NULL DEFAULT 'MIXED' COMMENT '性别限制：MALE_ONLY/FEMALE_ONLY/MIXED',
                                                  cutoff_month_day CHAR(5) NOT NULL DEFAULT '12-31' COMMENT '年龄截止月日，格式 MM-dd',
                                                  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1 启用 0 禁用',
                                                  remark VARCHAR(255) NULL COMMENT '备注',
                                                  created_by BIGINT NULL COMMENT '创建人 ID',
                                                  updated_by BIGINT NULL COMMENT '更新人 ID',
                                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                  UNIQUE KEY uk_tagr_age_group (age_group),
                                                  KEY idx_tagr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队组别报名规则表';

-- 8.1) 球队异动流水（解散/转单位/球员离队/转队等，供工作台与审计查询）
CREATE TABLE IF NOT EXISTS team_movement_log (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                                team_id BIGINT NULL COMMENT '球队 ID（球队删除后可空）',
                                                team_name_snapshot VARCHAR(100) NULL COMMENT '操作时队名快照',
                                                unit_id_snapshot BIGINT NULL COMMENT '操作时用于数据权限的单位（多为球队当时所属单位）',
                                                movement_type VARCHAR(32) NOT NULL COMMENT 'DISSOLVE/TRANSFER_UNIT/PLAYER_LEAVE/PLAYER_TRANSFER',
                                                summary VARCHAR(500) NULL COMMENT '列表展示用摘要',
                                                payload_json TEXT NULL COMMENT '结构化明细 JSON',
                                                effective_date DATE NULL COMMENT '业务生效日（如解散日、离队日）',
                                                created_by BIGINT NULL COMMENT '操作人 ID',
                                                updated_by BIGINT NULL COMMENT '更新人 ID',
                                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                KEY idx_tml_team (team_id),
                                                KEY idx_tml_unit (unit_id_snapshot),
                                                KEY idx_tml_created (created_at),
                                                CONSTRAINT fk_tml_team FOREIGN KEY (team_id) REFERENCES team_info(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队异动记录表';

-- 9) 人员主档表（自然人维度；身份以 person_role 为准，可多角色并存：教练+裁判等；person_type 保留兼容旧接口/列表入口）
CREATE TABLE IF NOT EXISTS person_profile (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                              person_type VARCHAR(20) NOT NULL COMMENT '冗余主类型(兼容):athlete/coach/referee/staff，与 person_role 首角色或业务写入保持一致',
                                              full_name VARCHAR(100) NOT NULL COMMENT '姓名',
                                              gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别:0 未知 1 男 2 女（选项见字典 gender_type）',
                                              avatar_file_id BIGINT NULL COMMENT '头像，对应 sys_file.id',
                                              birth_date DATE NULL COMMENT '出生日期',
                                              id_doc_type VARCHAR(32) NULL COMMENT '证件类型，对应字典 id_doc_type',
                                              id_card_no VARCHAR(32) NULL COMMENT '证件号',
                                              id_doc_file_id BIGINT NULL COMMENT '证件照片，对应 sys_file.id',
                                              phone VARCHAR(20) NULL COMMENT '联系方式',
                                              emergency_contact VARCHAR(100) NULL COMMENT '紧急联系人',
                                              emergency_phone VARCHAR(20) NULL COMMENT '紧急联系电话',
                                              last_medical_exam_date DATE NULL COMMENT '最近体检日期（运动员）',
                                              last_team_join_date DATE NULL COMMENT '最近入队日期',
                                              biz_status VARCHAR(32) NULL COMMENT '业务状态：运动员→竞技状态 player_status（在役/退役/禁赛/受伤）；教练→coach_status；裁判→referee_status；是否在队由 team_player 在册行推导，不落库',
                                              remark VARCHAR(255) NULL COMMENT '备注',
                                              created_by BIGINT NULL COMMENT '创建人 ID',
                                              updated_by BIGINT NULL COMMENT '更新人 ID',
                                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                              KEY idx_person_type_biz (person_type, biz_status),
                                              KEY idx_person_phone (phone),
                                              KEY idx_person_id_card_no (id_card_no),
                                              KEY idx_person_avatar_file (avatar_file_id),
                                              KEY idx_person_id_doc_file (id_doc_file_id),
                                              CONSTRAINT fk_person_avatar_file FOREIGN KEY (avatar_file_id) REFERENCES sys_file(id) ON DELETE SET NULL,
                                              CONSTRAINT fk_person_id_doc_file FOREIGN KEY (id_doc_file_id) REFERENCES sys_file(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员主档表';

-- 9.05) 人员角色关联表（一人多身份：教练可同时为裁判；运动员建议仅 athlete 一行，由应用层校验）
CREATE TABLE IF NOT EXISTS person_role (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                           person_id BIGINT NOT NULL COMMENT '人员主档 ID（person_profile.id）',
                                           role VARCHAR(20) NOT NULL COMMENT '身份: athlete/coach/referee/staff',
                                           created_by BIGINT NULL COMMENT '创建人 ID',
                                           updated_by BIGINT NULL COMMENT '更新人 ID',
                                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                           UNIQUE KEY uk_person_role (person_id, role),
                                           KEY idx_role_person (role, person_id),
                                           CONSTRAINT fk_person_role_person FOREIGN KEY (person_id) REFERENCES person_profile(id) ON DELETE CASCADE,
                                           CONSTRAINT chk_person_role_value CHECK (role IN ('athlete', 'coach', 'referee', 'staff'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员身份角色表（多对多：人与身份）';

-- 9.2) 运动员体检报告表（一个运动员可多条报告；每条报告可绑定多个附件 sys_file）
CREATE TABLE IF NOT EXISTS athlete_health_report (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                                 athlete_id BIGINT NOT NULL COMMENT '运动员 ID（person_profile.id）',
                                                 exam_date DATE NOT NULL COMMENT '体检日期',
                                                 remark VARCHAR(255) NULL COMMENT '备注',
                                                 created_by BIGINT NULL COMMENT '创建人 ID',
                                                 updated_by BIGINT NULL COMMENT '更新人 ID',
                                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                 KEY idx_athlete_exam (athlete_id, exam_date),
                                                 CONSTRAINT fk_athlete_health_report_athlete FOREIGN KEY (athlete_id) REFERENCES person_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动员体检报告表';

-- 9.25) 裁判证书表（一个裁判可维护多本证书）
CREATE TABLE IF NOT EXISTS referee_certificate (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                                referee_id BIGINT NOT NULL COMMENT '裁判人员 ID（person_profile.id，且 person_role 含 referee）',
                                                cert_scope VARCHAR(16) NOT NULL COMMENT '证书体系：CN(国内)/HK(香港)/INTL(国际)',
                                                cert_level VARCHAR(64) NOT NULL COMMENT '证书等级（文本/字典值）',
                                                issuing_authority VARCHAR(100) NOT NULL COMMENT '发证单位',
                                                register_region VARCHAR(100) NULL COMMENT '注册地区',
                                                valid_until DATE NULL COMMENT '有效期至',
                                                cert_file_id BIGINT NULL COMMENT '证书图片文件 ID（sys_file.id）',
                                                remark VARCHAR(255) NULL COMMENT '备注',
                                                created_by BIGINT NULL COMMENT '创建人 ID',
                                                updated_by BIGINT NULL COMMENT '更新人 ID',
                                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                KEY idx_ref_cert_referee (referee_id),
                                                KEY idx_ref_cert_scope_level (cert_scope, cert_level),
                                                KEY idx_ref_cert_valid_until (valid_until),
                                                KEY idx_ref_cert_file (cert_file_id),
                                                CONSTRAINT fk_ref_cert_referee FOREIGN KEY (referee_id) REFERENCES person_profile(id) ON DELETE CASCADE,
                                                CONSTRAINT fk_ref_cert_file FOREIGN KEY (cert_file_id) REFERENCES sys_file(id) ON DELETE SET NULL,
                                                CONSTRAINT chk_ref_cert_scope CHECK (cert_scope IN ('CN', 'HK', 'INTL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='裁判证书表';

-- 9.1) 球队教练关联表（支持多教练+类型；同一教练可多次入队，保留历史记录）
CREATE TABLE IF NOT EXISTS team_coach_rel (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                             team_id BIGINT NOT NULL COMMENT '球队 ID',
                                             person_id BIGINT NOT NULL COMMENT '教练人员 ID（person_profile.id）',
                                             coach_type VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '教练类型：HEAD/OTHER，对应字典 coach_team_role',
                                             sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                             rel_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '在队状态：ACTIVE/QUIT',
                                             quit_date DATE NULL COMMENT '退出日期（离队时记录）',
                                             created_by BIGINT NULL COMMENT '创建人 ID',
                                             updated_by BIGINT NULL COMMENT '更新人 ID',
                                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             KEY idx_team_coach_team_person (team_id, person_id),
                                             KEY idx_team_coach_team (team_id),
                                             KEY idx_team_coach_person (person_id),
                                             KEY idx_team_coach_type (coach_type),
                                             KEY idx_team_coach_status (rel_status),
                                             CONSTRAINT fk_team_coach_team FOREIGN KEY (team_id) REFERENCES team_info(id) ON DELETE CASCADE,
                                             CONSTRAINT fk_team_coach_person FOREIGN KEY (person_id) REFERENCES person_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队教练关联表';

-- 9.3) 球队球员表（支持历史多次入队：同一运动员在同一球队可保留多条历史记录）
CREATE TABLE IF NOT EXISTS team_player (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
                                          team_id BIGINT NOT NULL COMMENT '球队 ID',
                                          unit_id BIGINT NULL COMMENT '单位 ID（与球队所属单位一致，冗余便于查询）',
                                          person_id BIGINT NOT NULL COMMENT '球员人员 ID（person_profile.id，person_role 含 athlete）',
                                          player_role VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '球员类型：队长/副队长/队员，对应字典 team_player_role',
                                          jersey_no INT NOT NULL COMMENT '球服号（非负整数，可为 0）',
                                          join_date DATE NULL COMMENT '入队日期',
                                          roster_status VARCHAR(32) NOT NULL DEFAULT 'ON_TEAM' COMMENT '入队状态：在队/退出中/已退出，字典 team_player_roster_status',
                                          comp_status VARCHAR(32) NOT NULL DEFAULT 'ON_DUTY' COMMENT '本行竞技状态：在役/退役/禁赛/受伤，字典 player_status（与运动员主档 biz_status 同套编码）',
                                          quit_date DATE NULL COMMENT '退出日期（roster 已退出等）',
                                          remark VARCHAR(255) NULL COMMENT '备注',
                                          created_by BIGINT NULL COMMENT '创建人 ID',
                                          updated_by BIGINT NULL COMMENT '更新人 ID',
                                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                          KEY idx_team_player_team_person (team_id, person_id),
                                          KEY idx_team_player_team (team_id),
                                          KEY idx_team_player_unit (unit_id),
                                          KEY idx_team_player_person (person_id),
                                          KEY idx_team_player_roster (roster_status),
                                          KEY idx_team_player_comp (comp_status),
                                          CONSTRAINT fk_team_player_team FOREIGN KEY (team_id) REFERENCES team_info(id) ON DELETE CASCADE,
                                          CONSTRAINT fk_team_player_unit FOREIGN KEY (unit_id) REFERENCES sys_unit(id) ON DELETE SET NULL,
                                          CONSTRAINT fk_team_player_person FOREIGN KEY (person_id) REFERENCES person_profile(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队球员表';

SET FOREIGN_KEY_CHECKS = 1;
