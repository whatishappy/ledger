# 个人云端记账本（Personal Ledger）

> 一个面向个人/家庭的云端记账应用，覆盖收支记录、预算、统计、定时记账、账单导入、AI 助手、图片附件、知识库等场景，前后端分离 + Docker 一键部署 + Prometheus/Grafana 可观测性。

[![Java](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-green)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-brightgreen)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue)](#开源协议)

---

## 目录

- [一、项目介绍](#一项目介绍)
- [二、技术栈](#二技术栈)
- [三、项目结构](#三项目结构)
- [四、功能模块](#四功能模块)
- [五、快速开始](#五快速开始)
- [六、配置说明](#六配置说明)
- [七、接口文档与测试](#七接口文档与测试)
- [八、监控与可观测性](#八监控与可观测性)
- [九、路线图](#九路线图)
- [十、常见问题](#十常见问题)
- [十一、开源协议](#十一开源协议)

---

## 一、项目介绍

**个人云端记账本** 是一套生产可用的个人记账解决方案，目标：

- **完整记账闭环**：记一笔 → 预算管控 → 仪表盘分析 → 多年趋势对比
- **AI 智能助手**：自然语言查询、小票 OCR、周报、支出预测、预算推荐、RAG 知识库
- **高可用与成本控制**：Redis 缓存 + 乐观锁 + 幂等设计 + AI 主备自动切换 + 异步导出
- **可观测**：Prometheus 业务指标、Grafana 仪表盘、Knife4j 接口文档
- **开箱即用**：Docker Compose 一键拉起 app + Prometheus + Grafana + MinIO

### 核心机制

| 机制 | 说明 |
| :--- | :--- |
| 幂等记账 | 5 分钟内相同参数复用同一 `accountId`，避免重复记账（§5.3） |
| 跨月缓存双清 | 修改账目自动清空新旧月份的预算/仪表盘缓存（§5.5） |
| 乐观锁 | `version` 字段 + 2002 冲突码，防止并发覆盖（§5.5） |
| Token 版本号 | 注销/改密自增 `token_version`，旧 Token 即时失效（§3.4） |
| N+1 优化 | 预算查询 2 次 SQL 完成所有分类汇总（§6.4） |
| AI 主备切换 | 主模型连续失败自动切备用，恢复后探测切回（§15.10） |

---

## 二、技术栈

### 后端

| 类别 | 选型 |
| :--- | :--- |
| 语言/框架 | Java 17 + Spring Boot 3.5.16 |
| 安全 | Spring Security + JWT (jjwt 0.13) |
| 持久层 | MyBatis-Plus 3.5.17 + MySQL 8.0 |
| 缓存/锁 | Redisson 3.46 + Redis 5+ |
| AI | LangChain4j 1.0.0-beta2（主备模型 + Embedding + RAG） |
| 对象存储 | MinIO 8.5.11 |
| Excel | EasyExcel 4.0.3 |
| 文档 | Knife4j 4.5.0 + SpringDoc OpenAPI 2.8.4 |
| 监控 | Spring Boot Actuator + Micrometer + Prometheus |
| 工具 | Hutool 5.8.34 |

### 前端

| 类别 | 选型 |
| :--- | :--- |
| 框架 | Vue 3.5 + TypeScript 6 + Vite 8 |
| UI | Element Plus 2.14 |
| 图表 | ECharts 6.1 |
| 状态 | Pinia 4 + 持久化插件 |
| 路由 | Vue Router 4.6（History 模式） |
| Markdown | markdown-it + highlight.js |
| HTTP | Axios 1.19 |

### 基础设施

- **Docker / Docker Compose**：应用 + Prometheus + Grafana + MinIO 一键编排
- **Nginx**：前端静态资源 + History 路由兜底（`try_files`）

---

## 三、项目结构

```
personal-ledger/
├── src/main/java/com/ledger/
│   ├── LedgerApplication.java              # 启动类
│   ├── common/                             # 公共组件（Result/异常/UserContext/缓存）
│   ├── config/                             # MinIO 等配置
│   ├── controller/                         # 通用控制器（calendar/template/scheduled/image）
│   ├── modules/                            # 业务模块（按领域分包）
│   │   ├── account/                        # 账目模块
│   │   ├── user/                           # 用户/JWT/认证
│   │   ├── budget/                         # 预算模块
│   │   ├── statistics/                     # 仪表盘/同比/环比/趋势
│   │   ├── export/                         # Excel 异步导出
│   │   ├── tag/                            # 标签模块
│   │   ├── imports/                        # 账单导入（支付宝/微信）
│   │   └── ai/                             # AI 助手/OCR/RAG/调度
│   ├── scheduler/                          # 定时任务调度
│   └── security/                           # JWT 过滤器、SecurityConfig
├── src/main/resources/
│   ├── application.yml                     # 主配置（profile=dev）
│   ├── application-{dev,prod}.yml          # 环境差异配置
│   ├── redisson-{dev,prod}.yaml            # Redisson 客户端配置
│   └── db/schema.sql                       # 13 张表建表脚本
├── ledger-web/                             # Vue 3 前端
│   ├── src/api/                            # 13 个 API 模块
│   ├── src/components/                     # 16 个组件（含 AI 7 个）
│   ├── src/views/                          # 9 个页面（dashboard/account/budget/calendar/bill/scheduled/login/404）
│   ├── src/stores/                         # Pinia: app/user/ai
│   └── src/utils/                          # request/sse/markdown/format/date
├── monitoring/                             # Prometheus + Grafana 配置
├── docs/                                   # 设计文档（需求/概要/详细/前端方案）
├── .trae/documents/                        # 实施计划/接口测试文档
├── Dockerfile                              # 多阶段构建
├── docker-compose.yml                      # 一键编排
├── deploy.sh                               # 远程 VM 部署脚本
└── pom.xml
```

---

## 四、功能模块

### V2.0 基线（已稳定）

| 模块 | 关键功能 |
| :--- | :--- |
| 用户 | 注册/登录/登出/Refresh/修改密码/注销（Token 版本号失效） |
| 账目 | 增删改查 + 幂等 + 乐观锁 + 跨月双清 + 分页/筛选/关键词 |
| 预算 | 按月按分类设定 + N+1 优化查询 + 超支预警 |
| 统计 | 仪表盘（收支/分类/趋势/预算进度）+ Redis 缓存 |
| 导出 | EasyExcel 同步/异步导出 + 任务状态 + 文件清理 |

### V2.1 新增（已实现）

| 模块 | 关键功能 |
| :--- | :--- |
| 标签 | 多维标签（支出/收入）+ 账目关联 + 月度统计 |
| 交易模板 | 常用记账模板 + 一键应用 + 标签带入 |
| 账单导入 | 支付宝/微信 CSV 上传预览 + 分类映射 + 批量入库 |
| 定时交易 | Cron 表达式驱动 + 启用/停用 + 立即执行 |
| 图片附件 | 小票/发票上传 + 账目关联 + MinIO 存储 |
| 日历热力图 | 月度每日消费强度色阶 + 点击跳转明细 |
| 趋势对比 | 同比（vs 去年同月）+ 环比（vs 上月）+ 多月趋势 |
| AI 助手 | 流式对话 SSE + 工具调用 + 会话管理 + 配额 |
| AI OCR | 小票上传 → 结构化识别 → 一键记账 |
| AI 报告 | 财务周报 + 支出预测 + 预算推荐 + 省钱建议 |
| RAG | 知识库文档 + Embedding 索引 + 语义检索 |
| AI 高可用 | 主备模型自动切换 + 健康探测 + 本地降级 |

---

## 五、快速开始

### 5.1 环境要求

| 组件 | 版本 | 备注 |
| :--- | :--- | :--- |
| JDK | 17+ | 推荐 Eclipse Temurin 17 |
| Maven | 3.9+ | 或使用 IDE 内置 |
| Node.js | 20+ | 前端构建用 |
| MySQL | 8.0+ | 字符集 `utf8mb4` |
| Redis | 5.0+ | 缓存 + 分布式锁 |
| MinIO | 最新 | 对象存储（图片附件） |
| Docker | 24+ | 可选，生产部署 |

### 5.2 本地开发

#### 后端

```bash
# 1. 准备数据库
mysql -uroot -p -e "CREATE DATABASE ledger DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p ledger < src/main/resources/db/schema.sql

# 2. 修改 src/main/resources/application-dev.yml 中的数据库/Redis 连接信息

# 3. 启动 Redis（默认 localhost:6379）
# 4. 启动 MinIO（可选，V2.1 图片功能依赖）
docker run -d --name ledger-minio -p 9000:9000 -p 9001:9001 \
  minio/minio server /data --console-address ":9001"

# 5. 运行后端
mvn spring-boot:run
# 或打包：mvn clean package -DskipTests && java -jar target/personal-ledger.jar
```

#### 前端

```bash
cd ledger-web
npm install
npm run dev        # 开发模式：http://localhost:5173
# 生产构建：
npm run build      # 输出到 ledger-web/dist
```

#### 验证

- 后端接口文档：http://localhost:8080/doc.html
- 前端页面：http://localhost:5173
- 健康检查：http://localhost:8080/actuator/health

### 5.3 Docker 一键部署

```bash
# 1. 配置环境变量（必须修改的密钥）
export JWT_SECRET="your-strong-secret-at-least-32-chars"
export MYSQL_PASSWORD="your-mysql-password"
export REDIS_PASSWORD="your-redis-password"
export AI_PRIMARY_API_KEY="sk-xxx"        # 主模型 API Key
export AI_BACKUP_API_KEY="sk-xxx"         # 备用模型 API Key
export AI_EMBEDDING_API_KEY="sk-xxx"      # Embedding 模型 API Key
export AI_MODELSCOPE_API_KEY="ms-xxx"     # ModelScope（Embedding 备选）

# 2. 拉起全部容器（app + prometheus + grafana + minio）
docker-compose up -d --build

# 3. 查看启动日志
docker-compose logs -f app
```

部署完成后访问：

| 服务 | 地址 | 默认账号 |
| :--- | :--- | :--- |
| 应用 | http://localhost:8080 | — |
| 接口文档 | http://localhost:8080/doc.html | — |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| MinIO Console | http://localhost:9001 | minioadmin / minioadmin |

### 5.4 远程 VM 部署

```bash
# 修改 deploy.sh 中的 VM_HOST/VM_USER 后执行
VM_USER=root ./deploy.sh
```

---

## 六、配置说明

### 6.1 必须外部化的密钥

| 环境变量 | 默认值（仅 dev） | 说明 |
| :--- | :--- | :--- |
| `JWT_SECRET` | `ledger-dev-...` | JWT 签名密钥，生产必须改 ≥32 字符随机串 |
| `MYSQL_PASSWORD` | 空 | MySQL 密码 |
| `REDIS_PASSWORD` | 空 | Redis 密码（dev 可空） |
| `AI_PRIMARY_API_KEY` | `sk-V9U7...` | 主模型 API Key |
| `AI_BACKUP_API_KEY` | `sk-eeac...` | 备用模型 API Key |
| `AI_EMBEDDING_API_KEY` | `sk-XSGg...` | Embedding 模型 API Key |
| `AI_MODELSCOPE_API_KEY` | `ms-66b0...` | ModelScope（Embedding 备选） |

> **重要**：所有密钥均通过 `${ENV_VAR:default}` 形式注入，**严禁在生产配置中硬编码**。详见 `application.yml`。

### 6.2 Profile 切换

- **dev**（默认）：本地调试，详细日志（DEBUG），无密码 Redis
- **prod**：生产环境，INFO 日志，外部化密钥

切换方式：

```bash
java -jar app.jar --spring.profiles.active=prod
# 或
SPRING_PROFILES_ACTIVE=prod java -jar app.jar
```

### 6.3 前端环境变量

| 文件 | 用途 |
| :--- | :--- |
| `ledger-web/.env.development` | 开发：`VITE_API_BASE_URL=http://localhost:8080` |
| `ledger-web/.env.production` | 生产：`VITE_API_BASE_URL=/api` 或反向代理地址 |

---

## 七、接口文档与测试

### 7.1 在线接口文档

- **Knife4j**：http://localhost:8080/doc.html（按模块分组）
- **Swagger UI**：http://localhost:8080/swagger-ui.html

### 7.2 测试文档

完整的接口测试用例（V1.5 基线 56 条 + V2.1 新增模块）见：

- [接口测试文档.md](.trae/documents/接口测试文档.md)

文档包含：
- §1 测试环境与错误码速查
- §2 各模块测试用例（含期望响应）
- §3 核心设计验证清单（幂等/双清/乐观锁/N+1）
- §4 可观测性验证
- §5 测试数据准备 SQL
- §6 推荐测试顺序
- §7 常见问题排查
- §9 附录：**V2.1 接口 curl 速查**（含一键回归脚本）

### 7.3 快速 curl 验证

```bash
# 登录获取 Token
export TOKEN=$(curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser2026","password":"Test@2026"}' \
  | jq -r '.data.accessToken')

# 仪表盘
curl -s "http://localhost:8080/api/statistics/dashboard?month=2026-08" \
  -H "Authorization: Bearer $TOKEN" | jq '.code'

# AI 健康
curl -s http://localhost:8080/api/v1/ai/health \
  -H "Authorization: Bearer $TOKEN" | jq '.code'
```

> 更多命令见接口测试文档 §9「V2.1 接口 curl 速查」。

---

## 八、监控与可观测性

### 8.1 Prometheus 业务指标

访问 http://localhost:8080/actuator/prometheus 获取指标文本，关键指标：

| 指标 | 含义 |
| :--- | :--- |
| `ledger_account_success_count_total` | 记账成功次数 |
| `ledger_account_fail_count_total` | 记账失败次数 |
| `ledger_export_success_count_total` | 导出成功次数 |
| `ledger_export_fail_count_total` | 导出失败次数 |
| `ledger_cache_hit_count_total` | 缓存命中 |
| `ledger_cache_miss_count_total` | 缓存未命中 |
| `ledger_export_queue_size` | 异步导出队列长度 |

### 8.2 Grafana 仪表盘

- 地址：http://localhost:3000（admin/admin）
- 自动加载：`monitoring/grafana/provisioning/` 预置数据源与仪表盘
- 模板仪表盘包含：HTTP QPS、响应耗时、缓存命中率、导出队列、AI 调用统计

### 8.3 健康检查

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{...}}
```

Dockerfile 已内置 `HEALTHCHECK`，每 30s 探测一次。

---

## 九、路线图

| 版本 | 状态 | 重点 |
| :--- | :--- | :--- |
| V1.5 | ✅ 已发布 | 用户/账目/预算/统计/导出 + 5 项核心机制 |
| V2.0 | ✅ 已发布 | 模块化重构 + 异步导出 + 监控 |
| V2.1 | ✅ 已发布 | AI 助手 + 标签 + 模板 + 账单导入 + 定时交易 + 图片 + RAG |
| V2.2 | 🚧 规划中 | PWA 离线、多账本、家庭共享、移动端适配 |

---

## 十、常见问题

| 现象 | 排查 |
| :--- | :--- |
| 启动报 `Communications link failure` | MySQL 未启动或 `application-dev.yml` 数据源配置错误 |
| 启动报 `Unable to connect to Redis` | Redis 未启动；`redisson-prod.yaml` 的 `password` 字段为字符串 `"null"` 时需移除 |
| 接口返回 401 | Token 无效或已过期，重新登录 |
| 接口返回 1004 | Token 版本号不匹配（用户被注销/改密），重新登录 |
| 接口返回 2002 | 乐观锁冲突，刷新页面获取最新 `version` 后重试 |
| 跨月双清未触发 | 检查 `@TransactionalEventListener(AFTER_COMMIT)` 与事件发布 |
| 预算查询 SQL 过多 | 确认使用 `sumSpentGroupByCategory`，而非循环查询 |
| AI 对话返回 6007 | 主备模型均不可用，检查 API Key 配额与网络 |
| AI 对话返回 6002 | 当日配额已用尽（50 次 / 10 万 Token） |
| 前端构建 OOM | `NODE_OPTIONS=--max-old-space-size=4096 npm run build` |
| 前端路由 404 | Nginx 未配置 `try_files $uri $uri/ /index.html` |

---

## 十一、开源协议

本项目采用 **MIT License**，详见 [LICENSE](LICENSE) 文件。

> 商业使用、修改、分发、再发布均允许，但请保留原作者署名。**不承担任何明示或暗示的担保责任**。

---

## 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [MyBatis-Plus](https://baomidou.com/)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [ECharts](https://echarts.apache.org/)
- [LangChain4j](https://docs.langchain4j.dev/)
- [MinIO](https://min.io/)
- [Knife4j](https://doc.xiaominfo.com/)
