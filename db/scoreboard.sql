-- 棍网球计分器表结构，兼容 MySQL 5.6。
-- archive_user 中的球队、人员及字典数据属于外部平台，只保存业务 ID，不建立跨库外键。

SET NAMES utf8mb4;

-- 可复用规则模板，age_group 对应字典 team_age_group.item_value，同组别可配置多个模板。
CREATE TABLE IF NOT EXISTS `scoreboard_group_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `age_group` varchar(16) NOT NULL COMMENT '组别编码',
  `template_name` varchar(80) NOT NULL COMMENT '规则模板名称',

  `players_per_team` tinyint unsigned NOT NULL COMMENT '每队上场人数',
  `regular_periods` tinyint unsigned NOT NULL COMMENT '常规节数',
  `period_minutes` smallint unsigned NOT NULL COMMENT '每节分钟',
  `halftime_after_period` tinyint unsigned NOT NULL COMMENT '中场所在节后',
  `shot_clock_seconds` smallint unsigned NOT NULL DEFAULT 0 COMMENT '进攻钟秒数',
  `clear_seconds` smallint unsigned NOT NULL DEFAULT 0 COMMENT '推进提示秒数',
  `points_per_goal` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '每球得分',

  `period_break_minutes` smallint unsigned NOT NULL DEFAULT 0 COMMENT '节间休息分钟',
  `halftime_minutes` smallint unsigned NOT NULL DEFAULT 0 COMMENT '中场休息分钟',
  `timeout_seconds` smallint unsigned NOT NULL COMMENT '球队暂停秒数',
  `timeouts_per_half` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '每队每半场暂停次数',
  `overtime_timeouts` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '每队每节加时暂停次数',

  `stop_window_seconds` smallint unsigned NOT NULL DEFAULT 0 COMMENT '死球停表窗口秒数',
  `all_dead_stop` tinyint(1) NOT NULL DEFAULT 0 COMMENT '普通阶段死球停表：0=否，1=是',
  `foul_stop` tinyint(1) NOT NULL DEFAULT 0 COMMENT '无罚时犯规停表：0=否，1=是',
  `overtime_stop_all` tinyint(1) NOT NULL DEFAULT 0 COMMENT '加时全程死球停表：0=否，1=是',
  `shot_last_off` tinyint(1) NOT NULL DEFAULT 1 COMMENT '末段关闭进攻钟：0=否，1=是',

  `overtime_policy` varchar(16) NOT NULL COMMENT '加时规则：always=始终加时，knockout=仅淘汰赛加时，none=不加时',
  `overtime_minutes` smallint unsigned NOT NULL COMMENT '每节加时分钟',
  `golden_goal` tinyint(1) NOT NULL DEFAULT 0 COMMENT '金球制：0=否，1=是',
  `overtime_break_minutes` smallint unsigned NOT NULL DEFAULT 0 COMMENT '加时休息分钟',

  `minor_penalty_seconds` smallint unsigned NOT NULL COMMENT '绿牌罚时秒数',
  `major_penalty_seconds` smallint unsigned NOT NULL COMMENT '黄牌罚时秒数',
  `expulsion_penalty_seconds` smallint unsigned NOT NULL COMMENT '红牌代罚秒数',
  `major_limit` tinyint unsigned NOT NULL COMMENT '重大犯规罚退次数',
  `goalie_proxy` tinyint(1) NOT NULL DEFAULT 0 COMMENT '守门员由他人代罚：0=否，1=是',
  `pass_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '首次射门前要求接传：0=否，1=是',
  `alternate_first` varchar(16) NOT NULL COMMENT '首次交替球权：manual=手动指定，home=主队，away=客队',
  `alternate_reset` tinyint(1) NOT NULL DEFAULT 0 COMMENT '加时重置交替球权：0=否，1=是',

  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启用状态：0=停用，1=启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_rules_group_name` (`age_group`, `template_name`),
  KEY `idx_group_rules_age_group` (`age_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则模板';

-- 比赛基础信息和结果摘要。
CREATE TABLE IF NOT EXISTS `scoreboard_matches` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(80) NOT NULL COMMENT '比赛名称',
  `match_no` varchar(32) DEFAULT NULL COMMENT '场次编号',
  `match_date` date NOT NULL COMMENT '比赛日期',
  `match_time` time DEFAULT NULL COMMENT '计划比赛时间',
  `venue_name` varchar(100) DEFAULT NULL COMMENT '比赛场地',
  `age_group` varchar(16) NOT NULL COMMENT '组别编码，对应team_age_group.item_value',
  `group_rule_id` bigint DEFAULT NULL COMMENT '选用的规则模板ID；本场自定义时为空',
  `stage` varchar(16) NOT NULL DEFAULT 'league' COMMENT '比赛阶段：league=常规赛，knockout=淘汰赛',

  `home_team_id` bigint DEFAULT NULL COMMENT '主队ID，对应team_info.id',
  `home_team_name` varchar(100) NOT NULL COMMENT '主队名称快照',
  `home_team_color` varchar(32) DEFAULT NULL COMMENT '主队比赛服颜色',
  `away_team_id` bigint DEFAULT NULL COMMENT '客队ID，对应team_info.id',
  `away_team_name` varchar(100) NOT NULL COMMENT '客队名称快照',
  `away_team_color` varchar(32) DEFAULT NULL COMMENT '客队比赛服颜色',

  `status` varchar(16) NOT NULL DEFAULT 'READY' COMMENT '比赛状态：READY=待开始，ONGOING=进行中，FINISHED=已结束',
  `started_at` datetime DEFAULT NULL COMMENT '实际开始时间，首次鸣哨时写入',
  `finished_at` datetime DEFAULT NULL COMMENT '实际结束时间，确认比赛结束时写入',
  `home_score` smallint unsigned NOT NULL DEFAULT 0 COMMENT '主队当前总分',
  `away_score` smallint unsigned NOT NULL DEFAULT 0 COMMENT '客队当前总分',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID，对应sys_user.id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '删除标记：0=正常，1=删除',
  PRIMARY KEY (`id`),
  KEY `idx_matches_date` (`match_date`),
  KEY `idx_matches_group_rule` (`group_rule_id`),
  KEY `idx_matches_home_team` (`home_team_id`),
  KEY `idx_matches_away_team` (`away_team_id`),
  KEY `idx_matches_status` (`status`),
  CONSTRAINT `fk_matches_group_rule`
    FOREIGN KEY (`group_rule_id`) REFERENCES `scoreboard_group_rules` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛';

-- 本场最终规则。创建比赛时保存最终规则快照，本场修改不会影响规则模板或其他比赛。
CREATE TABLE IF NOT EXISTS `scoreboard_match_rules` (
  `match_id` bigint NOT NULL COMMENT '比赛ID',
  `is_customized` tinyint(1) NOT NULL DEFAULT 0 COMMENT '规则来源：0=与引用模板一致，1=本场自定义',
  `players_per_team` tinyint unsigned NOT NULL COMMENT '每队上场人数',
  `regular_periods` tinyint unsigned NOT NULL COMMENT '常规节数',
  `period_minutes` smallint unsigned NOT NULL COMMENT '每节分钟',
  `halftime_after_period` tinyint unsigned NOT NULL COMMENT '中场所在节后',
  `shot_clock_seconds` smallint unsigned NOT NULL DEFAULT 0 COMMENT '进攻钟秒数',
  `clear_seconds` smallint unsigned NOT NULL DEFAULT 0 COMMENT '推进提示秒数',
  `points_per_goal` tinyint unsigned NOT NULL DEFAULT 1 COMMENT '每球得分',

  `period_break_minutes` smallint unsigned NOT NULL DEFAULT 0 COMMENT '节间休息分钟',
  `halftime_minutes` smallint unsigned NOT NULL DEFAULT 0 COMMENT '中场休息分钟',
  `timeout_seconds` smallint unsigned NOT NULL COMMENT '球队暂停秒数',
  `timeouts_per_half` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '每队每半场暂停次数',
  `overtime_timeouts` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '每队每节加时暂停次数',

  `stop_window_seconds` smallint unsigned NOT NULL DEFAULT 0 COMMENT '死球停表窗口秒数',
  `all_dead_stop` tinyint(1) NOT NULL DEFAULT 0 COMMENT '普通阶段死球停表：0=否，1=是',
  `foul_stop` tinyint(1) NOT NULL DEFAULT 0 COMMENT '无罚时犯规停表：0=否，1=是',
  `overtime_stop_all` tinyint(1) NOT NULL DEFAULT 0 COMMENT '加时全程死球停表：0=否，1=是',
  `shot_last_off` tinyint(1) NOT NULL DEFAULT 1 COMMENT '末段关闭进攻钟：0=否，1=是',

  `overtime_policy` varchar(16) NOT NULL COMMENT '加时规则：always=始终加时，knockout=仅淘汰赛加时，none=不加时',
  `overtime_minutes` smallint unsigned NOT NULL COMMENT '每节加时分钟',
  `golden_goal` tinyint(1) NOT NULL DEFAULT 0 COMMENT '金球制：0=否，1=是',
  `overtime_break_minutes` smallint unsigned NOT NULL DEFAULT 0 COMMENT '加时休息分钟',

  `minor_penalty_seconds` smallint unsigned NOT NULL COMMENT '绿牌罚时秒数',
  `major_penalty_seconds` smallint unsigned NOT NULL COMMENT '黄牌罚时秒数',
  `expulsion_penalty_seconds` smallint unsigned NOT NULL COMMENT '红牌代罚秒数',
  `major_limit` tinyint unsigned NOT NULL COMMENT '重大犯规罚退次数',
  `goalie_proxy` tinyint(1) NOT NULL DEFAULT 0 COMMENT '守门员由他人代罚：0=否，1=是',
  `pass_required` tinyint(1) NOT NULL DEFAULT 0 COMMENT '首次射门前要求接传：0=否，1=是',
  `alternate_first` varchar(16) NOT NULL COMMENT '首次交替球权：manual=手动指定，home=主队，away=客队',
  `alternate_reset` tinyint(1) NOT NULL DEFAULT 0 COMMENT '加时重置交替球权：0=否，1=是',

  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`match_id`),
  CONSTRAINT `fk_match_rules_match`
    FOREIGN KEY (`match_id`) REFERENCES `scoreboard_matches` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本场规则快照';

-- 计分台当前运行状态，一场比赛一条；只在操作节点保存，不需要每秒写库。
CREATE TABLE IF NOT EXISTS `scoreboard_match_state` (
  `match_id` bigint NOT NULL COMMENT '比赛ID',
  `phase` varchar(16) NOT NULL DEFAULT 'READY' COMMENT '场内阶段：READY=待鸣哨，PERIOD=比赛节，PERIOD_END=本节结束，BREAK=节间或中场，TIMEOUT=球队暂停，FINISHED=比赛结束',
  `current_period` smallint unsigned NOT NULL DEFAULT 1 COMMENT '当前节次，从1开始，超过常规节数表示加时',

  `main_remaining_seconds` int unsigned NOT NULL DEFAULT 0 COMMENT '比赛钟剩余秒数',
  `shot_remaining_seconds` smallint unsigned NOT NULL DEFAULT 0 COMMENT '进攻钟剩余秒数',
  `clear_remaining_seconds` smallint unsigned DEFAULT NULL COMMENT '推进提示剩余秒数：NULL=未启动或已取消，0=已到时待裁判确认',
  `auxiliary_type` varchar(16) DEFAULT NULL COMMENT '辅助计时类型：BREAK=节间，HALFTIME=中场，OVERTIME_BREAK=加时休息，TIMEOUT=球队暂停',
  `auxiliary_remaining_seconds` int unsigned NOT NULL DEFAULT 0 COMMENT '辅助计时剩余秒数',
  `timeout_event_id` bigint DEFAULT NULL COMMENT '当前球队暂停事件ID，无球队暂停时为空',
  `timeout_return_phase` varchar(16) DEFAULT NULL COMMENT '球队暂停结束后返回阶段：READY=待鸣哨，PERIOD=比赛节，BREAK=节间或中场',
  `timeout_return_auxiliary_type` varchar(16) DEFAULT NULL COMMENT '球队暂停前辅助计时类型',
  `timeout_return_auxiliary_seconds` int unsigned NOT NULL DEFAULT 0 COMMENT '球队暂停前辅助计时剩余秒数',
  `timeout_return_auxiliary_running` tinyint(1) NOT NULL DEFAULT 0 COMMENT '球队暂停前辅助计时运行状态：0=暂停，1=运行',
  `main_running` tinyint(1) NOT NULL DEFAULT 0 COMMENT '比赛钟运行状态：0=暂停，1=运行',
  `auxiliary_running` tinyint(1) NOT NULL DEFAULT 0 COMMENT '辅助计时运行状态：0=暂停，1=运行',
  `timer_started_at` datetime DEFAULT NULL COMMENT '本次计时启动时间，全部暂停时为空',

  `ball_state` varchar(8) NOT NULL DEFAULT 'dead' COMMENT '球状态：dead=死球，live=活球',
  `possession_side` varchar(8) DEFAULT NULL COMMENT '当前球权：home=主队，away=客队',
  `frontcourt` tinyint(1) NOT NULL DEFAULT 0 COMMENT '当前控球队已进入前场：0=否，1=是',
  `alternate_side` varchar(8) DEFAULT NULL COMMENT '下一次交替球权：home=主队，away=客队',
  `reset_shot_on_resume` tinyint(1) NOT NULL DEFAULT 0 COMMENT '复赛时重置进攻钟：0=否，1=是',
  `golden_goal` tinyint(1) NOT NULL DEFAULT 0 COMMENT '本场已产生待确认金球：0=否，1=是',
  `golden_conflict` tinyint(1) NOT NULL DEFAULT 0 COMMENT '金球记录冲突待核对：0=否，1=是',
  `pending_goal_sequence_no` int unsigned DEFAULT NULL COMMENT '待确认进球预占的本场事件顺序号',
  `pending_goal_side` varchar(8) DEFAULT NULL COMMENT '待确认进球队伍：home=主队，away=客队',
  `pending_goal_period` smallint unsigned DEFAULT NULL COMMENT '待确认进球发生节次',
  `pending_goal_remaining_seconds` int unsigned DEFAULT NULL COMMENT '待确认进球发生时比赛钟剩余秒数',
  `pending_goal_roster_id` bigint DEFAULT NULL COMMENT '待确认进球球员本场名单ID',
  `pending_goal_assist_roster_id` bigint DEFAULT NULL COMMENT '待确认助攻球员本场名单ID',
  `home_timeouts_remaining` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '主队当前半场或加时剩余暂停次数',
  `away_timeouts_remaining` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '客队当前半场或加时剩余暂停次数',

  `state_version` int unsigned NOT NULL DEFAULT 0 COMMENT '状态版本号，用于防止并发覆盖',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`match_id`),
  CONSTRAINT `fk_match_state_match`
    FOREIGN KEY (`match_id`) REFERENCES `scoreboard_matches` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计分台运行状态';

-- 本场报名名单。球员来源于球队球员关系，允许先录姓名号码后回填关联 ID。
CREATE TABLE IF NOT EXISTS `scoreboard_match_roster` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `match_id` bigint NOT NULL COMMENT '比赛ID',
  `team_side` varchar(8) NOT NULL COMMENT '球队身份：home=主队，away=客队',
  `team_player_id` bigint DEFAULT NULL COMMENT '球队球员关系ID，对应team_player.id，未回填时为空',
  `player_name` varchar(100) NOT NULL COMMENT '球员姓名快照',
  `jersey_no` int unsigned NOT NULL COMMENT '本场球衣号码快照',
  `position_name` varchar(32) DEFAULT NULL COMMENT '本场位置快照',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roster_match_player` (`match_id`, `team_player_id`),
  UNIQUE KEY `uk_roster_match_jersey` (`match_id`, `team_side`, `jersey_no`),
  KEY `idx_roster_match_side` (`match_id`, `team_side`),
  KEY `idx_roster_team_player` (`team_player_id`),
  CONSTRAINT `fk_roster_match`
    FOREIGN KEY (`match_id`) REFERENCES `scoreboard_matches` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本场球员名单';

-- 比赛时间线。进球、球队暂停、判罚、球权和更正统一按发生顺序记录。
CREATE TABLE IF NOT EXISTS `scoreboard_match_events` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `match_id` bigint NOT NULL COMMENT '比赛ID',
  `sequence_no` int unsigned NOT NULL COMMENT '本场事件顺序号',
  `event_type` varchar(24) NOT NULL COMMENT '事件类型：MATCH_START=比赛开始，PERIOD_START=本节开始，PERIOD_END=本节结束，MATCH_END=比赛结束，GOAL=进球，TEAM_TIMEOUT=球队暂停，PENALTY=判罚，POSSESSION=球权，CORRECTION=更正，SYSTEM=系统记录',
  `event_title` varchar(100) NOT NULL COMMENT '事件标题',
  `period_no` smallint unsigned NOT NULL COMMENT '发生节次',
  `remaining_seconds` int unsigned NOT NULL COMMENT '发生时比赛钟剩余秒数',
  `duration_seconds` int unsigned DEFAULT NULL COMMENT '事件配置时长秒数，如球队暂停时长',
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件实际发生时间',
  `team_side` varchar(8) DEFAULT NULL COMMENT '相关球队：home=主队，away=客队',

  `roster_id` bigint DEFAULT NULL COMMENT '进球或犯规球员名单ID，可后续回填',
  `subject_name` varchar(100) DEFAULT NULL COMMENT '球队官员姓名或无法关联名单时的人员姓名快照',
  `assist_roster_id` bigint DEFAULT NULL COMMENT '助攻球员名单ID',
  `assist_name` varchar(100) DEFAULT NULL COMMENT '无法关联名单时的助攻球员号码或姓名快照',
  `points` tinyint unsigned DEFAULT NULL COMMENT '本次进球得分',

  `timeout_bucket` varchar(16) DEFAULT NULL COMMENT '暂停额度归属：FIRST_HALF=上半场，SECOND_HALF=下半场，OVERTIME=加时',
  `timeout_condition` varchar(16) DEFAULT NULL COMMENT '暂停条件：BEFORE_START=历史赛前，HALFTIME=中场，FRONTCOURT=前场控球，DEAD_BALL=死球',

  `penalty_subject` varchar(16) DEFAULT NULL COMMENT '犯规主体：player=球员，goalie=守门员，official=球队官员',
  `penalty_type` varchar(20) DEFAULT NULL COMMENT '判罚类型：turnover=仅换球权，minor=绿牌，major=黄牌，expulsion=红牌',
  `penalty_seconds` smallint unsigned DEFAULT NULL COMMENT '判罚总秒数',
  `served_by_roster_id` bigint DEFAULT NULL COMMENT '代罚球员名单ID',
  `served_by_name` varchar(100) DEFAULT NULL COMMENT '无法关联名单时的服罚球员号码或姓名快照',
  `foul_item` varchar(100) DEFAULT NULL COMMENT '犯规项或裁判说明',

  `related_event_id` bigint DEFAULT NULL COMMENT '关联原事件ID，用于更正、作废说明或补充',
  `event_status` varchar(16) NOT NULL DEFAULT 'VALID' COMMENT '记录状态：VALID=有效，CANCELLED=已作废，REPLACED=已被更正',
  `before_value` varchar(255) DEFAULT NULL COMMENT '更正前值',
  `after_value` varchar(255) DEFAULT NULL COMMENT '更正后值',
  `correction_reason` varchar(200) DEFAULT NULL COMMENT '更正或作废依据',
  `event_text` varchar(500) DEFAULT NULL COMMENT '事件详细说明快照',
  `rule_reference` varchar(100) DEFAULT NULL COMMENT '事件发生时的规则依据快照',
  `created_by` bigint DEFAULT NULL COMMENT '记录人ID，对应sys_user.id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_events_match_sequence` (`match_id`, `sequence_no`),
  KEY `idx_events_match_period` (`match_id`, `period_no`, `remaining_seconds`),
  KEY `idx_events_match_type` (`match_id`, `event_type`),
  KEY `idx_events_roster` (`roster_id`),
  KEY `idx_events_related` (`related_event_id`),
  CONSTRAINT `fk_events_match`
    FOREIGN KEY (`match_id`) REFERENCES `scoreboard_matches` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_events_roster`
    FOREIGN KEY (`roster_id`) REFERENCES `scoreboard_match_roster` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_events_assist_roster`
    FOREIGN KEY (`assist_roster_id`) REFERENCES `scoreboard_match_roster` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_events_served_by_roster`
    FOREIGN KEY (`served_by_roster_id`) REFERENCES `scoreboard_match_roster` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_events_related`
    FOREIGN KEY (`related_event_id`) REFERENCES `scoreboard_match_events` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛事件记录';

-- 有罚时判罚的当前运行状态；原始判罚内容保存在比赛事件中。
CREATE TABLE IF NOT EXISTS `scoreboard_match_penalties` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `match_id` bigint NOT NULL COMMENT '比赛ID',
  `penalty_event_id` bigint NOT NULL COMMENT '产生本次罚时的判罚事件ID',
  `remaining_seconds` smallint unsigned NOT NULL COMMENT '当前剩余罚时秒数',
  `penalty_state` varchar(16) NOT NULL DEFAULT 'WAITING' COMMENT '罚时状态：WAITING=待就座，SEATED=已就座待复赛，ACTIVE=执行中，EXPIRED=已到时待释放，RELEASED=已释放',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_penalties_event` (`penalty_event_id`),
  KEY `idx_penalties_match_state` (`match_id`, `penalty_state`),
  CONSTRAINT `fk_penalties_match`
    FOREIGN KEY (`match_id`) REFERENCES `scoreboard_matches` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_penalties_event`
    FOREIGN KEY (`penalty_event_id`) REFERENCES `scoreboard_match_events` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛罚时运行状态';

-- 本场球队教练。教练属于球队，可在创建比赛后从 team_coach_rel 补录。
CREATE TABLE IF NOT EXISTS `scoreboard_match_coaches` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `match_id` bigint NOT NULL COMMENT '比赛ID',
  `team_side` varchar(8) NOT NULL COMMENT '所属球队：home=主队，away=客队',
  `team_coach_rel_id` bigint DEFAULT NULL COMMENT '球队教练关系ID，对应team_coach_rel.id，未回填时为空',
  `person_id` bigint DEFAULT NULL COMMENT '人员ID，对应person_profile.id，未回填时为空',
  `coach_type` varchar(32) NOT NULL COMMENT '本场教练类型，对应coach_team_role.item_value',
  `person_name` varchar(100) NOT NULL COMMENT '教练姓名快照',
  `sort_no` int unsigned NOT NULL DEFAULT 0 COMMENT '同类型教练排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coaches_match_side_type_sort` (`match_id`, `team_side`, `coach_type`, `sort_no`),
  KEY `idx_coaches_team_coach_rel` (`team_coach_rel_id`),
  KEY `idx_coaches_person` (`person_id`),
  CONSTRAINT `fk_coaches_match`
    FOREIGN KEY (`match_id`) REFERENCES `scoreboard_matches` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本场球队教练';

-- 本场裁判及记分计时人员，不包含球队教练。
CREATE TABLE IF NOT EXISTS `scoreboard_match_officials` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `match_id` bigint NOT NULL COMMENT '比赛ID',
  `person_id` bigint DEFAULT NULL COMMENT '人员ID，对应person_profile.id，未关联时为空',
  `official_role` varchar(32) NOT NULL COMMENT '比赛角色：REFEREE=主裁判，ASSISTANT_REFEREE=助理裁判，BENCH_OFFICIAL=替补席裁判，SCOREKEEPER=记分计时员',
  `person_name` varchar(100) NOT NULL COMMENT '人员姓名快照',
  `sort_no` int unsigned NOT NULL DEFAULT 0 COMMENT '同角色人员排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_officials_match_role_sort` (`match_id`, `official_role`, `sort_no`),
  KEY `idx_officials_person` (`person_id`),
  CONSTRAINT `fk_officials_match`
    FOREIGN KEY (`match_id`) REFERENCES `scoreboard_matches` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本场裁判及记分人员';
