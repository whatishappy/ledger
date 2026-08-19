package com.ledger.modules.monitor.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 监控指标配置（按详细设计 §9.1）
 * 注册业务指标 Bean
 */
@Configuration
public class MetricsConfig {

    public static final String METRIC_ACCOUNT_SUCCESS = "ledger.account.success.count";
    public static final String METRIC_ACCOUNT_FAIL = "ledger.account.fail.count";
    public static final String METRIC_EXPORT_SUCCESS = "ledger.export.success.count";
    public static final String METRIC_EXPORT_FAIL = "ledger.export.fail.count";
    public static final String METRIC_CACHE_HIT = "ledger.cache.hit.count";
    public static final String METRIC_CACHE_MISS = "ledger.cache.miss.count";
    public static final String METRIC_API_DURATION = "ledger.api.duration";
    public static final String METRIC_EXPORT_QUEUE_SIZE = "ledger.export.queue.size";

    @Bean
    public Counter accountSuccessCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_ACCOUNT_SUCCESS)
                .description("记账成功次数")
                .register(registry);
    }

    @Bean
    public Counter accountFailCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_ACCOUNT_FAIL)
                .description("记账失败次数")
                .register(registry);
    }

    @Bean
    public Counter exportSuccessCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_EXPORT_SUCCESS)
                .description("导出成功次数")
                .register(registry);
    }

    @Bean
    public Counter exportFailCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_EXPORT_FAIL)
                .description("导出失败次数")
                .register(registry);
    }

    @Bean
    public Counter cacheHitCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_CACHE_HIT)
                .description("缓存命中次数")
                .register(registry);
    }

    @Bean
    public Counter cacheMissCounter(MeterRegistry registry) {
        return Counter.builder(METRIC_CACHE_MISS)
                .description("缓存未命中次数")
                .register(registry);
    }

    @Bean
    public Timer apiDurationTimer(MeterRegistry registry) {
        return Timer.builder(METRIC_API_DURATION)
                .description("API响应时间")
                .register(registry);
    }
}
