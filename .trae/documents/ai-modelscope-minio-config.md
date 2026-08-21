# AI 模型（魔搭 ModelScope）与 MinIO Docker 部署配置计划

> **状态**: 草案 — 暂停，待次日继续完善
>
> **暂停原因**: 用户指出仅一个模型不够，RAG 知识库需要 embedding 模型，计划范围需扩大。次日继续。

## 概述

将 AI 模块切换为魔搭社区（ModelScope）模型，并通过 Docker 部署 MinIO 对象存储服务以支持用户头像上传功能。计划分为两期：**第一期**配置对话模型和 MinIO，**第二期**搭建 RAG 知识库（embedding 模型 + 向量数据库）。

## 待解决问题（次日讨论）

### 问题 1：模型不够，需要多个模型

当前用户只提供了 `Qwen/Qwen2.5-VL-3B-Instruct` 一个模型。实际上完整 AI 功能至少需要 **3 个模型**：

| 用途 | 推荐模型（ModelScope） | 说明 |
|------|------------------------|------|
| 对话模型（主） | `Qwen/Qwen2.5-32B-Instruct` 或 `Qwen/Qwen2.5-7B-Instruct` | 工具调用、自然语言记账，参数越大准确度越高 |
| 对话模型（备） | `Qwen/Qwen2.5-7B-Instruct` | 主模型失败时降级使用 |
| Embedding 模型 | `gte-large-zh` 或 `bge-large-zh` | RAG 知识库文本向量化，非对话模型 |

**关键说明**: `Qwen/Qwen2.5-VL-3B-Instruct` 是视觉语言模型（VL），不是 embedding 模型，不能用于文本向量化。对话和 embedding 是两类完全不同的模型，无法共用。

### 问题 2：RAG 知识库需要 embedding 模型

- 当前代码已有 `AiKnowledgeDocument` 实体（含 `embeddingId` 和 `status` 字段）和 `ai_knowledge_document` 表
- 但尚未实现 embedding 逻辑（将文档内容转为向量）
- LangChain4j 提供 `EmbeddingModel` 接口和 `OpenAiEmbeddingModel`，可通过 ModelScope 的 `/v1/embeddings` 端点接入
- 需要确认 ModelScope API-Inference 是否支持 embedding 模型，以及具体支持的模型 ID

### 问题 3：向量数据库（Milvus）尚未部署

- 落地页（landing page）中提到了 Milvus 向量数据库
- 当前 `docker-compose.yml` 中未包含 Milvus 服务
- RAG 检索需要向量数据库存储和检索 embedding 向量
- 需要在 `docker-compose.yml` 中新增 Milvus 服务（或使用其他向量数据库方案）

### 问题 4：ModelScope 模型可用性待确认

- `Qwen/Qwen2.5-VL-3B-Instruct` 可能不在 ModelScope API-Inference 已支持的模型列表中
- 当前已确认支持的对话模型包括：`Qwen/Qwen2.5-7B-Instruct`、`Qwen/Qwen2.5-14B-Instruct`、`Qwen/Qwen2.5-32B-Instruct`、`Qwen/Qwen2.5-72B-Instruct` 等
- 次日需确认：ModelScope 是否支持 embedding 模型的 API 调用，以及具体模型 ID

## 当前状态分析

### AI 模块
- **LangChain4jConfig.java** 使用 `@Value` 注解读取配置，默认指向阿里云 DashScope（`https://dashscope.aliyuncs.com/compatible-mode/v1`）
- `application.yml` 中**未定义** `langchain4j` 配置段，`api-key` 默认为空字符串，导致 AI 模块当前处于降级模式（Bean 返回 null）
- 代码定义了主模型（`qwen-plus`）和备用模型（`qwen-turbo`）两个 Bean
- `AiChatServiceImpl` 通过反射调用模型的 `generate`/`chat` 方法，兼容不同版本

### MinIO 模块
- **MinioProperties.java** 硬编码默认值：`localhost:9000`、`minioadmin/minioadmin`、bucket `ledger-avatars`
- **MinioConfig.java** 创建 `MinioClient` Bean 并懒初始化 Bucket（`@Lazy`）
- `application.yml` 中**未定义** `minio` 配置段
- `docker-compose.yml` 中**未包含** MinIO 服务

### ModelScope API 确认
- **Base URL**: `https://api-inference.modelscope.cn/v1`
- **API Key 格式**: `ms-xxxxx`（用户提供的 `ms-66b0fae5-ddab-4579-a982-4c7798e121f7` 符合此格式）
- **模型 ID 格式**: `组织/模型名`，如 `Qwen/Qwen2.5-VL-3B-Instruct`
- **免费额度**: 每日 2000 次调用
- **兼容性**: OpenAI 兼容 `/v1/chat/completions` 端点，支持流式输出和 Function Calling

## 提议变更

### 变更 1：application.yml — 新增 AI 模型配置

**文件**: `src/main/resources/application.yml`

在文件末尾（`logging` 配置之后）新增 `langchain4j` 配置段：

```yaml
# LangChain4j AI 模型配置（魔搭社区 ModelScope）
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api-inference.modelscope.cn/v1
      api-key: ms-66b0fae5-ddab-4579-a982-4c7798e121f7
      model-name: Qwen/Qwen2.5-VL-3B-Instruct
      timeout: 60s
    backup:
      model-name: Qwen/Qwen2.5-VL-3B-Instruct
```

**说明**:
- `base-url` 指向魔搭社区 OpenAI 兼容端点
- `api-key` 使用用户提供的魔搭 SDK Token
- `model-name` 使用魔搭模型 ID 格式（`组织/模型名`）
- `timeout` 从默认 30s 提升到 60s，适配免费推理 API 的响应延迟
- `backup.model-name` 与主模型相同（用户仅提供一个模型）
- 这些属性路径与 `LangChain4jConfig.java` 中 `@Value` 注解的路径完全匹配，无需修改 Java 代码

**注意事项**:
- ModelScope 公测期间支持的模型列表中，`Qwen/Qwen2.5-VL-3B-Instruct`（VL 视觉语言模型）可能不在 API-Inference 服务列表中。当前 API-Inference 已确认支持的模型包括 `Qwen/Qwen2.5-7B-Instruct`、`Qwen/Qwen2.5-32B-Instruct` 等。如果该模型不可用，启动时 AI 调用会返回错误，届时可替换为 `Qwen/Qwen2.5-7B-Instruct` 或其他已支持模型。
- `Qwen2.5-VL-3B-Instruct` 是视觉语言模型，当前代码使用 `OpenAiChatModel`（纯文本），仅利用其文本对话能力。
- 3B 参数量较小，对于涉及 Function Calling 的 AI 工具调用场景，输出质量和准确性可能不如更大参数的模型。

### 变更 2：docker-compose.yml — 新增 MinIO 服务

**文件**: `docker-compose.yml`

在 `grafana` 服务之后、`volumes` 之前新增 MinIO 服务：

```yaml
  # MinIO 对象存储
  minio:
    image: minio/minio:latest
    container_name: ledger-minio
    restart: unless-stopped
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      - MINIO_ROOT_USER=minioadmin
      - MINIO_ROOT_PASSWORD=minioadmin
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data
    networks:
      - ledger-net
```

在 `volumes` 段新增：

```yaml
  minio-data:
```

**说明**:
- 9000 端口为 MinIO S3 API 端口，9001 为 Web Console
- 使用默认凭证 `minioadmin/minioadmin`（开发环境）
- 数据持久化到 `minio-data` Docker volume
- 加入 `ledger-net` 网络，与 app 容器互通

### 变更 3：application.yml — 新增 MinIO 配置段

**文件**: `src/main/resources/application.yml`

在 `langchain4j` 配置段之后新增 `minio` 配置段：

```yaml
# MinIO 对象存储配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: ledger-avatars
  expiry-seconds: 3600
```

**说明**:
- 开发环境下 endpoint 为 `http://localhost:9000`（IDE 本地运行 + Docker MinIO）
- 这些属性路径与 `MinioProperties.java` 的 `@ConfigurationProperties(prefix = "minio")` 匹配
- 无需修改任何 Java 代码

## 前置条件与部署步骤

### 需要用户操作

1. **启动虚拟机** — 确认 Docker 服务可用
2. **启动 MinIO 容器** — 在项目根目录执行：
   ```bash
   docker-compose up -d minio
   ```
3. **验证 MinIO** — 浏览器访问 `http://localhost:9001`，使用 `minioadmin/minioadmin` 登录确认服务正常
4. **启动应用** — IDE 中运行 Spring Boot 主类，观察日志确认：
   - LangChain4j 模型 Bean 创建成功（无 "API Key未配置" 警告）
   - MinIO Bucket 初始化成功（首次调用头像接口时触发 `@Lazy` 初始化）

## 假设与决策

1. **AI API Key 安全**: 当前直接写入 `application.yml` 适用于开发环境。生产环境应通过环境变量 `LANGCHAIN4J_OPEN_AI_CHAT_MODEL_API_KEY` 注入。
2. **MinIO 凭证安全**: 默认 `minioadmin/minioadmin` 仅用于开发。生产环境应通过环境变量 `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` 设置强密码。
3. **模型可用性**: 假设 `Qwen/Qwen2.5-VL-3B-Instruct` 在魔搭 API-Inference 中可用。如果不可用，备选模型为 `Qwen/Qwen2.5-7B-Instruct`。
4. **不需要修改 Java 代码**: 所有配置通过 `application.yml` 属性注入，`@Value` 和 `@ConfigurationProperties` 路径已匹配。

## 验证步骤

1. 启动 MinIO 容器后，执行 `curl http://localhost:9000/minio/health/live` 确认返回 200
2. 启动应用后，查看日志无 "LangChain4j API Key未配置" 警告
3. 调用 `GET /api/v1/ai/quota` 确认 AI 配额接口正常返回
4. 调用 `POST /api/v1/ai/chat:stream` 发送一条测试消息，确认 AI 回复正常
5. 调用 `GET /api/user/me/avatar-upload-url?fileType=jpg` 确认返回 MinIO 预签名上传 URL

## 第二期：RAG 知识库（次日规划）

> 以下为次日需要规划的内容框架，待确认 ModelScope embedding 支持情况后细化。

### 第二期变更预览（待细化）

1. **application.yml** — 新增 embedding 模型配置段
   - 需确定 ModelScope 支持的 embedding 模型 ID
   - 配置 `langchain4j.open-ai.embedding-model` 属性

2. **LangChain4jConfig.java** — 新增 `EmbeddingModel` Bean
   - 基于 `OpenAiEmbeddingModel` 创建 embedding Bean
   - 注入到知识库相关服务

3. **docker-compose.yml** — 新增 Milvus 向量数据库服务
   - Milvus 需要依赖 etcd 和 minio（可复用已部署的 MinIO）
   - 端口 19530（gRPC）、9091（健康检查）

4. **RAG 检索服务** — 新增代码实现
   - 文档上传 → embedding → 存入 Milvus
   - 查询时 → 问题 embedding → Milvus 语义检索 → 拼接上下文 → 对话模型回答

5. **application.yml** — 新增 Milvus 连接配置
