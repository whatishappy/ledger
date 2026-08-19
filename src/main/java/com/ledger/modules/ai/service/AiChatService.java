package com.ledger.modules.ai.service;

import com.ledger.modules.ai.dto.AiChatRequest;
import com.ledger.modules.ai.vo.AiChatMessageVO;
import com.ledger.modules.ai.vo.AiChatSessionVO;
import com.ledger.modules.ai.vo.AiQuotaVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiChatService {

    Flux<String> streamChat(Long userId, AiChatRequest request);

    List<AiChatSessionVO> listSessions(Long userId, Integer page, Integer size);

    List<AiChatMessageVO> listMessages(Long userId, Long sessionId, Integer limit);

    void deleteSession(Long userId, Long sessionId);

    AiQuotaVO getQuota(Long userId);
}
