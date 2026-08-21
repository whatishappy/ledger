package com.ledger.modules.account.listener;

import com.ledger.common.cache.CacheService;
import com.ledger.modules.account.event.AccountChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 账目变更事件监听器
 * 在事务提交后清除对应月份的 dashboard、budget 和 calendar 缓存
 * 跨月修改时发布两个事件，分别清除新旧月份缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountChangeListener {

    private final CacheService cacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountChange(AccountChangeEvent event) {
        Long userId = event.getUserId();
        String month = event.getMonth();
        log.info("事务提交后清除缓存: userId={}, month={}", userId, month);
        cacheService.evictDashboard(userId, month);
        cacheService.evictBudget(userId, month);
        cacheService.evictCalendar(userId, month);
    }
}
