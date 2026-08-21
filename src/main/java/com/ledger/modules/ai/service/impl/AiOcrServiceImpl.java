package com.ledger.modules.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.modules.ai.dto.AiOcrResultDTO;
import com.ledger.modules.ai.service.AiOcrService;
import com.ledger.service.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiOcrServiceImpl implements AiOcrService {

    private final MinioStorageService minioStorageService;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${langchain4j.primary.base-url:}")
    private String primaryBaseUrl;

    @Value("${langchain4j.primary.api-key:}")
    private String primaryApiKey;

    @Value("${langchain4j.primary.model-name:}")
    private String primaryModelName;

    @Override
    public AiOcrResultDTO uploadAndOcr(Long userId, MultipartFile file) {
        String imageUrl = uploadToMinio(userId, file);
        log.info("小票图片上传成功: userId={}, imageUrl={}", userId, imageUrl);
        return ocrByImageUrl(userId, imageUrl);
    }

    @Override
    public AiOcrResultDTO ocrByImageUrl(Long userId, String imageUrl) {
        log.info("开始OCR识别: userId={}, imageUrl={}", userId, imageUrl);
        if (primaryApiKey == null || primaryApiKey.isBlank()) {
            log.warn("OCR API Key未配置，返回空结果");
            AiOcrResultDTO empty = new AiOcrResultDTO();
            empty.setImageUrl(imageUrl);
            empty.setMerchant("识别失败");
            return empty;
        }

        try {
            String rawResponse = callAgnesOcrApi(imageUrl);
            log.info("Agnes OCR响应: userId={}, response={}", userId, rawResponse);
            AiOcrResultDTO result = parseOcrResponse(rawResponse);
            result.setImageUrl(imageUrl);
            return result;
        } catch (Exception e) {
            log.error("OCR识别失败: userId={}", userId, e);
            AiOcrResultDTO error = new AiOcrResultDTO();
            error.setImageUrl(imageUrl);
            error.setMerchant("识别失败: " + e.getMessage());
            return error;
        }
    }

    private String uploadToMinio(Long userId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (ext.isEmpty()) ext = ".jpg";

        String objectKey = "receipts/" + userId + "/" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ext;

        try {
            minioStorageService.uploadObject(objectKey, file.getInputStream(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("上传小票图片到MinIO失败", e);
        }
        return minioStorageService.getPublicUrl(objectKey);
    }

    private String callAgnesOcrApi(String imageUrl) {
        String endpoint = primaryBaseUrl + (primaryBaseUrl.endsWith("/") ? "" : "/") + "chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", primaryModelName);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image_url");
        Map<String, String> imageUrlContent = new HashMap<>();
        imageUrlContent.put("url", imageUrl);
        imageContent.put("image_url", imageUrlContent);
        content.add(imageContent);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "这是一张购物小票或账单。请仔细识别图片中的所有文字信息，并以严格的JSON格式返回结果。" +
                "返回JSON结构如下：\n" +
                "{\n" +
                "  \"merchant\": \"商户名称\",\n" +
                "  \"date\": \"交易日期，格式为YYYY-MM-DD\",\n" +
                "  \"total\": 总金额数字,\n" +
                "  \"paymentMethod\": \"支付方式\",\n" +
                "  \"category\": \"建议分类（如餐饮、购物、交通、娱乐、医疗、教育、住宿等）\",\n" +
                "  \"items\": [\n" +
                "    {\"name\": \"商品名\", \"price\": 单价, \"quantity\": 数量}\n" +
                "  ]\n" +
                "}\n" +
                "请只返回JSON，不要返回其他任何文字或解释。如果某项无法识别，该字段设为null。");
        content.add(textContent);

        userMsg.put("content", content);
        messages.add(userMsg);
        body.put("messages", messages);
        body.put("max_tokens", 2000);
        body.put("temperature", 0.1);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + primaryApiKey);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Agnes API返回错误状态: " + response.getStatusCode());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("构造OCR请求失败", e);
        }
    }

    private AiOcrResultDTO parseOcrResponse(String rawResponse) {
        AiOcrResultDTO result = new AiOcrResultDTO();
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    String content = message.get("content").asText();
                    content = extractJson(content);
                    if (content != null) {
                        parseStructuredResult(content, result);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析OCR响应失败", e);
            result.setMerchant("解析失败");
        }
        return result;
    }

    private String extractJson(String text) {
        if (text == null) return null;
        text = text.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastBacktick = text.lastIndexOf("```");
            if (firstNewline > 0 && lastBacktick > firstNewline) {
                return text.substring(firstNewline + 1, lastBacktick).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private void parseStructuredResult(String json, AiOcrResultDTO result) {
        try {
            JsonNode node = objectMapper.readTree(json);
            result.setMerchant(getTextOrNull(node, "merchant"));
            result.setDate(getTextOrNull(node, "date"));
            result.setPaymentMethod(getTextOrNull(node, "paymentMethod"));
            result.setCategory(getTextOrNull(node, "category"));

            JsonNode totalNode = node.get("total");
            if (totalNode != null && !totalNode.isNull()) {
                result.setTotal(totalNode.decimalValue());
            }

            JsonNode itemsNode = node.get("items");
            if (itemsNode != null && itemsNode.isArray()) {
                List<AiOcrResultDTO.ReceiptItem> items = new ArrayList<>();
                for (JsonNode itemNode : itemsNode) {
                    AiOcrResultDTO.ReceiptItem item = new AiOcrResultDTO.ReceiptItem();
                    item.setName(getTextOrNull(itemNode, "name"));
                    JsonNode p = itemNode.get("price");
                    if (p != null && !p.isNull()) item.setPrice(p.decimalValue());
                    JsonNode q = itemNode.get("quantity");
                    if (q != null && !q.isNull()) item.setQuantity(q.asInt());
                    items.add(item);
                }
                result.setItems(items);
            }
        } catch (Exception e) {
            log.warn("解析OCR结构化结果失败", e);
        }
    }

    private String getTextOrNull(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) return null;
        return field.asText();
    }
}
