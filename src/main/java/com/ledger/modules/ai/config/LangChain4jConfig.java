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

    @Value("${langchain4j.open-ai.chat-model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:qwen-plus}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.timeout:30s}")
    private Duration timeout;

    @Value("${langchain4j.open-ai.backup.model-name:qwen-turbo}")
    private String backupModelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LangChain4j API Key未配置，AI模块将使用降级模式");
            return null;
        }
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .temperature(0.3)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LangChain4j API Key未配置，流式AI模块将使用降级模式");
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(timeout)
                .temperature(0.3)
                .build();
    }

    @Bean
    public ChatLanguageModel backupChatLanguageModel() {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(backupModelName)
                .timeout(timeout)
                .temperature(0.3)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel backupStreamingChatLanguageModel() {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(backupModelName)
                .timeout(timeout)
                .temperature(0.3)
                .build();
    }
}
