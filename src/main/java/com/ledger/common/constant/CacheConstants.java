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

    /** 用户标签列表缓存：tags:{userId}，30分钟 */
    public static final String TAGS_KEY = "tags:%d";
    public static final long TAGS_TTL = 30 * 60 * 1000L;

    /** 用户模板列表缓存：templates:{userId}，30分钟 */
    public static final String TEMPLATES_KEY = "templates:%d";
    public static final long TEMPLATES_TTL = 30 * 60 * 1000L;

    /** AI会话消息缓存：ai:chat:{userId}:{sessionId}，1小时 */
    public static final String AI_CHAT_KEY = "ai:chat:%d:%d";
    public static final long AI_CHAT_TTL = 60 * 60 * 1000L;

    /** AI配额缓存：ai:quota:{userId}:{date}，24小时 */
    public static final String AI_QUOTA_KEY = "ai:quota:%d:%s";
    public static final long AI_QUOTA_TTL = 24 * 60 * 60 * 1000L;

    /** 日历视图缓存：calendar:{userId}:{month}，5分钟 */
    public static final String CALENDAR_KEY = "calendar:%d:%s";
    public static final long CALENDAR_TTL = 5 * 60 * 1000L;

    /** 登录失败计数缓存：login:fail:{username}，15分钟 */
    public static final String LOGIN_FAIL_KEY = "login:fail:%s";
    public static final long LOGIN_FAIL_TTL = 15 * 60 * 1000L;

    /** 导入预览临时数据：imports:preview:{userId}:{token}，10分钟 */
    public static final String IMPORT_PREVIEW_KEY = "imports:preview:%d:%s";
    public static final long IMPORT_PREVIEW_TTL = 10 * 60 * 1000L;

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

    public static String buildTagsKey(Long userId) {
        return String.format(TAGS_KEY, userId);
    }

    public static String buildTemplatesKey(Long userId) {
        return String.format(TEMPLATES_KEY, userId);
    }

    public static String buildAiChatKey(Long userId, Long sessionId) {
        return String.format(AI_CHAT_KEY, userId, sessionId);
    }

    public static String buildAiQuotaKey(Long userId, String date) {
        return String.format(AI_QUOTA_KEY, userId, date);
    }

    public static String buildCalendarKey(Long userId, String month) {
        return String.format(CALENDAR_KEY, userId, month);
    }

    public static String buildLoginFailKey(String username) {
        return String.format(LOGIN_FAIL_KEY, username);
    }

    public static String buildImportPreviewKey(Long userId, String token) {
        return String.format(IMPORT_PREVIEW_KEY, userId, token);
    }
}
