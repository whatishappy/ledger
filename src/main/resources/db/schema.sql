-- ============================================================
-- 个人云端记账本 - 数据库初始化脚本
-- 基于《详细设计说明书 V1.4》§10.2 表结构设计
-- ============================================================

-- ------------------------------------------------------------
-- 4.1 用户表（user）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '登录用户名',
  `password` varchar(255) NOT NULL COMMENT 'BCrypt加密密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `token_version` int(11) DEFAULT '1' COMMENT 'Token版本号（改密/注销时自增，旧Token失效）',
  `status` tinyint(1) DEFAULT '1' COMMENT '账户状态：1-正常，0-已注销',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

-- ------------------------------------------------------------
-- 4.2 账目表（account_book）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `account_book` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '账目ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `type` tinyint(1) NOT NULL COMMENT '收支类型：1-收入，0-支出',
  `category` varchar(20) NOT NULL COMMENT '收支分类：餐饮/交通/购物/工资/娱乐/其他',
  `amount` decimal(10,2) NOT NULL COMMENT '金额（精确到分）',
  `account_date` date NOT NULL COMMENT '业务发生日期（支持补录）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注描述',
  `extra_json` json DEFAULT NULL COMMENT '扩展JSON字段（AI标签等）',
  `version` int(11) DEFAULT '1' COMMENT '乐观锁版本号',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识：0-未删除，1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '系统入库时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '系统更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `account_date`),
  KEY `idx_user_category` (`user_id`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账目记录主表';

-- ------------------------------------------------------------
-- 4.3 预算表（budget）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `budget` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预算ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `category` varchar(20) NOT NULL COMMENT '支出分类（关联account_book.category）',
  `month` varchar(7) NOT NULL COMMENT '预算月份，格式：YYYY-MM',
  `amount_limit` decimal(10,2) NOT NULL COMMENT '月度预算上限金额',
  `version` int(11) DEFAULT '1' COMMENT '乐观锁版本号',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识：0-未删除，1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_category_month` (`user_id`, `category`, `month`) COMMENT '唯一约束：同用户同分类同月仅一条记录',
  KEY `idx_user_month` (`user_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度预算表';

-- ------------------------------------------------------------
-- 4.4 导出任务表（export_task）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `export_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_id` varchar(64) NOT NULL COMMENT '任务唯一标识（客户端查询凭证）',
  `user_id` bigint(20) NOT NULL COMMENT '发起用户ID',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '任务状态：0-待处理，1-处理中，2-已完成，3-失败，4-已过期',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件下载地址（完成时填充）',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `row_count` int(11) DEFAULT NULL COMMENT '导出行数',
  `error_msg` varchar(255) DEFAULT NULL COMMENT '失败原因（任务失败时填充）',
  `expire_time` datetime DEFAULT NULL COMMENT '文件过期时间（默认创建时间+7天）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务管理表';
