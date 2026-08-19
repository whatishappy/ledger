package com.ledger.modules.export.task;

import com.ledger.common.enums.ExportTaskStatusEnum;
import com.ledger.modules.export.entity.ExportTask;
import com.ledger.modules.export.mapper.ExportTaskMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出文件定时清理任务（按详细设计 §10.5）
 * 每天 03:00 执行：
 * 1. 标记过期任务（expire_time < NOW）
 * 2. 删除7天前的任务记录和文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportFileCleanupTask {

    private final ExportTaskMapper exportTaskMapper;

    /**
     * 每天 03:00 执行清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        log.info("开始执行导出文件清理任务");
        try {
            // 1. 标记过期任务（expire_time < NOW）
            List<ExportTask> expiredTasks = exportTaskMapper.findExpiredTasks();
            for (ExportTask task : expiredTasks) {
                ExportTask update = new ExportTask();
                update.setId(task.getId());
                update.setStatus(ExportTaskStatusEnum.EXPIRED.getValue());
                exportTaskMapper.updateById(update);
                // 删除过期文件
                if (task.getFileUrl() != null) {
                    deleteFile(task.getFileUrl());
                }
            }
            log.info("标记过期任务完成: {} 个", expiredTasks.size());

            // 2. 删除7天前的任务记录和文件
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            List<ExportTask> oldTasks = exportTaskMapper.findOldTasks(threshold);
            for (ExportTask task : oldTasks) {
                if (task.getFileUrl() != null) {
                    deleteFile(task.getFileUrl());
                }
                exportTaskMapper.deleteById(task.getId());
            }
            log.info("删除7天前任务记录完成: {} 个", oldTasks.size());
        } catch (Exception e) {
            log.error("导出文件清理任务执行失败", e);
        }
    }

    private void deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    log.debug("删除文件成功: {}", filePath);
                }
            }
        } catch (Exception e) {
            log.warn("删除文件失败: {}", filePath, e);
        }
    }
}
