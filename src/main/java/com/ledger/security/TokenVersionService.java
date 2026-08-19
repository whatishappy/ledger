package com.ledger.security;

/**
 * Token 版本号服务
 */
public interface TokenVersionService {

    /**
     * 获取用户最新 Token 版本号
     * 优先查 Redis（token:version:{userId}，7天），回源 DB 并回写
     */
    Integer getTokenVersion(Long userId);

    /**
     * 用户 Token 版本号自增（改密/注销时调用，使所有旧Token失效）
     * 更新 DB + 刷新 Redis
     */
    void incrementTokenVersion(Long userId);
}
