package com.ledger.modules.ai.service;

import com.ledger.modules.ai.entity.AiKnowledgeDocument;

import java.util.List;

public interface KnowledgeBaseService {

    AiKnowledgeDocument create(AiKnowledgeDocument doc);

    AiKnowledgeDocument update(Long id, AiKnowledgeDocument doc);

    void delete(Long id);

    AiKnowledgeDocument getById(Long id);

    List<AiKnowledgeDocument> listAll();

    List<AiKnowledgeDocument> listByType(String docType);

    List<AiKnowledgeDocument> searchByKeyword(String keyword, int limit);

    List<AiKnowledgeDocument> ragSearch(String query, int topK);

    void indexDocument(Long id);

    void indexAllPending();
}
