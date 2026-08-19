package com.ledger.modules.ai.service;

import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGuardService {

    private final RedissonClient redissonClient;

    private static final Set<String> BLACKLIST_TOOLS = Set.of(
            "delete_account",
            "update_user_password",
            "modify_budget_beyond_limit",
            "delete_user",
            "export_excel",
            "refresh_token"
    );

    private static final String DEGRADE_KEY = "ai:degrade:1";
    private static final String FAIL_COUNT_KEY = "ai:failcount:primary";

    private final AtomicInteger primaryFailCount = new AtomicInteger(0);

    public void assertNotBlacklisted(String toolName) {
        if (BLACKLIST_TOOLS.contains(toolName)) {
            log.warn("AI工具调用被黑名单拦截: toolName={}", toolName);
            throw new BusinessException(ResultCode.AI_OPERATION_BLACKLISTED);
        }
    }

    public boolean isDegraded() {
        try {
            RBucket<Boolean> bucket = redissonClient.getBucket(DEGRADE_KEY);
            Boolean degraded = bucket.get();
            return degraded != null && degraded;
        } catch (Exception e) {
            log.warn("检查降级状态失败，默认不降级", e);
            return false;
        }
    }

    public void recordPrimaryFail() {
        int count = primaryFailCount.incrementAndGet();
        log.warn("主模型调用失败，累计失败次数: {}", count);
        if (count >= 3) {
            try {
                RBucket<Boolean> bucket = redissonClient.getBucket(DEGRADE_KEY);
                bucket.set(true, 10, TimeUnit.MINUTES);
                log.info("主模型连续3次失败，自动切换到备用模型10分钟");
            } catch (Exception e) {
                log.warn("设置降级标记失败", e);
            }
            primaryFailCount.set(0);
        }
    }

    public void recordPrimarySuccess() {
        if (primaryFailCount.get() > 0) {
            primaryFailCount.set(0);
            log.debug("主模型调用成功，重置失败计数器");
        }
    }
}
