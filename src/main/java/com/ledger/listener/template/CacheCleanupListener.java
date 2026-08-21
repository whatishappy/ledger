package com.ledger.listener.template;

import com.ledger.common.cache.CacheService;
import com.ledger.event.template.TemplateChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheCleanupListener {

    private final CacheService cacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTemplateChanged(TemplateChangedEvent event) {
        Long userId = event.getUserId();
        log.info("事务提交后清除模板缓存: userId={}", userId);
        cacheService.evictTemplates(userId);
    }
}
