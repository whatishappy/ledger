package com.ledger.common.constant;

/**
 * 缓存 Key 常量（基于详细设计 §13 完整缓存 Key 汇总）
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /** 用户信息缓存：user:{userId}，30分钟 */
    public static final String USER_KEY = "user:%d";
    public static final long USER_TTL = 30 * 60 * 1000L;

    /** 仪表盘缓存：dashboard:{userId}:{month}，5分钟 */
    public static final String DASHBOARD_KEY = "dashboard:%d:%s";
    public static final long DASHBOARD_TTL = 5 * 60 * 1000L;

    /** 预算列表缓存：budget:{userId}:{month}，10分钟 */
    public static final String BUDGET_KEY = "budget:%d:%s";
    public static final long BUDGET_TTL = 10 * 60 * 1000L;

    /** 用户 Token 版本号缓存：token:version:{userId}，7天 */
    public static final String TOKEN_VERSION_KEY = "token:version:%d";
    public static final long TOKEN_VERSION_TTL = 7 * 24 * 60 * 60 * 1000L;

    /** 记账幂等缓存：idempotent:account:{key}，5分钟 */
    public static final String IDEMPOTENT_ACCOUNT_KEY = "idempotent:account:%s";
    public static final long IDEMPOTENT_TTL = 5 * 60 * 1000L;

    public static String buildUserKey(Long userId) {
        return String.format(USER_KEY, userId);
    }

    public static String buildDashboardKey(Long userId, String month) {
        return String.format(DASHBOARD_KEY, userId, month);
    }

    public static String buildBudgetKey(Long userId, String month) {
        return String.format(BUDGET_KEY, userId, month);
    }

    public static String buildTokenVersionKey(Long userId) {
        return String.format(TOKEN_VERSION_KEY, userId);
    }

    public static String buildIdempotentKey(String md5Key) {
        return String.format(IDEMPOTENT_ACCOUNT_KEY, md5Key);
    }
}
