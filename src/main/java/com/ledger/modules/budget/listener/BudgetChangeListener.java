package com.ledger.modules.budget.listener;

import com.ledger.common.cache.CacheService;
import com.ledger.modules.budget.event.BudgetChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 预算变更事件监听器
 * 在事务提交后清除对应月份的 budget 和 dashboard 缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetChangeListener {

    private final CacheService cacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBudgetChange(BudgetChangeEvent event) {
        Long userId = event.getUserId();
        String month = event.getMonth();
        log.info("事务提交后清除缓存（预算变更）: userId={}, month={}", userId, month);
        cacheService.evictBudget(userId, month);
        cacheService.evictDashboard(userId, month);
    }
}
