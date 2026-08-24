package com.ledger.modules.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.primary.base-url:}")
    private String primaryBaseUrl;

    @Value("${langchain4j.primary.api-key:}")
    private String primaryApiKey;

    @Value("${langchain4j.primary.model-name:}")
    private String primaryModelName;

    @Value("${langchain4j.primary.timeout:60s}")
    private Duration primaryTimeout;

    @Value("${langchain4j.primary.temperature:0.3}")
    private Double primaryTemperature;

    @Value("${langchain4j.backup.base-url:}")
    private String backupBaseUrl;

    @Value("${langchain4j.backup.api-key:}")
    private String backupApiKey;

    @Value("${langchain4j.backup.model-name:}")
    private String backupModelName;

    @Value("${langchain4j.backup.timeout:60s}")
    private Duration backupTimeout;

    @Value("${langchain4j.backup.temperature:0.3}")
    private Double backupTemperature;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (primaryApiKey == null || primaryApiKey.isBlank()) {
            log.warn("LangChain4j 主模型 API Key 未配置，AI 模块将使用降级模式");
            return null;
        }
        log.info("初始化主对话模型: model={}, baseUrl={}", primaryModelName, primaryBaseUrl);
        return OpenAiChatModel.builder()
                .baseUrl(primaryBaseUrl)
                .apiKey(primaryApiKey)
                .modelName(primaryModelName)
                .timeout(primaryTimeout)
                .temperature(primaryTemperature)
                // 禁用内部重试：避免单个慢请求 ×3 重试拖死整个 SSE 流（最坏 3×timeout 无响应）
                .maxRetries(0)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        if (primaryApiKey == null || primaryApiKey.isBlank()) {
            log.warn("LangChain4j 主模型 API Key 未配置，流式 AI 模块将使用降级模式");
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(primaryBaseUrl)
                .apiKey(primaryApiKey)
                .modelName(primaryModelName)
                .timeout(primaryTimeout)
                .temperature(primaryTemperature)
                .build();
    }

    @Bean
    public ChatLanguageModel backupChatLanguageModel() {
        if (backupApiKey == null || backupApiKey.isBlank()) {
            log.warn("LangChain4j 备用模型 API Key 未配置");
            return null;
        }
        log.info("初始化备用对话模型: model={}, baseUrl={}", backupModelName, backupBaseUrl);
        return OpenAiChatModel.builder()
                .baseUrl(backupBaseUrl)
                .apiKey(backupApiKey)
                .modelName(backupModelName)
                .timeout(backupTimeout)
                .temperature(backupTemperature)
                .maxRetries(0)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel backupStreamingChatLanguageModel() {
        if (backupApiKey == null || backupApiKey.isBlank()) {
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(backupBaseUrl)
                .apiKey(backupApiKey)
                .modelName(backupModelName)
                .timeout(backupTimeout)
                .temperature(backupTemperature)
                .build();
    }
}
