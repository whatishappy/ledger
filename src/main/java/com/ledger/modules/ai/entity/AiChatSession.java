package com.ledger.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_chat_session", autoResultMap = true)
public class AiChatSession implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String title;

    private LocalDateTime lastMessageAt;

    private Integer messageCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
