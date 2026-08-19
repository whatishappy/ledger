package com.ledger.modules.ai.dto;

import lombok.Data;

@Data
public class AiAttachmentDTO {

    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
}
