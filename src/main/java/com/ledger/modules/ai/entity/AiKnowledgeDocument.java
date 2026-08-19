package com.ledger.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_knowledge_document", autoResultMap = true)
public class AiKnowledgeDocument implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String docType;

    private String title;

    private String content;

    private String embeddingId;

    private Integer status;

    private LocalDateTime createdAt;
}
