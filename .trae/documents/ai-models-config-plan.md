# AI 模型与 MinIO 配置实施计划

## 目标

为 Ledger 记账系统配置三个 AI 模型（主对话、备用对话、Embedding）和 MinIO 对象存储服务。

## 模型配置信息

| 用途 | 模型名称 | 提供商 | Base URL | API Key |
|------|---------|--------|----------|---------|
| 主对话模型 | `Agnes-2.5-Flash` | Agnes AI | `https://apihub.agnes-ai.cn/v1` | `sk-V9U7JgvceJCnjIqJ1bxa6acnfX0iaJAAtRfxqyc3JrdkxaSK` |
| 备用对话模型 | `deepseek-v4-flash` | 腾讯云 TokenHub | `https://tokenhub.tencentmaas.com/v1` | `sk-eeac7bd693474efeb8c12feb67e15a59` |
| Embedding 模型 | `kinfra-text-embedding-0.6b`（腾讯云） | 腾讯云 TokenHub | 复用腾讯云 TokenHub API | `sk-XSGgVIMTytcEXCmsr8VrCPsTFg23daklHVorP7l1k2NcLVHc` |

**注意**:
- Agnes AI 2026年7月29日迁移至中国站 `.cn` 域名
- 腾讯云 TokenHub 支持 DeepSeek、混元、Kinfra 等多个模型
- Embedding 模型选择腾讯云的 `kinfra-text-embedding-0.6b`（用户图片中显示的腾讯云免费模型之一）

## 变更计划

### 变更 1：修改 `LangChain4jConfig.java` — 支持双提供商配置

**文件**: `src/main/java/com/ledger/modules/ai/config/LangChain4jConfig.java`

**问题**: 当前代码所有 Bean 共享同一个 `baseUrl` 和 `apiKey`，无法支持主备模型来自不同提供商。

**修改方案**:
- 主模型 Bean 使用 `langchain4j.primary.*` 前缀的配置
- 备用模型 Bean 使用 `langchain4j.backup.*` 前缀的配置
- 新增配置属性：
  - `langchain4j.primary.base-url`（主模型 base URL）
  - `langchain4j.primary.api-key`（主模型 API Key）
  - `langchain4j.primary.model-name`（主模型名称）
  - `langchain4j.backup.base-url`（备用模型 base URL）
  - `langchain4j.backup.api-key`（备用模型 API Key）
  - `langchain4j.backup.model-name`（备用模型名称）
  - `langchain4j.embedding.base-url`（Embedding 模型 base URL）
  - `langchain4j.embedding.api-key`（Embedding 模型 API Key）
  - `langchain4j.embedding.model-name`（Embedding 模型名称）

**需要新增的 Bean**:
- `EmbeddingModel` Bean（用于 RAG 知识库文本向量化）

### 变更 2：修改 `application.yml` — 新增 AI 模型配置段

**文件**: `src/main/resources/application.yml`

在 `logging` 配置之后新增：

```yaml
# LangChain4j AI 模型配置
langchain4j:
  primary:
    base-url: https://apihub.agnes-ai.cn/v1
    api-key: sk-V9U7JgvceJCnjIqJ1bxa6acnfX0iaJAAtRfxqyc3JrdkxaSK
    model-name: Agnes-2.5-Flash
    timeout: 60s
    temperature: 0.3
  backup:
    base-url: https://tokenhub.tencentmaas.com/v1
    api-key: sk-eeac7bd693474efeb8c12feb67e15a59
    model-name: deepseek-v4-flash
    timeout: 60s
    temperature: 0.3
  embedding:
    base-url: https://tokenhub.tencentmaas.com/v1
    api-key: sk-XSGgVIMTytcEXCmsr8VrCPsTFg23daklHVorP7l1k2NcLVHc
    model-name: kinfra-text-embedding-0.6b
    timeout: 30s

# MinIO 对象存储配置
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: ledger-avatars
  expiry-seconds: 3600
```

### 变更 3：修改 `docker-compose.yml` — 新增 MinIO 服务

**文件**: `docker-compose.yml`

在 `grafana` 服务之后新增：

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

### 变更 4：新增 `EmbeddingConfig.java` — Embedding 模型配置类

**文件**: `src/main/java/com/ledger/modules/ai/config/EmbeddingConfig.java`（新文件）

创建 Embedding 模型配置类，用于创建 `EmbeddingModel` Bean。

## 实施步骤

1. 修改 `LangChain4jConfig.java`，将单提供商配置改为主备双提供商配置
2. 新建 `EmbeddingConfig.java`，添加 Embedding 模型 Bean
3. 修改 `application.yml`，新增 `langchain4j` 和 `minio` 配置段
4. 修改 `docker-compose.yml`，新增 MinIO 服务和 volume
5. 验证配置正确性

## 风险与注意事项

1. **API Key 安全**: 所有密钥直接写入配置文件，仅适合开发环境。生产环境需改用环境变量。
2. **模型名称确认**: Agnes 的模型名可能是 `Agnes-2.5-Flash`、`agnes-2.0-flash` 或 `agnes-large`，需以实际 API 文档为准。若调用失败需及时调整。
3. **Embedding 模型可用性**: 腾讯云 TokenHub 是否支持 `kinfra-text-embedding-0.6b` 需要实际验证。备选方案：使用 `qwen3-embedding-0.6b` 或其他可用的 embedding 模型。
4. **Agnes API 地址**: 已确认迁移到 `.cn` 域名（2026年7月29日），如果实际调用失败可尝试回退到 `apihub.agnes-ai.com`。
5. **MinIO 启动**: 需要在虚拟机中启动 Docker 并运行 MinIO 容器，应用才能正常使用头像功能。

## 后续扩展（不在本次范围）

- RAG 知识库集成（向量数据库 Milvus）
- Embedding 服务实现
- 文档上传与向量化流程
- AI 配额统计与管理
