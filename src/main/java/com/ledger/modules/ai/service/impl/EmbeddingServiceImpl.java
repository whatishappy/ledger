package com.ledger.modules.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.modules.ai.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Value("${ai.embedding.api-url:https://api-inference.modelscope.cn/v1/embeddings}")
    private String apiUrl;

    @Value("${ai.embedding.api-key:}")
    private String apiKey;

    @Value("${ai.embedding.model:Qwen3-Embedding-0.6B-GGUF}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Float> embed(String text) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", text
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.get("data");
                if (data != null && data.isArray() && !data.isEmpty()) {
                    JsonNode embedding = data.get(0).get("embedding");
                    List<Float> result = new ArrayList<>();
                    for (JsonNode v : embedding) {
                        result.add((float) v.asDouble());
                    }
                    return result;
                }
            }
            log.warn("Embedding API 返回空结果");
            return List.of();
        } catch (Exception e) {
            log.error("Embedding 生成失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<float[]> batchEmbed(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            List<Float> embedding = embed(text);
            if (!embedding.isEmpty()) {
                float[] arr = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    arr[i] = embedding.get(i);
                }
                results.add(arr);
            } else {
                results.add(new float[0]);
            }
        }
        return results;
    }
}
