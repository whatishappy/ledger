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
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL（MinIO路径）',
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
  `template_id` bigint(20) DEFAULT NULL COMMENT '来源模板ID（可选）',
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

-- ------------------------------------------------------------
-- 5.1 标签表（tag）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `color` varchar(7) DEFAULT '#1890ff' COMMENT '标签颜色（HEX格式）',
  `type` tinyint(1) DEFAULT '0' COMMENT '标签类型：0-自定义，1-系统',
  `sort` int(11) DEFAULT '0' COMMENT '排序值（越小越靠前）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  UNIQUE KEY `uk_user_name` (`user_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义标签表';

-- ------------------------------------------------------------
-- 5.2 账目-标签关联表（account_tag）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `account_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `account_id` bigint(20) NOT NULL COMMENT '账目ID',
  `tag_id` bigint(20) NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_tag` (`account_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账目与标签多对多关联表';

-- ------------------------------------------------------------
-- 5.3 交易模板表（transaction_template）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `type` tinyint(1) NOT NULL COMMENT '收支类型：1-收入，0-支出',
  `category` varchar(50) NOT NULL COMMENT '收支分类',
  `amount` decimal(12,2) DEFAULT NULL COMMENT '预设金额',
  `remark` varchar(255) DEFAULT NULL COMMENT '预设备注',
  `tags` json DEFAULT NULL COMMENT '预设标签ID数组',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快速记账模板表';

-- ------------------------------------------------------------
-- 5.4 交易图片表（transaction_image）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction_image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `account_id` bigint(20) NOT NULL COMMENT '关联账目ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片存储URL（MinIO路径）',
  `image_type` tinyint(1) DEFAULT '1' COMMENT '图片类型：1-小票/凭证，2-其他',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_id` (`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账目附件图片表';

-- ------------------------------------------------------------
-- 5.5 定时交易表（scheduled_transaction）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `scheduled_transaction` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '定时任务ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `cron` varchar(50) NOT NULL COMMENT 'Cron表达式',
  `type` tinyint(1) NOT NULL COMMENT '收支类型：1-收入，0-支出',
  `category` varchar(50) NOT NULL COMMENT '收支分类',
  `amount` decimal(12,2) NOT NULL COMMENT '交易金额',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `next_run_at` datetime DEFAULT NULL COMMENT '下次执行时间',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用，0-停用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_next_run` (`next_run_at`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周期性自动记账任务表';

-- ------------------------------------------------------------
-- 5.6 AI对话会话表（ai_chat_session）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `title` varchar(255) DEFAULT NULL COMMENT '会话标题',
  `last_message_at` datetime DEFAULT NULL COMMENT '最后消息时间',
  `message_count` int(11) DEFAULT '0' COMMENT '消息总数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手对话会话表';

-- ------------------------------------------------------------
-- 5.7 AI对话消息表（ai_chat_message）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint(20) NOT NULL COMMENT '所属会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色：user/assistant/system',
  `content` text COMMENT '消息内容',
  `tokens` int(11) DEFAULT '0' COMMENT '消耗Token数',
  `tool_calls` json DEFAULT NULL COMMENT '工具调用记录',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息明细表';

-- ------------------------------------------------------------
-- 5.8 AI报告任务表（ai_report_task）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_report_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `type` varchar(20) NOT NULL COMMENT '报告类型：weekly/monthly/yearly/custom',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0-待生成，1-生成中，2-已完成，3-失败',
  `content` text COMMENT '报告内容（Markdown）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI智能报告生成任务表';

-- ------------------------------------------------------------
-- 5.9 AI知识库文档表（ai_knowledge_document）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `doc_type` varchar(20) NOT NULL COMMENT '文档类型：guide/rule/faq',
  `title` varchar(255) NOT NULL COMMENT '文档标题',
  `content` text NOT NULL COMMENT '文档内容',
  `embedding_id` varchar(100) DEFAULT NULL COMMENT '向量存储ID',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态：0-待索引，1-已索引，2-已禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_doc_type` (`doc_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI助手知识库文档表';
