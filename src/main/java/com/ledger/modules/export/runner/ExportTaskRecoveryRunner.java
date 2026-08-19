package com.ledger.modules.export.runner;

import com.ledger.common.enums.ExportTaskStatusEnum;
import com.ledger.modules.export.entity.ExportTask;
import com.ledger.modules.export.mapper.ExportTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出任务启动恢复机制（按详细设计 §10.2.4）
 * 应用启动时扫描 status=1 且 update_time < NOW() - 30min 的任务
 * 重置为待处理(0) 并标记为失败(3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskRecoveryRunner implements ApplicationRunner {

    private final ExportTaskMapper exportTaskMapper;

    @Override
    public void run(ApplicationArguments args) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<ExportTask> timeoutTasks = exportTaskMapper.findTimeoutProcessingTasks(threshold);
        if (timeoutTasks.isEmpty()) {
            log.info("启动恢复：无超时的处理中导出任务");
            return;
        }
        log.info("启动恢复：发现 {} 个超时的处理中导出任务，重置为失败", timeoutTasks.size());
        for (ExportTask task : timeoutTasks) {
            ExportTask update = new ExportTask();
            update.setId(task.getId());
            update.setStatus(ExportTaskStatusEnum.FAILED.getValue());
            update.setErrorMsg("服务重启时任务被中断");
            exportTaskMapper.updateById(update);
            log.warn("重置超时任务: taskId={}, userId={}", task.getTaskId(), task.getUserId());
        }
    }
}
