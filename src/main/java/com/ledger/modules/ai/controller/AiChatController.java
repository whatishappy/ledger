package com.ledger.modules.ai.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.ai.dto.AiChatRequest;
import com.ledger.modules.ai.dto.AiOcrResultDTO;
import com.ledger.modules.ai.service.AiChatService;
import com.ledger.modules.ai.service.AiGuardService;
import com.ledger.modules.ai.service.AiOcrService;
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
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI模块", description = "AI智能助手聊天、会话、配额、小票OCR接口（AI-01~AI-10）")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiOcrService aiOcrService;
    private final AiGuardService aiGuardService;

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

    @PostMapping(value = "/receipt/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI-06 上传小票图片并OCR识别")
    public Result<AiOcrResultDTO> uploadReceipt(
            @RequestParam("file") MultipartFile file) {
        Long userId = UserContext.requireUserId();
        log.info("小票上传OCR请求: userId={}, fileName={}, size={}", userId,
                file.getOriginalFilename(), file.getSize());
        AiOcrResultDTO result = aiOcrService.uploadAndOcr(userId, file);
        return Result.success(result);
    }

    @PostMapping(value = "/receipt/ocr")
    @Operation(summary = "AI-07 通过图片URL进行OCR识别")
    public Result<AiOcrResultDTO> ocrReceiptByUrl(@RequestParam("imageUrl") String imageUrl) {
        Long userId = UserContext.requireUserId();
        log.info("小票URL OCR请求: userId={}, imageUrl={}", userId, imageUrl);
        AiOcrResultDTO result = aiOcrService.ocrByImageUrl(userId, imageUrl);
        return Result.success(result);
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

    @GetMapping("/health")
    @Operation(summary = "AI-08 查询AI模型健康状态")
    public Result<java.util.Map<String, Object>> getAiHealth() {
        return Result.success(aiGuardService.getHealthStatus());
    }
}
