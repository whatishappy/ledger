package com.ledger.modules.ai.service;

import com.ledger.common.constant.CacheConstants;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.ai.vo.AiQuotaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final RedissonClient redissonClient;

    private static final int DAILY_CHAT_LIMIT = 50;
    private static final long DAILY_TOKEN_LIMIT = 100000L;
    private static final String FIELD_CHAT_USED = "chatUsed";
    private static final String FIELD_TOKEN_USED = "tokenUsed";

    public boolean checkAndConsume(Long userId, int estimateTokens, boolean writeOper) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = CacheConstants.buildAiQuotaKey(userId, date);

        RMap<String, Integer> quotaMap = redissonClient.getMap(key);

        Integer chatUsed = quotaMap.get(FIELD_CHAT_USED);
        if (chatUsed == null) chatUsed = 0;

        Integer tokenUsed = quotaMap.get(FIELD_TOKEN_USED);
        if (tokenUsed == null) tokenUsed = 0;

        if (chatUsed + 1 > DAILY_CHAT_LIMIT) {
            throw new BusinessException(ResultCode.AI_QUOTA_EXHAUSTED);
        }
        if ((long) tokenUsed + estimateTokens > DAILY_TOKEN_LIMIT) {
            throw new BusinessException(ResultCode.AI_QUOTA_EXHAUSTED);
        }

        quotaMap.put(FIELD_CHAT_USED, chatUsed + 1);
        quotaMap.put(FIELD_TOKEN_USED, tokenUsed + estimateTokens);

        long ttlSeconds = calculateTtlSeconds();
        if (!quotaMap.isExists() || quotaMap.remainTimeToLive() <= 0) {
            quotaMap.expire(ttlSeconds, TimeUnit.SECONDS);
        }

        return true;
    }

    public AiQuotaVO getQuota(Long userId) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = CacheConstants.buildAiQuotaKey(userId, date);

        RMap<String, Integer> quotaMap = redissonClient.getMap(key);
        Integer chatUsed = quotaMap.get(FIELD_CHAT_USED);
        Integer tokenUsed = quotaMap.get(FIELD_TOKEN_USED);

        int chatUsedVal = chatUsed != null ? chatUsed : 0;
        long tokenUsedVal = tokenUsed != null ? tokenUsed.longValue() : 0L;

        double chatPercent = (chatUsedVal * 100.0) / DAILY_CHAT_LIMIT;
        double tokenPercent = (tokenUsedVal * 100.0) / DAILY_TOKEN_LIMIT;

        return new AiQuotaVO(
                chatUsedVal,
                DAILY_CHAT_LIMIT,
                tokenUsedVal,
                DAILY_TOKEN_LIMIT,
                chatPercent,
                tokenPercent
        );
    }

    private long calculateTtlSeconds() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59));
        return java.time.Duration.between(now, endOfDay).getSeconds();
    }
}
