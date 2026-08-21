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
            log.error("查询幂等缓存失败: key={}", idempotentKey, e);
            return null;
        }
    }

    public void setIdempotentAccount(String idempotentKey, Long accountId) {
        try {
            RBucket<Long> bucket = redissonClient.getBucket(CacheConstants.buildIdempotentKey(idempotentKey));
            bucket.set(accountId, CacheConstants.IDEMPOTENT_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("写入幂等缓存失败: key={}", idempotentKey, e);
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
            log.error("查询Dashboard缓存失败: userId={}, month={}", userId, month, e);
            return null;
        }
    }

    public <T> void setDashboard(Long userId, String month, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildDashboardKey(userId, month));
            bucket.set(data, CacheConstants.DASHBOARD_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("写入Dashboard缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    public void evictDashboard(Long userId, String month) {
        try {
            redissonClient.getBucket(CacheConstants.buildDashboardKey(userId, month)).delete();
        } catch (Exception e) {
            log.error("删除Dashboard缓存失败: userId={}, month={}", userId, month, e);
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
            log.error("查询Budget缓存失败: userId={}, month={}", userId, month, e);
            return null;
        }
    }

    public <T> void setBudget(Long userId, String month, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildBudgetKey(userId, month));
            bucket.set(data, CacheConstants.BUDGET_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("写入Budget缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    public void evictBudget(Long userId, String month) {
        try {
            redissonClient.getBucket(CacheConstants.buildBudgetKey(userId, month)).delete();
        } catch (Exception e) {
            log.error("删除Budget缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    // ===== Calendar 日历热力图缓存 =====

    public <T> T getCalendar(Long userId, String month, Class<T> clazz) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildCalendarKey(userId, month));
            T data = bucket.get();
            recordCacheHitOrMiss(data != null);
            return data;
        } catch (Exception e) {
            log.error("查询Calendar缓存失败: userId={}, month={}", userId, month, e);
            return null;
        }
    }

    public <T> void setCalendar(Long userId, String month, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildCalendarKey(userId, month));
            bucket.set(data, CacheConstants.CALENDAR_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("写入Calendar缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    public void evictCalendar(Long userId, String month) {
        try {
            redissonClient.getBucket(CacheConstants.buildCalendarKey(userId, month)).delete();
        } catch (Exception e) {
            log.error("删除Calendar缓存失败: userId={}, month={}", userId, month, e);
        }
    }

    // ===== Tags 标签缓存 =====

    public <T> T getTags(Long userId, Class<T> clazz) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildTagsKey(userId));
            T data = bucket.get();
            recordCacheHitOrMiss(data != null);
            return data;
        } catch (Exception e) {
            log.error("查询Tags缓存失败: userId={}", userId, e);
            return null;
        }
    }

    public <T> void setTags(Long userId, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildTagsKey(userId));
            bucket.set(data, CacheConstants.TAGS_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("写入Tags缓存失败: userId={}", userId, e);
        }
    }

    public void evictTags(Long userId) {
        try {
            redissonClient.getBucket(CacheConstants.buildTagsKey(userId)).delete();
        } catch (Exception e) {
            log.error("删除Tags缓存失败: userId={}", userId, e);
        }
    }

    // ===== Import 预览缓存 =====

    public <T> T getImportPreview(Long userId, String token, Class<T> clazz) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildImportPreviewKey(userId, token));
            return bucket.get();
        } catch (Exception e) {
            log.error("查询Import预览缓存失败: userId={}, token={}", userId, token, e);
            return null;
        }
    }

    public <T> void setImportPreview(Long userId, String token, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildImportPreviewKey(userId, token));
            bucket.set(data, CacheConstants.IMPORT_PREVIEW_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("写入Import预览缓存失败: userId={}, token={}", userId, token, e);
        }
    }

    public void evictImportPreview(Long userId, String token) {
        try {
            redissonClient.getBucket(CacheConstants.buildImportPreviewKey(userId, token)).delete();
        } catch (Exception e) {
            log.error("删除Import预览缓存失败: userId={}, token={}", userId, token, e);
        }
    }

    // ===== Template 模板缓存 =====

    public <T> T getTemplates(Long userId, Class<T> clazz) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildTemplatesKey(userId));
            T data = bucket.get();
            recordCacheHitOrMiss(data != null);
            return data;
        } catch (Exception e) {
            log.error("查询Templates缓存失败: userId={}", userId, e);
            return null;
        }
    }

    public <T> void setTemplates(Long userId, T data) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(CacheConstants.buildTemplatesKey(userId));
            bucket.set(data, CacheConstants.TEMPLATES_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("写入Templates缓存失败: userId={}", userId, e);
        }
    }

    public void evictTemplates(Long userId) {
        try {
            redissonClient.getBucket(CacheConstants.buildTemplatesKey(userId)).delete();
        } catch (Exception e) {
            log.error("删除Templates缓存失败: userId={}", userId, e);
        }
    }

    // ===== 用户信息缓存 =====

    public void evictUser(Long userId) {
        try {
            redissonClient.getBucket(CacheConstants.buildUserKey(userId)).delete();
        } catch (Exception e) {
            log.error("删除用户缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 清除用户所有 dashboard、budget、calendar、tags、templates 缓存（注销时）
     */
    public void evictAllUserCaches(Long userId) {
        try {
            RKeys keys = redissonClient.getKeys();
            keys.deleteByPattern("dashboard:" + userId + ":*");
            keys.deleteByPattern("budget:" + userId + ":*");
            keys.deleteByPattern("calendar:" + userId + ":*");
            keys.delete(CacheConstants.buildTagsKey(userId));
            keys.delete(CacheConstants.buildTemplatesKey(userId));
            keys.delete(CacheConstants.buildUserKey(userId));
        } catch (Exception e) {
            log.error("清除用户所有缓存失败: userId={}", userId, e);
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
