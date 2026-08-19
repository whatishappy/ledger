package com.ledger.modules.ai.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.ai.dto.AiChatRequest;
import com.ledger.modules.ai.service.AiChatService;
import com.ledger.modules.ai.vo.AiChatMessageVO;
import com.ledger.modules.ai.vo.AiChatSessionVO;
import com.ledger.modules.ai.vo.AiQuotaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI模块", description = "AI智能助手聊天、会话、配额接口（AI-01~AI-05）")
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping(value = "/chat:stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI-01 SSE流式聊天接口")
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody AiChatRequest request) {
        Long userId = UserContext.requireUserId();
        log.info("AI流式聊天请求: userId={}, sessionId={}", userId, request.getSessionId());
        return aiChatService.streamChat(userId, request)
                .map(raw -> ServerSentEvent.<String>builder()
                        .data(raw)
                        .build())
                .onErrorResume(e -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data("{\"code\":6001,\"message\":\"" + e.getMessage() + "\"}")
                        .build()));
    }

    @GetMapping("/sessions")
    @Operation(summary = "AI-02 查询会话列表")
    public Result<List<AiChatSessionVO>> listSessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = UserContext.requireUserId();
        List<AiChatSessionVO> sessions = aiChatService.listSessions(userId, page, size);
        return Result.success(sessions);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "AI-03 查询会话历史消息")
    public Result<List<AiChatMessageVO>> listMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "50") Integer limit) {
        Long userId = UserContext.requireUserId();
        List<AiChatMessageVO> messages = aiChatService.listMessages(userId, sessionId, limit);
        return Result.success(messages);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "AI-04 删除会话")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = UserContext.requireUserId();
        aiChatService.deleteSession(userId, sessionId);
        return Result.success();
    }

    @GetMapping("/quota")
    @Operation(summary = "AI-05 查询今日AI配额")
    public Result<AiQuotaVO> getQuota() {
        Long userId = UserContext.requireUserId();
        AiQuotaVO quota = aiChatService.getQuota(userId);
        return Result.success(quota);
    }
}
