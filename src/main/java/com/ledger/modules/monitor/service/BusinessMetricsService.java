package com.ledger.modules.monitor.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 业务指标埋点服务（按详细设计 §9.2）
 * 封装 Micrometer 指标记录方法
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * 记录记账成功
     */
    public void recordAccountSuccess() {
        incrementCounter(MetricsConfigConstants.METRIC_ACCOUNT_SUCCESS);
    }

    /**
     * 记录记账失败
     */
    public void recordAccountFail() {
        incrementCounter(MetricsConfigConstants.METRIC_ACCOUNT_FAIL);
    }

    /**
     * 记录导出成功
     */
    public void recordExportSuccess() {
        incrementCounter(MetricsConfigConstants.METRIC_EXPORT_SUCCESS);
    }

    /**
     * 记录导出失败
     */
    public void recordExportFail() {
        incrementCounter(MetricsConfigConstants.METRIC_EXPORT_FAIL);
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        incrementCounter(MetricsConfigConstants.METRIC_CACHE_HIT);
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        incrementCounter(MetricsConfigConstants.METRIC_CACHE_MISS);
    }

    /**
     * 记录每日记账数（Gauge，按天统计）
     */
    public void recordDailyAccountCount(long count) {
        meterRegistry.gauge("ledger.account.daily.count", count);
    }

    /**
     * 记录日均活跃用户数（Gauge）
     */
    public void recordDailyActiveUsers(long count) {
        meterRegistry.gauge("ledger.user.daily.active", count);
    }

    /**
     * 记录导出队列深度（Gauge）
     */
    public void recordExportQueueSize(int size) {
        meterRegistry.gauge("ledger.export.queue.size", size);
    }

    private void incrementCounter(String metricName) {
        try {
            Counter counter = meterRegistry.find(metricName).counter();
            if (counter == null) {
                counter = Counter.builder(metricName).register(meterRegistry);
            }
            counter.increment();
        } catch (Exception e) {
            log.warn("记录指标失败: {}", metricName, e);
        }
    }

    /**
     * 指标常量
     */
    private static final class MetricsConfigConstants {
        static final String METRIC_ACCOUNT_SUCCESS = "ledger.account.success.count";
        static final String METRIC_ACCOUNT_FAIL = "ledger.account.fail.count";
        static final String METRIC_EXPORT_SUCCESS = "ledger.export.success.count";
        static final String METRIC_EXPORT_FAIL = "ledger.export.fail.count";
        static final String METRIC_CACHE_HIT = "ledger.cache.hit.count";
        static final String METRIC_CACHE_MISS = "ledger.cache.miss.count";
    }
}
