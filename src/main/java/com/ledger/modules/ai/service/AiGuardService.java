package com.ledger.modules.ai.service;

import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
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
    private static final String PRIMARY_STATUS_KEY = "ai:status:primary";
    private static final String BACKUP_STATUS_KEY = "ai:status:backup";

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
        setStatus(PRIMARY_STATUS_KEY, "DOWN");
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
        setStatus(PRIMARY_STATUS_KEY, "UP");
    }

    public void recordBackupSuccess() {
        setStatus(BACKUP_STATUS_KEY, "UP");
    }

    public void recordBackupFail() {
        setStatus(BACKUP_STATUS_KEY, "DOWN");
    }

    @Scheduled(fixedDelay = 120000)
    public void healthCheckProbe() {
        if (!isDegraded()) {
            return;
        }
        log.info("执行主模型健康探测...");
        try {
            RBucket<Boolean> bucket = redissonClient.getBucket(DEGRADE_KEY);
            if (bucket != null) {
                bucket.delete();
            }
            primaryFailCount.set(0);
            setStatus(PRIMARY_STATUS_KEY, "RECOVERING");
            log.info("主模型降级标记已清除，进入恢复探测状态");
        } catch (Exception e) {
            log.warn("健康探测失败", e);
        }
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("degraded", isDegraded());
        status.put("primaryFailCount", primaryFailCount.get());
        status.put("primaryStatus", getStatus(PRIMARY_STATUS_KEY, "UNKNOWN"));
        status.put("backupStatus", getStatus(BACKUP_STATUS_KEY, "UNKNOWN"));
        status.put("blacklistedTools", BLACKLIST_TOOLS);
        return status;
    }

    private void setStatus(String key, String value) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(value, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.debug("设置状态失败: key={}", key);
        }
    }

    private String getStatus(String key, String defaultValue) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            String val = bucket.get();
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
