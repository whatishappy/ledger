package com.ledger.modules.ai.controller;

import com.ledger.common.result.Result;
import com.ledger.modules.ai.entity.AiKnowledgeDocument;
import com.ledger.modules.ai.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "AI知识库", description = "RAG知识库文档管理")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeService;

    @PostMapping
    @Operation(summary = "创建知识库文档")
    public Result<AiKnowledgeDocument> create(@RequestBody AiKnowledgeDocument doc) {
        return Result.success(knowledgeService.create(doc));
    }

    @GetMapping
    @Operation(summary = "获取所有知识库文档")
    public Result<List<AiKnowledgeDocument>> listAll() {
        return Result.success(knowledgeService.listAll());
    }

    @GetMapping("/type/{docType}")
    @Operation(summary = "按类型获取知识库文档")
    public Result<List<AiKnowledgeDocument>> listByType(@PathVariable String docType) {
        return Result.success(knowledgeService.listByType(docType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个文档详情")
    public Result<AiKnowledgeDocument> getById(@PathVariable Long id) {
        return Result.success(knowledgeService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文档")
    public Result<AiKnowledgeDocument> update(
            @PathVariable Long id,
            @RequestBody AiKnowledgeDocument doc) {
        return Result.success(knowledgeService.update(id, doc));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/index")
    @Operation(summary = "索引单个文档")
    public Result<Void> index(@PathVariable Long id) {
        knowledgeService.indexDocument(id);
        return Result.success();
    }

    @PostMapping("/index-all")
    @Operation(summary = "批量索引所有待索引文档")
    public Result<Void> indexAll() {
        knowledgeService.indexAllPending();
        return Result.success();
    }

    @GetMapping("/search")
    @Operation(summary = "关键词搜索知识库")
    public Result<List<AiKnowledgeDocument>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int limit) {
        return Result.success(knowledgeService.searchByKeyword(keyword, limit));
    }

    @GetMapping("/rag-search")
    @Operation(summary = "RAG语义搜索知识库")
    public Result<List<AiKnowledgeDocument>> ragSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        return Result.success(knowledgeService.ragSearch(query, topK));
    }
}
