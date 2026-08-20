# 个人云端记账本 - AI 模块需求分析说明书 V1.0

| 文档版本 | 修改日期   | 修改人 | 修改内容                           | 备注     |
| :------- | :--------- | :----- | :--------------------------------- | :------- |
| V1.0     | 2026-08-19 | -      | 初稿创建（Grill-Me 决策树产出）    | 待评审   |

## 1. 引言

### 1.1 背景

概要设计 V1.5 第 1.2 节明确提出"预留 AI 智能体接口"作为扩展目标，第 3 节功能全景图预留了"语义解析 + 智能建议"两个方向。本文档基于 Grill-Me 多轮决策讨论结果，对 AI 模块的需求、架构、技术选型进行完整定义。

### 1.2 AI Agent 定位

| 维度         | 定位                                                         |
| :----------- | :----------------------------------------------------------- |
| **目标用户** | 终端用户（P0 级）：职场新人、大学生、自由职业者              |
| **核心价值** | 从"3 秒极速记账"升级到"1 秒说一句话记账"，提供主动式财务洞察 |
| **上线要求** | 需上线生产环境，代码后续开源至 GitHub                        |
| **非目标**   | 不做开发者技术演示（P1 级暂不考虑），不做 B 端团队协作        |

### 1.3 术语表

| 术语           | 解释                                                        |
| :------------- | :---------------------------------------------------------- |
| Agent          | AI 智能体，能理解用户意图、调用工具、返回自然语言结果       |
| Tool Call      | 大模型根据用户意图自动选择并调用后端方法（如记账、查询统计）|
| RAG            | 检索增强生成，从知识库检索相关文档拼入 Prompt 提升回答质量  |
| SSE            | Server-Sent Events，服务端推送流式响应给前端                |
| LangChain4j    | Java 生态的 LLM 应用开发框架，提供 Agent/Tool/Memory 能力   |
| DashScope      | 阿里云百炼大模型服务平台，提供通义千问系列模型的 API 调用   |
| Milvus         | 开源向量数据库，用于存储和检索 RAG 知识库的 Embedding 向量  |

## 2. 功能场景清单

### 2.1 MVP 场景总览

| 编号 | 场景名称         | 类型     | 触发方式               | 核心能力                               |
| :--- | :--------------- | :------- | :--------------------- | :------------------------------------- |
| A1   | 自然语言记账     | 输入侧   | 用户在聊天框输入文本   | 语义解析→结构化→直接写入账目           |
| A2   | 小票 OCR 记账    | 输入侧   | 用户上传小票/截图      | 多模态模型识别→结构化→直接写入账目    |
| B2   | 预算智能推荐     | 分析侧   | 用户询问或月初触发     | 基于近 3 个月消费数据推荐各分类预算   |
| B3   | 财务周报生成     | 分析侧   | 每周一 09:00 定时触发 | 汇总数据→找异常→给建议→推送至聊天窗口 |
| B4   | 个性化省钱建议   | 分析侧   | 用户询问               | 基于消费模式给出可执行省钱建议         |
| C    | 自然语言数据问答 | 问答侧   | 用户在聊天框提问       | 理解意图→调用查询接口→自然语言回答    |
| D2   | 未来支出预测     | 规划侧   | 用户询问               | 基于历史数据预测下月总支出及各分类支出 |

### 2.2 场景详解

#### A1 - 自然语言记账

**用户故事**：用户说「昨天晚上和同事吃饭花了 258」

**AI 解析结果**：
```json
{
  "type": 0,
  "category": "餐饮",
  "amount": 258.00,
  "accountDate": "2026-08-18",
  "remark": "和同事吃饭"
}
```

**执行流程**：
1. 用户在聊天框输入自然语言
2. AI 解析意图为"记账"，提取结构化字段
3. 直接调用 `/api/account/add`（幂等）写入数据库
4. 返回记账预览卡片：「已记录：餐饮 -258 元（8月18日）」，支持点击修改/撤销

**权限**：直接写入，无需二次确认（A1 是核心高频场景，确认会破坏"1 秒记账"体验）

#### A2 - 小票 OCR 记账

**用户故事**：用户上传一张餐饮小票照片

**AI 解析流程**：
1. 用户上传图片（前端转 Base64 或上传文件）
2. 调用多模态模型（通义千问 VL）识别小票内容
3. 提取关键字段：商户名、金额、日期、商品明细
4. 自动归类（基于商户名匹配分类）
5. 返回记账预览卡片供用户确认后写入

**权限**：直接写入（同 A1 逻辑），预览卡片支持修改

#### B2 - 预算智能推荐

**用户故事**：用户说「帮我推荐下月预算」

**AI 执行流程**：
1. 调用统计接口获取近 3 个月各分类实际支出
2. AI 分析消费趋势（环比/同比），给出各分类建议值
3. 结合 RAG 理财知识库给出推荐理由
4. 返回预算建议卡片，用户确认后批量写入预算表

**权限**：二次确认（写入预算需要用户确认）

#### B3 - 财务周报生成

**用户故事**：每周一早上 09:00，用户在聊天窗口收到 AI 生成的周报

**执行流程**（单个 Tool 方法内部串行完成，无需多 Agent）：
1. `@Scheduled(cron = "0 0 9 ? * MON")` 触发
2. Redisson 分布式锁防多实例重复执行
3. 调用统计 Service 获取本周收支汇总
4. 调用预算 Service 获取预算执行情况
5. 调用省钱建议 Service 找出异常消费点
6. 查询 RAG 知识库匹配相关理财建议
7. AI 整合以上信息生成自然语言周报
8. 通过 SSE 推送至用户聊天窗口
9. 写入 `ai_chat_message` 表（role=assistant）

**权限**：系统主动推送，无写操作

**失败处理**：写入 `ai_report_task` 表记录状态，失败定时重试 3 次（指数退避 10s/30s/90s）

#### B4 - 个性化省钱建议

**用户故事**：用户说「我有什么可以省钱的」

**AI 执行流程**：
1. 调用统计接口获取消费分类占比和趋势
2. AI 识别异常消费模式（如某分类环比增长 > 30%）
3. 检索 RAG 理财知识库匹配省钱建议
4. 结合用户实际数据给出可执行建议

**输出示例**：「你本周奶茶消费 5 杯共 125 元，建议每周控制在 3 杯以内，每月可省约 120 元。」

**权限**：只读，无写操作

#### C - 自然语言数据问答

**用户故事**：用户问「我 7 月外卖花了多少」「今年最大的 3 笔支出」

**AI 执行流程**：
1. AI 理解用户意图，选择调用对应查询 Tool
2. Tool 内部调用现有接口（`/api/statistics/dashboard`、`/api/account/page` 等）
3. AI 将结构化结果转换为自然语言回答

**权限**：只读

#### D2 - 未来支出预测

**用户故事**：用户问「预测我下个月要花多少钱」

**AI 执行流程**：
1. 获取近 6 个月各分类支出数据
2. AI 基于趋势预测下月各分类支出
3. 汇总给出总支出预测值和置信区间

**权限**：只读

## 3. 技术选型

### 3.1 AI 框架选型

| 分类           | 技术组件           | 版本    | 选型理由                                                     |
| :------------- | :----------------- | :------ | :----------------------------------------------------------- |
| **AI 框架**    | LangChain4j        | 1.x GA  | Java 生态最成熟的 LLM 框架；@AiService + @Tool + @MemoryId 三件套覆盖全部场景 |
| **Spring 集成**| langchain4j-spring-boot-starter | 对齐 LangChain4j 版本 | 官方 Starter，自动配置 Bean                          |
| **大模型服务** | 阿里云百炼 DashScope | -     | 通义千问 OpenAI 兼容模式，LangChain4j 自带 OpenAI 模块改 base-url 即用 |
| **备用模型**   | 智谱 GLM-4 / DeepSeek | -    | 免费额度，主模型故障时自动切换                               |

### 3.2 LangChain4j 核心能力映射

| LangChain4j 能力       | 对应场景                          | 实现方式                                    |
| :--------------------- | :-------------------------------- | :------------------------------------------ |
| `@AiService` 接口代理  | 统一聊天入口                      | 定义 `LedgerAiService` 接口，框架自动代理   |
| `@Tool` 注解           | A1/A2/B2/B3/B4/C/D2 全部 7 个场景 | `AiTools` 类中定义 10~15 个 @Tool 方法      |
| `@MemoryId`            | 会话上下文隔离                    | 按用户 ID + 会话 ID 隔离，自动管理记忆窗口  |
| `ChatMemory` 自定义    | 跨设备会话同步                    | Redis+MySQL 双层存储实现                    |
| `Structured Output`    | A1 结构化记账                     | 指定返回类型为 `AccountAddDTO`，自动注入 Schema |
| `UserMessage + ImageData` | A2 小票 OCR                   | 多模态模型接收图片 + 文本指令               |
| `StreamingChatLanguageModel` | SSE 流式响应               | `TokenStream` / `Flux` 推送至 `SseEmitter`  |

### 3.3 免费模型清单（个人开发者优先）

| 提供方         | 模型               | 免费额度                      | 多模态 | 备注                       |
| :------------- | :----------------- | :---------------------------- | :----- | :------------------------- |
| 阿里云百炼     | qwen-plus          | 新用户 500 万 Token（约 3 个月）| 否     | 文本主力模型                |
| 阿里云百炼     | qwen-vl-plus       | 按次计费（0.008 元/次）        | 是     | A2 小票 OCR 用             |
| 智谱 AI        | glm-4              | 新用户 500 万 Token            | 否     | 备用文本模型                |
| DeepSeek       | deepseek-chat      | 有限免费额度                   | 否     | 备用文本模型                |

> **注**：免费额度以各平台最新政策为准，上线前需复核。A2 多模态场景建议预算少量费用（预计 < 10 元/月）。

## 4. 架构设计

### 4.1 单 Agent + 多 Tool 架构

```
┌─────────────────────────────────────────────────────────────┐
│                    用户聊天界面                             │
│            (SSE 流式响应 + 会话上下文)                       │
└──────────────────────┬──────────────────────────────────────┘
                       │ SSE
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              LedgerAiService (@AiService)                   │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐              │
│  │ ChatMemory │  │ RAG 检索  │  │ Tool 调用 │              │
│  │ (Redis+DB)│  │ (Milvus)  │  │ (10~15个) │              │
│  └───────────┘  └───────────┘  └───────────┘              │
│                                                             │
│  AiTools 类 (@Tool 方法清单):                               │
│  ┌─────────────┬──────────────┬──────────────┐             │
│  │ addAccount  │addAccountOcr │ queryAccount│             │
│  │ getDashboard│ getBudget    │ recommendBudget│           │
│  │ suggestSavings│predictExpense│generateWeeklyReport│     │
│  └─────────────┴──────────────┴──────────────┘             │
└──────────────────────┬──────────────────────────────────────┘
                       │ Tool 内部调用
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              现有业务 Service 层                            │
│  AccountService / StatisticsService / BudgetService / ...  │
└─────────────────────────────────────────────────────────────┘
```

**设计决策**：
- 不引入多 Agent 编排框架（Harness），单 Agent + 多 Tool 足够覆盖 7 个场景
- B3 周报生成的"多步骤"在单个 Tool 方法内部串行完成，无需拆分为多 Agent
- 当 Tool 数量未来超过 25 个时，可迁移为 Router Agent + 子 Agent 架构

### 4.2 Tool 方法清单

| Tool 方法                | 对应场景 | 权限     | 调用的现有接口/Service            |
| :----------------------- | :------- | :------- | :-------------------------------- |
| `addAccount(...)`        | A1       | 直接写入 | `AccountService.add()`            |
| `addAccountByOcr(...)`   | A2       | 直接写入 | 多模态模型 + `AccountService.add()`|
| `getDashboard(...)`      | C        | 只读     | `StatisticsService.dashboard()`   |
| `queryAccount(...)`      | C        | 只读     | `AccountService.page()`           |
| `getBudget(...)`         | B2/C     | 只读     | `BudgetService.list()`            |
| `recommendBudget(...)`   | B2       | 二次确认 | `StatisticsService` + AI 分析     |
| `suggestSavings(...)`    | B4       | 只读     | `StatisticsService` + RAG 检索     |
| `predictExpense(...)`    | D2       | 只读     | `StatisticsService` + AI 预测     |
| `generateWeeklyReport()` | B3       | 系统推送 | 多个 Service 串行调用 + AI 整合   |

### 4.3 交互流程（以 A1 自然语言记账为例）

```
用户输入: "昨天晚上和同事吃饭花了258"
    │
    ▼
LedgerAiService.chat(@MemoryId userId, @UserMessage text)
    │
    ├─ 1. ChatMemory 从 Redis 加载最近 20 条消息作为上下文
    │
    ├─ 2. RAG 检索（可选）：查询理财知识库匹配相关文档
    │
    ├─ 3. 模型推理：识别意图=记账，选择 addAccount Tool
    │
    ├─ 4. Tool 执行：addAccount(userId, type=0, category="餐饮",
    │      amount=258, accountDate="2026-08-18", remark="和同事吃饭")
    │      └─ 内部调用 AccountService.add() → 写入数据库 → 清除缓存
    │
    ├─ 5. 模型二次推理：整合 Tool 返回结果生成自然语言回复
    │
    └─ 6. SSE 流式推送回复给前端：
         "已记录：餐饮 -258元（8月18日）[查看/修改/撤销]"
```

## 5. 数据权限策略

### 5.1 数据可见范围

| 范围         | 说明                                                         |
| :----------- | :----------------------------------------------------------- |
| **用户数据** | 每个 Tool 调用前强制注入当前登录 userId（从 SecurityContextHolder 获取），AI 只能访问该 userId 的数据 |
| **RAG 知识库** | 外挂理财常识、记账分类说明、省钱技巧等公开知识，B2/B4 场景引用增强建议质量 |

### 5.2 写操作权限矩阵

| 场景             | 写操作         | 权限策略                                                     |
| :--------------- | :------------- | :----------------------------------------------------------- |
| A1 自然语言记账  | 新增账目       | **直接写入**，返回预览卡片支持修改/撤销                      |
| A2 小票 OCR 记账 | 新增账目       | **直接写入**，返回预览卡片支持修改                           |
| B2 预算推荐      | 批量写入预算   | **二次确认**，返回预算建议卡片，用户确认后写入               |
| C 问答           | 无写操作       | 只读                                                         |
| B4 省钱建议       | 无写操作       | 只读                                                         |
| D2 支出预测       | 无写操作       | 只读                                                         |
| B3 周报生成       | 写入聊天消息   | 系统推送，无业务数据写入                                     |

### 5.3 高危操作黑名单

以下操作 AI **绝对禁止**执行，即使在 Tool 方法中存在，也在 Agent 层拦截：

| 黑名单操作             | 原因                                   |
| :--------------------- | :------------------------------------- |
| 修改密码 /api/user/password | 涉及账号安全，必须走原页面             |
| 账号注销 /api/user/delete   | 不可逆操作，必须走原页面               |
| 删除单条账目 /api/account/delete/{id} | 不可逆操作，用户必须手动确认 |
| 导出 Excel /api/export/excel | 异步流程 + 文件下载，必须走原页面      |
| 刷新 Token /api/auth/refresh | 鉴权相关，必须走原流程                 |

## 6. 数据存储方案

### 6.1 新增数据表设计

#### 6.1.1 AI 聊天会话表（ai_chat_session）

```sql
CREATE TABLE `ai_chat_session` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL COMMENT '会话唯一标识（UUID）',
  `user_id` bigint(20) NOT NULL,
  `title` varchar(100) DEFAULT NULL COMMENT '会话标题（取首条消息摘要）',
  `status` tinyint(1) DEFAULT '1' COMMENT '1-活跃，0-已归档',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 6.1.2 AI 聊天消息表（ai_chat_message）

```sql
CREATE TABLE `ai_chat_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `role` varchar(20) NOT NULL COMMENT 'user/assistant/tool',
  `content` text NOT NULL COMMENT '消息内容',
  `extra_json` json DEFAULT NULL COMMENT 'Tool调用信息、Token用量、模型名称等',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 6.1.3 AI 周报任务表（ai_report_task）

```sql
CREATE TABLE `ai_report_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `report_type` varchar(20) NOT NULL DEFAULT 'WEEKLY' COMMENT 'WEEKLY/MONTHLY',
  `period` varchar(20) NOT NULL COMMENT '2026-W33 或 2026-08',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0-待处理，1-处理中，2-已完成，3-失败',
  `content` text DEFAULT NULL COMMENT '生成的周报内容',
  `error_msg` varchar(500) DEFAULT NULL,
  `retry_count` int(11) DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_type_period` (`user_id`, `report_type`, `period`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 6.1.4 RAG 知识文档表（ai_knowledge_document）

```sql
CREATE TABLE `ai_knowledge_document` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL,
  `chunk_content` text NOT NULL COMMENT '切分后的文档片段',
  `category` varchar(50) NOT NULL COMMENT 'SAVING_TIPS/BUDGET_GUIDE/CATEGORY_DESC等',
  `source` varchar(200) DEFAULT NULL COMMENT '来源',
  `milvus_id` varchar(64) DEFAULT NULL COMMENT 'Milvus 中对应的向量ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 6.2 存储架构

| 存储组件 | 用途                           | Key/表设计                                    | 过期策略        |
| :------- | :----------------------------- | :-------------------------------------------- | :-------------- |
| **Redis** | 聊天消息热缓存（最近 100 条）   | `ai:chat:{sessionId}` (List)                 | 24 小时自动过期 |
| **Redis** | 用户日配额计数器               | `ai:quota:{userId}:{date}` (String)          | 当天 23:59:59   |
| **Redis** | 周报去重标记                   | `report:{userId}:{yearWeek}` (String)        | 7 天            |
| **MySQL** | 聊天会话/消息冷存储             | `ai_chat_session` / `ai_chat_message` 表     | 永久（按需归档）|
| **MySQL** | 周报任务状态                   | `ai_report_task` 表                           | 永久            |
| **MySQL** | RAG 知识文档原文               | `ai_knowledge_document` 表                   | 永久            |
| **Milvus**| RAG 知识向量索引               | Collection: `ledger_knowledge`               | 永久            |

### 6.3 会话记忆策略

| 策略             | 说明                                                         |
| :--------------- | :----------------------------------------------------------- |
| **上下文窗口**   | 最近 20 条消息（约 4000~8000 Token），超出自动截断最旧消息   |
| **热缓存**       | Redis List 存最近 100 条消息，读取时优先命中 Redis           |
| **冷存储**       | 所有消息写入 MySQL `ai_chat_message` 表，Redis 未命中时回查  |
| **会话隔离**     | `@MemoryId` = `userId + ":" + sessionId`，用户间/会话间隔离  |
| **跨设备同步**   | 用户换设备登录时，从 MySQL 拉取历史会话列表和消息            |

## 7. 高可用设计

### 7.1 主备双模型热切

```
用户请求
    │
    ▼
┌──────────────┐     成功      ┌──────────────┐
│ 主模型: 百炼 │ ──────────► │ 返回结果     │
│ qwen-plus    │             └──────────────┘
└──────┬───────┘
       │ 失败（超时/5xx/限流）
       ▼
┌──────────────┐     成功      ┌──────────────┐
│ 备用模型:     │ ──────────► │ 返回结果     │
│ 智谱 GLM-4   │             └──────────────┘
│ 或 DeepSeek  │
└──────┬───────┘
       │ 仍失败
       ▼
┌──────────────────────────────────────────┐
│ 降级文案:                                 │
│ "AI 助手暂时不可用，请稍后再试。          │
│  你的记账/统计功能不受影响。              │
│  A1 记账降级: 返回预填表单（金额已填好）"  │
└──────────────────────────────────────────┘
```

### 7.2 用户日限额

| 维度       | 限制值                     | Redis Key                    | 说明                     |
| :--------- | :------------------------- | :--------------------------- | :----------------------- |
| 对话次数   | 50 次/天/用户              | `ai:quota:{userId}:{date}`  | 正常使用 5~10 次/天      |
| Token 用量 | 10 万 Token/天/用户         | `ai:token:{userId}:{date}`   | 正常使用约 1~2 万/天     |

超额返回友好提示：「今日 AI 助手使用已达上限，明日重置。」

### 7.3 分场景超时与重试

| 场景               | 超时时间 | 重试策略                       | 说明                           |
| :----------------- | :------- | :----------------------------- | :----------------------------- |
| 普通对话 (A1/C等)  | 30s      | 不重试（用户主动重发）         | —                              |
| A2 小票 OCR        | 60s      | 不重试                         | 多模态响应较慢                 |
| B3 周报生成        | 120s     | 重试 3 次（10s/30s/90s 退避）  | 定时任务内，可容忍等待         |
| SSE 首 Token       | 15s      | 超时则降级为非流式             | 15s 无输出切换降级             |
| SSE chunk 间隔     | 30s      | 超时则结束流                   | 防止连接挂死                   |

### 7.4 降级场景明细

| 场景 | 降级行为                                                     |
| :--- | :----------------------------------------------------------- |
| A1   | AI 不可用时，返回预填记账表单（从用户输入提取金额填入）      |
| A2   | AI 不可用时，提示「OCR 识别暂不可用，请手动填写记账表单」    |
| C    | AI 不可用时，提示「AI 问答暂不可用，请直接查看仪表盘」       |
| B2   | AI 不可用时，降级为简单均值推荐（近 3 月均值，无 AI 分析）   |
| B3   | AI 不可用时，跳过本周周报，下次定时任务补发                  |
| B4   | AI 不可用时，提示「省钱建议暂不可用」                        |
| D2   | AI 不可用时，降级为简单线性外推（无 AI 推理）                |

## 8. 部署架构变更

### 8.1 新增组件

| 组件    | 版本  | 部署方式     | 资源要求                     |
| :------ | :---- | :----------- | :--------------------------- |
| Milvus  | 2.4+  | Docker 容器  | 内存 1GB（单机 Standalone 模式） |
| etcd    | 3.5+  | Docker 容器  | Milvus 元数据存储（随 Milvus 部署） |
| minio   | 最新  | Docker 容器  | Milvus 对象存储（随 Milvus 部署） |

### 8.2 部署架构图（V2.0 含 AI 模块）

```
┌─────────────────────────────────────────────────────────────────┐
│                        互联网 / 用户                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Nginx (反向代理)                            │
│                   ├── SSL 终止 (HTTPS)                         │
│                   └── SSE 长连接超时配置 (proxy_read_timeout 120s)│
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot App (容器)                       │
│                   ├── 端口: 8080                               │
│                   ├── LangChain4j Agent                        │
│                   └── 实例数: 1 (V1.0)                        │
└─────────────────────────────────────────────────────────────────┘
          │              │              │              │
          ▼              ▼              ▼              ▼
┌───────────────┐ ┌───────────┐ ┌───────────┐ ┌───────────────┐
│  MySQL 8.0    │ │ Redis 7   │ │ Milvus    │ │ 百炼 API      │
│  (AI聊天表 +  │ │ (会话缓存  │ │ (RAG向量  │ │ (云大模型     │
│   原有业务表) │ │  + 配额)  │ │   索引)   │ │   外部服务)   │
└───────────────┘ └───────────┘ └───────────┘ └───────────────┘
                                                        │
                                                        ▼
                                               ┌───────────────┐
                                               │ 智谱/DeepSeek │
                                               │ (备用模型)    │
                                               └───────────────┘
```

### 8.3 Docker Compose 变更

新增以下服务到 `docker-compose.yml`：

```yaml
services:
  etcd:
    image: quay.io/coreos/etcd:v3.5.5
    environment:
      ETCD_AUTO_COMPACTION_MODE: periodic
      ETCD_AUTO_COMPACTION_RETENTION: "0"
      ETCD_QUOTA_BACKEND_BYTES: "4294967296"
    volumes:
      - etcd_data:/etcd
    command: etcd -advertise-client-urls=http://127.0.0.1:2379 -listen-client-urls http://0.0.0.0:2379

  minio:
    image: minio/minio:latest
    environment:
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
    volumes:
      - minio_data:/minio/data
    command: minio server /minio/data

  milvus:
    image: milvusdb/milvus:v2.4.0
    command: ["milvus", "run", "standalone"]
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000
    ports:
      - "19530:19530"
    depends_on:
      - etcd
      - minio

volumes:
  etcd_data:
  minio_data:
```

## 9. 接口设计

### 9.1 AI 接口总览

| 接口路径                      | 方法 | 功能                | 认证 | 响应类型 |
| :---------------------------- | :--- | :------------------ | :--- | :------- |
| `/api/ai/chat`                | POST | 发送消息（流式响应）| 是   | SSE      |
| `/api/ai/chat/upload`         | POST | 上传图片（A2 OCR）  | 是   | JSON     |
| `/api/ai/session/list`        | GET  | 查询会话列表        | 是   | JSON     |
| `/api/ai/session/messages`    | GET  | 查询会话历史消息    | 是   | JSON     |
| `/api/ai/session/delete`      | DELETE | 删除会话           | 是   | JSON     |
| `/api/ai/quota`               | GET  | 查询今日剩余配额    | 是   | JSON     |

### 9.2 聊天接口（SSE 流式）

```
POST /api/ai/chat
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "sessionId": "uuid-xxx",
  "message": "今天打车花了35"
}

Response: text/event-stream
data: {"type":"chunk","content":"已"}
data: {"type":"chunk","content":"记录"}
data: {"type":"tool_call","tool":"addAccount","result":{"id":123,"category":"交通","amount":35}}
data: {"type":"done","content":"已记录：交通 -35元（8月19日）[修改/撤销]"}
```

### 9.3 图片上传接口（A2 小票 OCR）

```
POST /api/ai/chat/upload
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}

Response: application/json
{
  "code": 0,
  "data": {
    "sessionId": "uuid-xxx",
    "parsedAccount": {
      "type": 0,
      "category": "餐饮",
      "amount": 258.00,
      "accountDate": "2026-08-19",
      "remark": "老乡鸡"
    },
    "previewCard": "已识别：餐饮 258元（老乡鸡）[确认记账/修改]"
  }
}
```

## 10. 安全设计

| 机制               | 实现方式                                                     |
| :----------------- | :----------------------------------------------------------- |
| **鉴权**           | 复用现有 JWT 双 Token 体系，AI 接口全部需要 Access Token     |
| **用户隔离**       | 每个 Tool 方法第一参数为 userId，从 SecurityContextHolder 获取 |
| **配额限制**       | Redis 计数器，50 次/天/用户 + 10 万 Token/天/用户            |
| **黑名单拦截**     | Agent 层 Tool 调用前置拦截器，禁止高危操作                   |
| **Prompt 注入防护**| 对用户输入做基础过滤（移除特殊指令词如"忽略以上指令"等）     |
| **API Key 安全**   | 百炼/智谱 API Key 存环境变量，不硬编码，不入 Git             |
| **日志脱敏**       | AI 聊天日志不记录完整金额数据（生产环境可选关闭聊天记录日志）|

## 11. 可观测性

| 指标类别     | 指标名称                    | 采集方式                          | 告警阈值      |
| :----------- | :-------------------------- | :-------------------------------- | :------------ |
| **业务指标** | AI 对话成功率               | `ledger.ai.chat.success.rate`     | < 95% 告警    |
| **业务指标** | AI 记账解析成功率           | `ledger.ai.account.parse.rate`     | < 90% 告警    |
| **业务指标** | AI 小票识别准确率           | `ledger.ai.ocr.accuracy.rate`      | < 80% 告警    |
| **业务指标** | 周报生成成功率              | `ledger.ai.report.success.rate`    | < 95% 告警    |
| **性能指标** | AI 首 Token 响应时间        | `ledger.ai.first.token.duration`   | P95 > 5s 告警 |
| **性能指标** | AI 完整响应时间             | `ledger.ai.total.duration`         | P95 > 30s 告警|
| **成本指标** | 日均 Token 消耗             | `ledger.ai.token.daily.count`     | 监控趋势      |
| **成本指标** | 模型切换次数                | `ledger.ai.model.fallback.count`   | 突增告警      |

## 12. 风险分析

| 风险项                    | 影响 | 概率 | 缓解措施                                                     |
| :------------------------ | :--- | :--- | :----------------------------------------------------------- |
| **免费额度耗尽**          | 高   | 中   | 主备双模型 + 用户日限额 + 成本监控告警                       |
| **百炼 API 宕机**         | 高   | 低   | 自动切换备用模型（智谱/DeepSeek）+ 降级文案                  |
| **A1 解析错误率偏高**     | 中   | 中   | System Prompt 约束输出格式 + 结构化 Schema 校验 + 预览卡片   |
| **A2 OCR 准确率不足**     | 中   | 中   | 多模态模型 + 预览卡片让用户确认后写入                       |
| **Prompt 注入攻击**      | 中   | 低   | 输入过滤 + 黑名单 Tool 拦截 + System Prompt 边界约束         |
| **Milvus 单点故障**       | 中   | 低   | Milvus Standalone 模式重启自动恢复；RAG 降级为跳过知识库     |
| **聊天历史 Redis 丢失**   | 低   | 中   | Redis 仅做热缓存，MySQL 冷存储兜底                           |
| **Token 费用超预期**     | 高   | 中   | 用户日限额 + 成本指标监控 + 周报批量任务控制调用频率         |

## 13. 项目里程碑（AI 模块）

| 阶段                              | 交付物                                   |
| :-------------------------------- | :--------------------------------------- |
| 环境搭建：Milvus + 百炼 API 配置  | Docker Compose + application-ai.yml      |
| LangChain4j 集成 + AiService 骨架 | AiService 接口 + AiTools 类 + ChatMemory |
| A1 自然语言记账（Tool + 写入）    | 聊天接口 + 结构化解析 + 预览卡片         |
| A2 小票 OCR 记账（多模态）         | 图片上传 + 多模态调用 + 解析             |
| C 自然语言问答（Tool 调用查询）   | 问答 Tool + 上下文记忆                   |
| B2/B4/D2 分析类 Tool               | 预算推荐 + 省钱建议 + 支出预测           |
| B3 周报定时生成 + 推送             | 定时任务 + 周报 Tool + SSE 推送          |
| RAG 知识库搭建（Milvus + 文档）    | 知识库初始化 + 检索集成                  |
| 高可用：主备切换 + 限额 + 降级     | 双模型配置 + 配额拦截 + 降级文案         |
| 安全：黑名单 + Prompt 注入防护     | Tool 拦截器 + 输入过滤                   |
| 可观测性埋点                       | Prometheus 指标 + Grafana 大盘           |
| 联调 + 测试 + 文档                 | 全场景联调 + Knife4j 可调试               |

## 14. 配置文件规划

### 14.1 新增配置文件

| 文件                    | 用途                              |
| :---------------------- | :-------------------------------- |
| `application-ai.yml`   | AI 模块独立配置（百炼 Key、模型参数、超时、限额） |
| `redisson-ai.yaml`     | AI Redis 命名空间隔离配置（可选）  |

### 14.2 配置项规划

```yaml
# application-ai.yml
ai:
  # 主模型（百炼 DashScope）
  primary:
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${DASHSCOPE_API_KEY}
    model-name: qwen-plus
    multimodal-model-name: qwen-vl-plus
    timeout: 30s

  # 备用模型（智谱）
  fallback:
    base-url: https://open.bigmodel.cn/api/paas/v4
    api-key: ${ZHIPU_API_KEY}
    model-name: glm-4
    timeout: 30s

  # 用户配额
  quota:
    daily-chat-limit: 50
    daily-token-limit: 100000

  # 场景超时
  timeout:
    normal: 30s
    ocr: 60s
    weekly-report: 120s
    sse-first-token: 15s
    sse-chunk-interval: 30s

  # RAG 知识库
  rag:
    milvus:
      host: ${MILVUS_HOST:localhost}
      port: 19530
      collection: ledger_knowledge
    embedding-model: text-embedding-v2  # 百炼 Embedding 模型

  # 周报定时任务
  weekly-report:
    cron: "0 0 9 ? * MON"
    retry-max: 3
    retry-backoff: 10s,30s,90s
```
