package com.ledger.modules.ai.service;

import com.ledger.modules.ai.dto.AiOcrResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AiOcrService {

    AiOcrResultDTO uploadAndOcr(Long userId, MultipartFile file);

    AiOcrResultDTO ocrByImageUrl(Long userId, String imageUrl);
}
