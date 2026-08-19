package com.ledger.common.context;

/**
 * 用户上下文（ThreadLocal）
 * 存储 JWT 认证过滤器解析出的当前用户ID
 * 约束：禁止在业务层手动清理，由过滤器 finally 块统一清理
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取当前用户ID，未认证抛出异常
     */
    public static Long requireUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null) {
            throw new IllegalStateException("当前线程未设置用户上下文");
        }
        return userId;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
