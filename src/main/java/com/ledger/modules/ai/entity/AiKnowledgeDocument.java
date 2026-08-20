package com.ledger.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_knowledge_document")
public class AiKnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("doc_type")
    private String docType;

    private String title;

    private String content;

    @TableField("embedding_id")
    private String embeddingId;

    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
