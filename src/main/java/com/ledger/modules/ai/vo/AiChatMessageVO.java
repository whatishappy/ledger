package com.ledger.modules.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageVO {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Integer tokens;
    private LocalDateTime createdAt;
}
