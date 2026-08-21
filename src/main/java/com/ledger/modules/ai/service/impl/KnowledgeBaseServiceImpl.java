package com.ledger.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.modules.ai.entity.AiKnowledgeDocument;
import com.ledger.modules.ai.mapper.AiKnowledgeDocumentMapper;
import com.ledger.modules.ai.service.EmbeddingService;
import com.ledger.modules.ai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final AiKnowledgeDocumentMapper documentMapper;
    private final EmbeddingService embeddingService;

    @Override
    @Transactional
    public AiKnowledgeDocument create(AiKnowledgeDocument doc) {
        doc.setStatus(0);
        doc.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(doc);
        log.info("创建知识库文档: id={}, title={}", doc.getId(), doc.getTitle());
        return doc;
    }

    @Override
    @Transactional
    public AiKnowledgeDocument update(Long id, AiKnowledgeDocument doc) {
        AiKnowledgeDocument existing = documentMapper.selectById(id);
        if (existing == null) throw new RuntimeException("文档不存在: " + id);
        if (doc.getTitle() != null) existing.setTitle(doc.getTitle());
        if (doc.getContent() != null) {
            existing.setContent(doc.getContent());
            existing.setStatus(0);
        }
        if (doc.getDocType() != null) existing.setDocType(doc.getDocType());
        documentMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        documentMapper.deleteById(id);
    }

    @Override
    public AiKnowledgeDocument getById(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public List<AiKnowledgeDocument> listAll() {
        return documentMapper.selectList(null);
    }

    @Override
    public List<AiKnowledgeDocument> listByType(String docType) {
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getDocType, docType);
        return documentMapper.selectList(wrapper);
    }

    @Override
    public List<AiKnowledgeDocument> searchByKeyword(String keyword, int limit) {
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<AiKnowledgeDocument>()
                .like(AiKnowledgeDocument::getTitle, keyword)
                .or()
                .like(AiKnowledgeDocument::getContent, keyword)
                .last("LIMIT " + limit);
        return documentMapper.selectList(wrapper);
    }

    @Override
    public List<AiKnowledgeDocument> ragSearch(String query, int topK) {
        List<AiKnowledgeDocument> allDocs = documentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getStatus, 1)
        );
        if (allDocs.isEmpty()) {
            return searchByKeyword(query, topK);
        }

        List<Float> queryEmbedding = embeddingService.embed(query);
        if (queryEmbedding.isEmpty()) {
            return searchByKeyword(query, topK);
        }

        Map<Long, List<Float>> docEmbeddings = new HashMap<>();
        for (AiKnowledgeDocument doc : allDocs) {
            List<Float> emb = embeddingService.embed(doc.getTitle() + " " + doc.getContent());
            if (!emb.isEmpty()) {
                docEmbeddings.put(doc.getId(), emb);
            }
        }

        List<Map.Entry<Long, Double>> scoredDocs = new ArrayList<>();
        for (Map.Entry<Long, List<Float>> entry : docEmbeddings.entrySet()) {
            double similarity = cosineSimilarity(queryEmbedding, entry.getValue());
            scoredDocs.add(new AbstractMap.SimpleEntry<>(entry.getKey(), similarity));
        }

        scoredDocs.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<AiKnowledgeDocument> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scoredDocs.size()); i++) {
            Long docId = scoredDocs.get(i).getKey();
            AiKnowledgeDocument doc = documentMapper.selectById(docId);
            if (doc != null) {
                results.add(doc);
            }
        }

        if (results.isEmpty()) {
            return searchByKeyword(query, topK);
        }
        return results;
    }

    @Override
    @Transactional
    public void indexDocument(Long id) {
        AiKnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) return;
        List<Float> embedding = embeddingService.embed(doc.getTitle() + " " + doc.getContent());
        if (!embedding.isEmpty()) {
            doc.setStatus(1);
            doc.setEmbeddingId("emb_" + id);
            documentMapper.updateById(doc);
            log.info("文档索引完成: id={}", id);
        } else {
            log.warn("文档索引失败，embedding为空: id={}", id);
        }
    }

    @Override
    @Transactional
    public void indexAllPending() {
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getStatus, 0);
        List<AiKnowledgeDocument> pendingDocs = documentMapper.selectList(wrapper);
        for (AiKnowledgeDocument doc : pendingDocs) {
            try {
                indexDocument(doc.getId());
            } catch (Exception e) {
                log.error("文档索引失败: id={}", doc.getId(), e);
            }
        }
        log.info("批量索引完成: 共处理{}篇文档", pendingDocs.size());
    }

    private double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a.size() != b.size()) return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
