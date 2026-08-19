package com.ledger.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {

    private Long sessionId;

    @NotBlank(message = "聊天内容不能为空")
    @Size(max = 500, message = "聊天内容不能超过500字")
    private String content;

    private List<AiAttachmentDTO> attachments;
}
