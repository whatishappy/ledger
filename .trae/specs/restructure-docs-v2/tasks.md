# Tasks

- [x] Task 1: 修复完整需求分析 V2.0 交叉引用路径
  - [ ] SubTask 1.1: 将 AI 模块引用路径从 `个人记账本——AI模块需求分析.md` 改为 `docs/个人记账本——AI模块需求分析.md`
  - [ ] SubTask 1.2: 将前端设计引用路径从 `前端页面设计方案.md` 改为 `docs/前端页面设计方案.md`
  - [ ] SubTask 1.3: 在文档头部新增文档层次关系说明，标注本文件为主文档

- [x] Task 2: 概要设计 V1.5 → V2.0 升级
  - [ ] SubTask 2.1: 版本历史表新增 V2.0 行
  - [ ] SubTask 2.2: §1.2 项目目标移除「预留AI智能体接口」，改为「AI 驱动的个人记账」
  - [ ] SubTask 2.3: §2 技术栈新增 Milvus、MinIO、LangChain4j、百炼、智谱、Vue 3、Tailwind CSS
  - [ ] SubTask 2.4: §3 功能全景图移除「AI扩展(预留)」，改为「AI 智能助手」；新增标签/模板/日历/导入/介绍页
  - [ ] SubTask 2.5: §4 数据库设计新增 9 张 V2.0 新表（tag/account_tag/transaction_template/transaction_image/scheduled_transaction/ai_chat_session/ai_chat_message/ai_report_task/ai_knowledge_document）+ user 表加 avatar_url 字段
  - [ ] SubTask 2.6: §5 缓存策略新增 tags/templates/ai:chat/ai:quota 四类 Key
  - [ ] SubTask 2.7: §6 接口设计新增标签/模板/导入/图片/定时交易/统计/头像/AI 接口
  - [ ] SubTask 2.8: §8 部署架构新增 Milvus + etcd + MinIO，Nginx 路由分离介绍页
  - [ ] SubTask 2.9: §10 里程碑替换为 P0/P1/P2 三阶段路线图

- [x] Task 3: 详细设计 V1.4 → V2.0 升级
  - [ ] SubTask 3.1: 版本历史表新增 V2.0 行
  - [ ] SubTask 3.2: §10 数据设计新增 9 张 V2.0 新表 DDL + ER 图更新
  - [ ] SubTask 3.3: §11 接口清单新增 V2.0 接口（标签/模板/导入/图片/定时交易/统计/头像/AI）
  - [ ] SubTask 3.4: §12 错误码表新增 V2.0 错误码（标签/模板/导入/AI 相关）
  - [ ] SubTask 3.5: §13 缓存 Key 表新增 V2.0 缓存 Key

- [x] Task 4: 实施计划新增 V2.0 里程碑
  - [ ] SubTask 4.1: 标题改为 V2.0，版本历史说明 V1.5 已完成
  - [ ] SubTask 4.2: 新增 M11 标签模块（表 + CRUD + 前端选择器）
  - [ ] SubTask 4.3: 新增 M12 交易模板模块（表 + CRUD + 前端）
  - [ ] SubTask 4.4: 新增 M13 交易日历热力图（统计接口 + 前端日历组件）
  - [ ] SubTask 4.5: 新增 M14 支付宝/微信账单导入（解析器 + 导入接口 + 前端上传）
  - [ ] SubTask 4.6: 新增 M15 用户增强（头像上传 + 登录限流 + 草稿保存）
  - [ ] SubTask 4.7: 新增 M16 AI 模块（LangChain4j + AiService + AiTools + SSE + 7 场景）
  - [ ] SubTask 4.8: 新增 M17 介绍页（独立静态 HTML + Tailwind + 6 板块）
  - [ ] SubTask 4.9: 新增 M18 全链路联调 + Docker 部署 V2.0

- [x] Task 5: 接口测试文档新增 V2.0 测试用例
  - [ ] SubTask 5.1: 标题改为 V2.0，说明 V1.5 的 56 条已通过
  - [ ] SubTask 5.2: 新增标签模块测试用例 TC-T01~T04
  - [ ] SubTask 5.3: 新增交易模板模块测试用例 TC-TP01~TP04
  - [ ] SubTask 5.4: 新增账单导入模块测试用例 TC-IM01~IM03
  - [ ] SubTask 5.5: 新增日历统计测试用例 TC-CA01~CA02
  - [ ] SubTask 5.6: 新增 AI 模块测试用例 TC-AI01~AI05

- [x] Task 6: 前端页面设计方案版本号升级
  - [ ] SubTask 6.1: 文档标题版本号改为 V2.0

# Task Dependencies
- [Task 2] depends on [Task 1]（概要设计引用完整需求分析的版本号和路径）
- [Task 3] depends on [Task 2]（详细设计引用概要设计的版本号）
- [Task 4] depends on [Task 2]（实施计划引用概要设计的功能清单）
- [Task 5] depends on [Task 4]（测试文档引用实施计划的里程碑）
- [Task 6] 无依赖，可并行
