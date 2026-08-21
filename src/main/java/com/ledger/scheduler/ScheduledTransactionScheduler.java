package com.ledger.scheduler;

import com.ledger.entity.ScheduledTransaction;
import com.ledger.service.ScheduledTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTransactionScheduler {

    private final ScheduledTransactionService scheduledService;

    @Scheduled(fixedDelay = 60000)
    public void executeDueTransactions() {
        List<ScheduledTransaction> dueList = scheduledService.listDueTransactions();
        if (dueList.isEmpty()) return;

        log.info("检测到{}条待执行定时交易", dueList.size());
        for (ScheduledTransaction st : dueList) {
            try {
                scheduledService.executeScheduledTransaction(st.getId());
            } catch (Exception e) {
                log.error("定时交易执行失败: id={}", st.getId(), e);
            }
        }
    }
}
