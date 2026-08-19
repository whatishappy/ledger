package com.ledger.common.cache;

import com.ledger.common.constant.CacheConstants;
import com.ledger.modules.monitor.service.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务（封装 Redisson 操作）
 * Key 设计基于详细设计 §13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheService {

    private final RedissonClient redissonClient;
    private final BusinessMetricsService metricsService;

    // ===== 幂等缓存 =====

    public Long getIdempotentAccount(String idempotentKey) {
        try {
            RBucket<Long> bucket = redissonClient.getBucket(CacheConstants.buildIdempotentKey(idempotentKey));
            return bucket.get();
        } catch (Exception e) {
            log.warn("查询幂等缓存失败: key={}", idempotentKey, e);
            return null;
        }
    }

    public void setIdempotentAccount(String idempotentKey, Long accountId) {
        try {
            RBucket<Long> bucket = redissonClient.getBucket(CacheConstants.buildIdempotentKey(idempotentKey));
            bucket.set(accountId, CacheConstants.IDEMPOTENT_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("写入幂等缓存失败: key={}", idempotentKey, e);
        }
    }

    // ===== Dashboard 缓存 =====

    public <T> T getDashboard(Long userId, String month, Class<T> clazz) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildDashboardKey(userId, month));
            T data = bucket.get();
            recordCacheHitOrMiss(data != null);
            return data;
        } catch (Exception e) {
            log.warn("查询Dashboard缓存失败: userId={}, month={}", userId, month, e);
            return null;
        }
    }

    public <T> void setDashboard(Long userId, String month, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildDashboardKey(userId, month));
            bucket.set(data, CacheConstants.DASHBOARD_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("写入Dashboard缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    public void evictDashboard(Long userId, String month) {
        try {
            redissonClient.getBucket(CacheConstants.buildDashboardKey(userId, month)).delete();
        } catch (Exception e) {
            log.warn("删除Dashboard缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    // ===== Budget 缓存 =====

    public <T> T getBudget(Long userId, String month, Class<T> clazz) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildBudgetKey(userId, month));
            T data = bucket.get();
            recordCacheHitOrMiss(data != null);
            return data;
        } catch (Exception e) {
            log.warn("查询Budget缓存失败: userId={}, month={}", userId, month, e);
            return null;
        }
    }

    public <T> void setBudget(Long userId, String month, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildBudgetKey(userId, month));
            bucket.set(data, CacheConstants.BUDGET_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("写入Budget缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    public void evictBudget(Long userId, String month) {
        try {
            redissonClient.getBucket(CacheConstants.buildBudgetKey(userId, month)).delete();
        } catch (Exception e) {
            log.warn("删除Budget缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    // ===== 用户信息缓存 =====

    public void evictUser(Long userId) {
        try {
            redissonClient.getBucket(CacheConstants.buildUserKey(userId)).delete();
        } catch (Exception e) {
            log.warn("删除用户缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 清除用户所有 dashboard 和 budget 缓存（注销时）
     */
    public void evictAllUserCaches(Long userId) {
        try {
            RKeys keys = redissonClient.getKeys();
            keys.deleteByPattern("dashboard:" + userId + ":*");
            keys.deleteByPattern("budget:" + userId + ":*");
            keys.delete(CacheConstants.buildUserKey(userId));
        } catch (Exception e) {
            log.warn("清除用户所有缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 记录缓存命中/未命中指标
     */
    private void recordCacheHitOrMiss(boolean hit) {
        if (hit) {
            metricsService.recordCacheHit();
        } else {
            metricsService.recordCacheMiss();
        }
    }
}
