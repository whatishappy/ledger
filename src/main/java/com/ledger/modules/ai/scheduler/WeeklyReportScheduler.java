package com.ledger.modules.ai.scheduler;

import com.ledger.modules.ai.service.LedgerAiTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final LedgerAiTools ledgerAiTools;

    @Scheduled(cron = "0 0 9 ? * MON")
    public void generateWeeklyReports() {
        log.info("开始生成周度财务报告: date={}", LocalDate.now());
        try {
            String report = ledgerAiTools.generateWeeklyReport(null);
            log.info("周度报告生成完成: {}", report.length() > 200 ? report.substring(0, 200) + "..." : report);
        } catch (Exception e) {
            log.error("周度报告生成失败: {}", e.getMessage(), e);
        }
    }
}
