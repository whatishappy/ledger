package com.ledger.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName(value = "ai_chat_message", autoResultMap = true)
public class AiChatMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private String role;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String content;

    private Integer tokens;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String toolCalls;

    private LocalDateTime createdAt;
}
