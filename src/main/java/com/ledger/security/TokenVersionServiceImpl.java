package com.ledger.security;

import com.ledger.common.constant.CacheConstants;
import com.ledger.modules.user.entity.User;
import com.ledger.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 版本号服务实现
 * - 查询：优先 Redis 缓存（7天TTL），回源 DB 并回写
 * - 自增：更新 DB + 刷新 Redis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVersionServiceImpl implements TokenVersionService {

    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    @Override
    public Integer getTokenVersion(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = CacheConstants.buildTokenVersionKey(userId);
        try {
            RBucket<Integer> bucket = redissonClient.getBucket(key);
            Integer cached = bucket.get();
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("查询 Token 版本号缓存失败，降级查DB: userId={}", userId, e);
        }
        // 回源 DB
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        Integer version = user.getTokenVersion();
        // 回写缓存
        try {
            RBucket<Integer> bucket = redissonClient.getBucket(key);
            bucket.set(version, CacheConstants.TOKEN_VERSION_TTL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("回写 Token 版本号缓存失败: userId={}", userId, e);
        }
        return version;
    }

    @Override
    public void incrementTokenVersion(Long userId) {
        // 更新 DB：token_version + 1
        User update = new User();
        update.setId(userId);
        // 使用 SQL 自增避免并发问题
        userMapper.incrementTokenVersion(userId);
        // 刷新缓存
        User user = userMapper.selectById(userId);
        if (user != null) {
            String key = CacheConstants.buildTokenVersionKey(userId);
            try {
                RBucket<Integer> bucket = redissonClient.getBucket(key);
                bucket.set(user.getTokenVersion(), CacheConstants.TOKEN_VERSION_TTL, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.warn("刷新 Token 版本号缓存失败: userId={}", userId, e);
            }
        }
    }
}
