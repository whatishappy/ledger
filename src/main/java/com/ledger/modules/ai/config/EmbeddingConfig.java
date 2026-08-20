package com.ledger.modules.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${langchain4j.embedding.base-url:}")
    private String baseUrl;

    @Value("${langchain4j.embedding.api-key:}")
    private String apiKey;

    @Value("${langchain4j.embedding.model-name:}")
    private String modelName;

    @Value("${langchain4j.embedding.timeout:30s}")
    private Duration timeout;

    @Bean
    public EmbeddingModel embeddingModel() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LangChain4j Embedding 模型 API Key 未配置，RAG 知识库功能将不可用");
            return null;
        }
        log.info("初始化 Embedding 模型: model={}, baseUrl={}", modelName, baseUrl);
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .build();
    }
}
