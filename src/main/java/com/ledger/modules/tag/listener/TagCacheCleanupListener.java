package com.ledger.modules.tag.listener;

import com.ledger.common.cache.CacheService;
import com.ledger.modules.tag.event.TagChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagCacheCleanupListener {

    private final CacheService cacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTagChanged(TagChangedEvent event) {
        Long userId = event.getUserId();
        log.info("事务提交后清除缓存（标签变更）: userId={}", userId);
        cacheService.evictTags(userId);
    }
}
