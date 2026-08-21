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
public class AiChatSessionVO {

    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime lastMessageAt;
    private Integer messageCount;
    private LocalDateTime createdAt;
}
