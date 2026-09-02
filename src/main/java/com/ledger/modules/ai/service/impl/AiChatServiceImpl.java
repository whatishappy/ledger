package com.ledger.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.common.constant.CacheConstants;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.ai.dto.AiChatRequest;
import com.ledger.modules.ai.entity.AiChatMessage;
import com.ledger.modules.ai.entity.AiChatSession;
import com.ledger.modules.ai.mapper.AiChatMessageMapper;
import com.ledger.modules.ai.mapper.AiChatSessionMapper;
import com.ledger.modules.ai.service.AiChatService;
import com.ledger.modules.ai.service.AiGuardService;
import com.ledger.modules.ai.service.LedgerAiTools;
import com.ledger.modules.ai.service.QuotaService;
import com.ledger.modules.ai.vo.AiChatMessageVO;
import com.ledger.modules.ai.vo.AiChatSessionVO;
import com.ledger.modules.ai.vo.AiQuotaVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final QuotaService quotaService;
    private final AiGuardService aiGuardService;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final LedgerAiTools ledgerAiTools;
    // 使用 ObjectProvider 让 AI key 缺失（Bean 返回 null）时也能启动，调用时再判空降级
    private final ObjectProvider<ChatLanguageModel> chatLanguageModelProvider;
    private final ObjectProvider<ChatLanguageModel> backupChatLanguageModelProvider;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final int HISTORY_LIMIT = 50;
    private static final String SYSTEM_PROMPT = buildSystemPrompt();

    private static String buildSystemPrompt() {
        return "你是一位专业的个人财务助手，服务于中文用户。" +
                "你可以调用以下11种工具来帮助用户（通过输出特殊的TOOL_CALL标记触发）：\n" +
                "1. natural_lang_bookkeeping(userId,date,type,category,amount,remark) - 自然语言记账，type=0支出/1收入\n" +
                "2. queryTransactions(userId,startDate,endDate,category,limit) - 查询交易\n" +
                "3. get_dashboard_summary(userId,month) - 获取某月仪表盘汇总\n" +
                "4. get_budget_status(userId,month) - 获取某月预算执行进度\n" +
                "5. get_tag_statistics(userId,year,month) - 获取月度标签统计\n" +
                "6. get_calendar_heatmap(userId,month) - 获取某月日历热力图\n" +
                "7. generate_saving_suggestions(userId,month) - 基础省钱建议\n" +
                "8. receipt_ocr(userId,imageUrl) - OCR识别小票图片\n" +
                "9. predict_expense(userId,month) - 预测指定月份的支出\n" +
                "10. recommend_budget(userId,month) - 智能预算推荐\n" +
                "11. generate_weekly_report(userId) - 生成周度财务报告\n" +
                "调用工具时，仅输出单行JSON：{\"tool\":\"toolName\",\"args\":{key:value,...}}，不要有额外文字。" +
                "用户ID会由系统自动注入。" +
                "根据用户意图选择最合适的工具：小票图片→receipt_ocr；支出预测→predict_expense；预算建议→recommend_budget；财务报告→generate_weekly_report。" +
                "收到工具返回结果后，用自然语言总结给用户。\n" +
                "回答要简洁、友好、专业，中文输出。";
    }

    @Override
    public Flux<String> streamChat(Long userId, AiChatRequest request) {
        Sinks.Many<String> sink = Sinks.many().multicast().directBestEffort();
        new Thread(() -> executeChat(userId, request, sink)).start();
        return sink.asFlux();
    }

    private void executeChat(Long userId, AiChatRequest request, Sinks.Many<String> sink) {
        try {
            int estimateTokens = request.getContent().length() * 2;
            boolean writeOper = request.getContent().contains("记") || request.getContent().contains("加")
                    || request.getContent().contains("入") || request.getContent().contains("写");
            quotaService.checkAndConsume(userId, estimateTokens, writeOper);

            Long sessionId = resolveSessionId(userId, request);

            List<ChatMessage> messageHistory = loadMessageHistory(userId, sessionId);

            AiChatMessage userMsg = saveUserMessage(userId, sessionId, request.getContent());
            cacheChatMessage(userId, sessionId, userMsg);

            List<ChatMessage> workingMessages = new ArrayList<>(messageHistory);
            workingMessages.add(UserMessage.from(request.getContent()));

            String fullResponse = runAgentLoop(userId, workingMessages, sink);

            AiChatMessage assistantMsg = saveAssistantMessage(userId, sessionId, fullResponse);
            cacheChatMessage(userId, sessionId, assistantMsg);
            updateSessionAfterMessage(userId, sessionId);

            sink.tryEmitNext("data: [DONE]\n\n");
            sink.tryEmitComplete();
        } catch (BusinessException e) {
            log.warn("AI对话业务异常: userId={}, code={}, msg={}", userId, e.getResultCode().getCode(), e.getMessage());
            sink.tryEmitNext("error: " + toJson(new ErrorPayload(e.getResultCode().getCode(), e.getMessage())) + "\n\n");
            sink.tryEmitComplete();
        } catch (Exception e) {
            log.error("AI对话系统异常: userId={}", userId, e);
            sink.tryEmitNext("error: " + toJson(new ErrorPayload(6001, "AI对话失败：" + e.getMessage())) + "\n\n");
            sink.tryEmitComplete();
        }
    }

    private String runAgentLoop(Long userId, List<ChatMessage> messages, Sinks.Many<String> sink) {
        List<ChatMessage> loopMessages = new ArrayList<>(messages);
        if (loopMessages.isEmpty() || !(loopMessages.get(0) instanceof SystemMessage)) {
            loopMessages.add(0, SystemMessage.from(SYSTEM_PROMPT));
        }

        StringBuilder fullAnswer = new StringBuilder();
        int maxIterations = 5;
        for (int i = 0; i < maxIterations; i++) {
            String rawModelOutput;
            try {
                ChatLanguageModel model = selectModel();
                rawModelOutput = callModelViaReflection(model, loopMessages);
                aiGuardService.recordPrimarySuccess();
                if (rawModelOutput == null) rawModelOutput = "";
            } catch (Exception e) {
                // 6011 = AI 服务未配置，属环境问题而非模型调用失败，直接穿透降级逻辑
                if (e instanceof BusinessException be
                        && be.getResultCode() == ResultCode.AI_SERVICE_UNCONFIGURED) {
                    throw be;
                }
                log.warn("模型调用异常，迭代={}, error={}", i, e.getMessage());
                aiGuardService.recordPrimaryFail();
                ChatLanguageModel backup = backupChatLanguageModelProvider.getIfAvailable();
                if (i == 0 && backup != null) {
                    try {
                        rawModelOutput = callModelViaReflection(backup, loopMessages);
                        aiGuardService.recordBackupSuccess();
                        if (rawModelOutput == null) rawModelOutput = "";
                    } catch (Exception ex) {
                        aiGuardService.recordBackupFail();
                        if (fullAnswer.length() == 0) {
                            return degradeFallback(userId, sink);
                        }
                        break;
                    }
                } else if (fullAnswer.length() == 0) {
                    return degradeFallback(userId, sink);
                } else {
                    break;
                }
            }

            ToolCall toolCall = parseToolCall(rawModelOutput);
            if (toolCall == null) {
                emitChunks(sink, rawModelOutput);
                fullAnswer.append(rawModelOutput);
                loopMessages.add(AiMessage.from(rawModelOutput));
                break;
            }

            String toolName = toolCall.name;
            Map<String, Object> args = toolCall.args;
            args.put("userId", userId);

            try {
                aiGuardService.assertNotBlacklisted(toolName);
                injectMissingArgs(toolName, args);
                String toolResultStr = invokeTool(toolName, args);

                String resultEvent = String.format(
                        "{\"type\":\"tool_call\",\"tool\":\"%s\",\"result\":%s}",
                        toolName, toJsonSafe(toolResultStr));
                sink.tryEmitNext("data: " + resultEvent + "\n\n");

                loopMessages.add(AiMessage.from("调用工具" + toolName + "，参数:" + toJson(args)));
                loopMessages.add(UserMessage.from("工具" + toolName + "返回结果：" + toolResultStr));
            } catch (Exception e) {
                log.warn("工具执行失败: tool={}, error={}", toolName, e.getMessage());
                String errResult = String.format(
                        "{\"type\":\"tool_call\",\"tool\":\"%s\",\"error\":\"%s\"}",
                        toolName, e.getMessage());
                sink.tryEmitNext("data: " + errResult + "\n\n");
                loopMessages.add(UserMessage.from("工具" + toolName + "执行失败：" + e.getMessage()));
            }
        }
        return fullAnswer.toString();
    }

    private String degradeFallback(Long userId, Sinks.Many<String> sink) {
        String month = YearMonth.now().toString();
        String fallback = "AI模型暂时不可用，我先给您提供本地财务查询和省钱建议服务。\n";
        try {
            String suggestions = ledgerAiTools.generateSavingSuggestions(userId, month);
            fallback += "\n【本月省钱建议】\n" + suggestions;
        } catch (Exception ignored) {
        }
        emitChunks(sink, fallback);
        return fallback;
    }

    private void injectMissingArgs(String toolName, Map<String, Object> args) {
        String curMonth = YearMonth.now().toString();
        LocalDate today = LocalDate.now();
        switch (toolName) {
            case "queryTransactions":
                if (!args.containsKey("startDate")) args.put("startDate", today.minusDays(30).toString());
                if (!args.containsKey("endDate")) args.put("endDate", today.toString());
                if (!args.containsKey("limit")) args.put("limit", 20);
                break;
            case "get_dashboard_summary":
            case "get_budget_status":
            case "get_calendar_heatmap":
            case "generate_saving_suggestions":
                if (!args.containsKey("month")) args.put("month", curMonth);
                break;
            case "get_tag_statistics":
                if (!args.containsKey("year")) args.put("year", today.getYear());
                if (!args.containsKey("month")) args.put("month", today.getMonthValue());
                break;
            case "natural_lang_bookkeeping":
                if (!args.containsKey("date")) args.put("date", today.toString());
                break;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String invokeTool(String toolName, Map<String, Object> args) throws Exception {
        Method[] methods = LedgerAiTools.class.getDeclaredMethods();
        for (Method m : methods) {
            dev.langchain4j.agent.tool.Tool toolAnno = m.getAnnotation(dev.langchain4j.agent.tool.Tool.class);
            String annotatedName = m.getName();
            if (toolAnno != null) {
                try {
                    Object v = dev.langchain4j.agent.tool.Tool.class.getMethod("value").invoke(toolAnno);
                    if (v != null) {
                        if (v instanceof String s && !s.isEmpty()) annotatedName = s;
                        else if (v instanceof String[] arr && arr.length > 0 && arr[0] != null && !arr[0].isEmpty()) annotatedName = arr[0];
                    }
                } catch (Exception ignored) {
                }
            }
            if (!toolName.equals(annotatedName) && !toolName.equals(m.getName())) continue;

            Class<?>[] paramTypes = m.getParameterTypes();
            String[] paramNames = getParamNames(m);
            Object[] invokeArgs = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                String pname = paramNames[i];
                Object val = args.get(pname);
                if (val == null && paramNames[i].equals("userId")) val = args.get("userId");
                invokeArgs[i] = convertType(val, paramTypes[i]);
            }
            Object result = m.invoke(ledgerAiTools, invokeArgs);
            return result != null ? result.toString() : "";
        }
        throw new IllegalArgumentException("工具不存在: " + toolName);
    }

    private String[] getParamNames(Method m) {
        return switch (m.getName()) {
            case "createAccountFromNL" -> new String[]{"userId","date","type","category","amount","remark"};
            case "queryTransactions" -> new String[]{"userId","startDate","endDate","category","limit"};
            case "getDashboardSummary" -> new String[]{"userId","month"};
            case "getBudgetStatus" -> new String[]{"userId","month"};
            case "getTagStatistics" -> new String[]{"userId","year","month"};
            case "getCalendarHeatmap" -> new String[]{"userId","month"};
            case "generateSavingSuggestions" -> new String[]{"userId","month"};
            case "receiptOcr" -> new String[]{"userId","imageUrl"};
            case "predictExpense" -> new String[]{"userId","month"};
            case "recommendBudget" -> new String[]{"userId","month"};
            case "generateWeeklyReport" -> new String[]{"userId"};
            default -> {
                java.lang.reflect.Parameter[] params = m.getParameters();
                String[] names = new String[params.length];
                for (int i = 0; i < params.length; i++) names[i] = params[i].getName();
                yield names;
            }
        };
    }

    private Object convertType(Object val, Class<?> targetType) {
        if (val == null) {
            if (targetType == Long.class) return null;
            if (targetType == Integer.class) return null;
            if (targetType == LocalDate.class) return LocalDate.now();
            if (targetType.isPrimitive()) {
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
            }
            return null;
        }
        if (targetType.isInstance(val)) return val;
        String s = val.toString();
        if (targetType == Long.class || targetType == long.class) return Long.parseLong(s);
        if (targetType == Integer.class || targetType == int.class) {
            try { return Integer.parseInt(s); } catch (Exception e) { return null; }
        }
        if (targetType == BigDecimal.class) return new BigDecimal(s);
        if (targetType == LocalDate.class) return LocalDate.parse(s);
        if (targetType == String.class) return s;
        return val;
    }

    private ToolCall parseToolCall(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (!trimmed.startsWith("{")) return null;
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (!node.has("tool")) return null;
            ToolCall tc = new ToolCall();
            tc.name = node.get("tool").asText();
            tc.args = new java.util.HashMap<>();
            if (node.has("args")) {
                Iterator<Map.Entry<String, JsonNode>> it = node.get("args").fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    JsonNode v = e.getValue();
                    if (v.isNull()) continue;
                    Object pv;
                    if (v.isNumber()) {
                        if (v.doubleValue() == v.longValue()) pv = v.longValue();
                        else pv = v.decimalValue();
                    } else if (v.isBoolean()) pv = v.booleanValue();
                    else pv = v.asText();
                    tc.args.put(e.getKey(), pv);
                }
            }
            return tc;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private ChatLanguageModel selectModel() {
        ChatLanguageModel backup = backupChatLanguageModelProvider.getIfAvailable();
        if (aiGuardService.isDegraded() && backup != null) {
            log.debug("使用备用模型（降级模式）");
            return backup;
        }
        ChatLanguageModel primary = chatLanguageModelProvider.getIfAvailable();
        if (primary != null) {
            return primary;
        }
        if (backup != null) {
            return backup;
        }
        throw new BusinessException(ResultCode.AI_SERVICE_UNCONFIGURED);
    }

    private void emitChunks(Sinks.Many<String> sink, String text) {
        if (text == null || text.isEmpty()) return;
        int step = 3;
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + step, text.length());
            String chunk = text.substring(i, end);
            String event = String.format("{\"type\":\"chunk\",\"content\":%s}", toJsonSafe(chunk));
            sink.tryEmitNext("data: " + event + "\n\n");
            try { Thread.sleep(15); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    private String callModelViaReflection(Object model, List<ChatMessage> messages) throws Exception {
        if (model == null) {
            throw new BusinessException(ResultCode.AI_CHAT_FAILED, "AI模型未配置");
        }
        String fallback = toChatOnlyPrompt(messages);
        Method[] methods = model.getClass().getInterfaces().length > 0
                ? model.getClass().getInterfaces()[0].getMethods()
                : model.getClass().getMethods();

        for (Method m : methods) {
            if (!"generate".equals(m.getName()) && !"chat".equals(m.getName())) continue;
            Class<?>[] pts = m.getParameterTypes();
            try {
                Object result;
                if (pts.length == 1 && List.class.isAssignableFrom(pts[0])) {
                    result = m.invoke(model, messages);
                } else if (pts.length == 1 && pts[0].isArray() && ChatMessage.class.isAssignableFrom(pts[0].getComponentType())) {
                    result = m.invoke(model, (Object) messages.toArray(new ChatMessage[0]));
                } else if (pts.length > 0 && ChatMessage.class.isAssignableFrom(pts[0])) {
                    Object[] arr = messages.toArray(new ChatMessage[0]);
                    Object castArr = java.lang.reflect.Array.newInstance(ChatMessage.class, arr.length);
                    System.arraycopy(arr, 0, castArr, 0, arr.length);
                    result = m.invoke(model, castArr);
                } else if (pts.length == 1 && String.class.isAssignableFrom(pts[0])) {
                    result = m.invoke(model, fallback);
                } else {
                    continue;
                }
                return extractTextFromResult(result);
            } catch (IllegalArgumentException | IllegalAccessException ignored) {
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable t = e.getTargetException() != null ? e.getTargetException() : e;
                if (t instanceof RuntimeException re) throw re;
                throw new RuntimeException(t);
            }
        }

        List<Method> all = new ArrayList<>();
        try {
            Collections.addAll(all, model.getClass().getMethods());
        } catch (Exception ignored) {
        }
        for (Method m : all) {
            if (!"generate".equals(m.getName()) && !"chat".equals(m.getName())) continue;
            Class<?>[] pts = m.getParameterTypes();
            try {
                Object result;
                if (pts.length == 1 && String.class.isAssignableFrom(pts[0])) {
                    result = m.invoke(model, fallback);
                } else if (pts.length == 1 && List.class.isAssignableFrom(pts[0])) {
                    result = m.invoke(model, messages);
                } else {
                    continue;
                }
                return extractTextFromResult(result);
            } catch (IllegalArgumentException | IllegalAccessException ignored) {
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable t = e.getTargetException() != null ? e.getTargetException() : e;
                if (t instanceof RuntimeException re) throw re;
                throw new RuntimeException(t);
            }
        }
        throw new BusinessException(ResultCode.AI_CHAT_FAILED, "无法调用模型generate方法");
    }

    private String extractTextFromResult(Object result) throws Exception {
        if (result == null) return "";
        if (result instanceof String s) return s;
        // langchain4j 1.x: ChatResponse.aiMessage(); 0.x: Response.content()
        for (String accessor : new String[]{"aiMessage", "content", "getContent"}) {
            try {
                Method m = result.getClass().getMethod(accessor);
                Object msg = m.invoke(result);
                String text = extractMessageText(msg);
                if (text != null) return text;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        return result.toString();
    }

    private String extractMessageText(Object msg) {
        if (msg == null) return null;
        if (msg instanceof String s) return s;
        for (String accessor : new String[]{"text", "getText", "textContent"}) {
            try {
                Method m = msg.getClass().getMethod(accessor);
                Object t = m.invoke(msg);
                if (t != null) return t.toString();
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String toChatOnlyPrompt(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            String prefix;
            String text;
            if (msg instanceof SystemMessage sm) {
                prefix = "System";
                text = sm.text();
            } else if (msg instanceof UserMessage um) {
                // UserMessage.text() 已弃用，改用 contents() 提取文本
                prefix = "User";
                text = userMessageText(um);
            } else if (msg instanceof AiMessage am) {
                prefix = "Assistant";
                text = am.text();
            } else {
                continue;
            }
            sb.append(prefix).append(": ").append(text).append('\n');
        }
        sb.append("Assistant: ");
        return sb.toString();
    }

    private String userMessageText(UserMessage um) {
        for (Content c : um.contents()) {
            if (c instanceof TextContent tc) {
                String t = tc.text();
                if (t != null) return t;
            }
        }
        return "";
    }

    private Long resolveSessionId(Long userId, AiChatRequest request) {
        if (request.getSessionId() != null) {
            AiChatSession session = sessionMapper.selectById(request.getSessionId());
            if (session != null && session.getUserId().equals(userId)) {
                return session.getId();
            }
        }
        AiChatSession newSession = new AiChatSession();
        newSession.setUserId(userId);
        String title = request.getContent();
        if (title.length() > 15) title = title.substring(0, 15);
        newSession.setTitle(title);
        newSession.setMessageCount(0);
        newSession.setCreatedAt(LocalDateTime.now());
        newSession.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(newSession);
        return newSession.getId();
    }

    private List<ChatMessage> loadMessageHistory(Long userId, Long sessionId) {
        List<ChatMessage> result = new ArrayList<>();
        try {
            String cacheKey = CacheConstants.buildAiChatKey(userId, sessionId);
            RDeque<String> deque = redissonClient.getDeque(cacheKey);
            List<String> cached = deque.readAll();
            int start = Math.max(0, cached.size() - HISTORY_LIMIT);
            for (int i = start; i < cached.size(); i++) {
                ChatMessage msg = parseChatMessage(cached.get(i));
                if (msg != null) result.add(msg);
            }
        } catch (Exception e) {
            log.warn("从Redis加载聊天历史失败，将从MySQL加载", e);
        }
        if (result.size() < HISTORY_LIMIT) {
            LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                    .eq(AiChatMessage::getSessionId, sessionId)
                    .orderByDesc(AiChatMessage::getId)
                    .last("LIMIT " + HISTORY_LIMIT);
            List<AiChatMessage> dbMessages = messageMapper.selectList(wrapper);
            for (int i = dbMessages.size() - 1; i >= 0; i--) {
                AiChatMessage db = dbMessages.get(i);
                result.add(toChatMessage(db));
            }
        }
        return result;
    }

    private AiChatMessage saveUserMessage(Long userId, Long sessionId, String content) {
        AiChatMessage msg = new AiChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole("user");
        msg.setContent(content);
        msg.setTokens(content.length() * 2);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return msg;
    }

    private AiChatMessage saveAssistantMessage(Long userId, Long sessionId, String content) {
        AiChatMessage msg = new AiChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setTokens(content.length() * 3);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);
        return msg;
    }

    private void cacheChatMessage(Long userId, Long sessionId, AiChatMessage msg) {
        try {
            String cacheKey = CacheConstants.buildAiChatKey(userId, sessionId);
            RDeque<String> deque = redissonClient.getDeque(cacheKey);
            deque.addLast(toJson(msg));
            while (deque.size() > HISTORY_LIMIT) {
                deque.removeFirst();
            }
            deque.expire(java.time.Duration.ofMillis(CacheConstants.AI_CHAT_TTL));
        } catch (Exception e) {
            log.warn("缓存聊天消息失败", e);
        }
    }

    private void updateSessionAfterMessage(Long userId, Long sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) return;
        session.setLastMessageAt(LocalDateTime.now());
        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 2);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    public List<AiChatSessionVO> listSessions(Long userId, Integer page, Integer size) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 20;
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getLastMessageAt, AiChatSession::getCreatedAt)
                .last("LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize);
        List<AiChatSession> sessions = sessionMapper.selectList(wrapper);
        return sessions.stream().map(this::toSessionVO).toList();
    }

    @Override
    public List<AiChatMessageVO> listMessages(Long userId, Long sessionId, Integer limit) {
        int lim = limit != null && limit > 0 ? limit : 50;
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT " + lim);
        List<AiChatMessage> messages = messageMapper.selectList(wrapper);
        List<AiChatMessageVO> result = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            result.add(toMessageVO(messages.get(i)));
        }
        return result;
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        LambdaQueryWrapper<AiChatMessage> msgWrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId);
        messageMapper.delete(msgWrapper);
        sessionMapper.deleteById(sessionId);
        try {
            redissonClient.getDeque(CacheConstants.buildAiChatKey(userId, sessionId)).delete();
        } catch (Exception e) {
            log.warn("删除会话缓存失败", e);
        }
    }

    @Override
    public AiQuotaVO getQuota(Long userId) {
        return quotaService.getQuota(userId);
    }

    private AiChatSessionVO toSessionVO(AiChatSession s) {
        return AiChatSessionVO.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .title(s.getTitle())
                .lastMessageAt(s.getLastMessageAt())
                .messageCount(s.getMessageCount())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private AiChatMessageVO toMessageVO(AiChatMessage m) {
        return AiChatMessageVO.builder()
                .id(m.getId())
                .sessionId(m.getSessionId())
                .role(m.getRole())
                .content(m.getContent())
                .tokens(m.getTokens())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private ChatMessage toChatMessage(AiChatMessage m) {
        if ("user".equals(m.getRole())) {
            return UserMessage.from(m.getContent() != null ? m.getContent() : "");
        } else if ("assistant".equals(m.getRole())) {
            return AiMessage.from(m.getContent() != null ? m.getContent() : "");
        } else {
            return null;
        }
    }

    private ChatMessage parseChatMessage(String json) {
        try {
            AiChatMessage m = objectMapper.readValue(json, AiChatMessage.class);
            return toChatMessage(m);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String toJsonSafe(String text) {
        try {
            return objectMapper.writeValueAsString(text);
        } catch (JsonProcessingException e) {
            return "\"" + text.replace("\"", "\\\"") + "\"";
        }
    }

    private record ErrorPayload(int code, String message) {
    }

    private static class ToolCall {
        String name;
        Map<String, Object> args;
    }
}
