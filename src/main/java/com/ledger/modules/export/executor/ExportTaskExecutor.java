package com.ledger.modules.export.executor;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ledger.common.enums.ExportTaskStatusEnum;
import com.ledger.modules.account.entity.Account;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.export.config.ExportProperties;
import com.ledger.modules.export.dto.AccountExcelRow;
import com.ledger.modules.export.entity.ExportTask;
import com.ledger.modules.export.mapper.ExportTaskMapper;
import com.ledger.modules.monitor.service.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 异步导出任务执行器（按详细设计 §8.3）
 * - 线程池异步执行（core=2, max=5, queue=100）
 * - 分布式锁防止重复执行
 * - 全局并发上限5（Semaphore）
 * - 单用户限流：Redis 计数器，5分钟1次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskExecutor {

    private final AccountMapper accountMapper;
    private final ExportTaskMapper exportTaskMapper;
    private final ExportProperties exportProperties;
    private final RedissonClient redissonClient;
    private final BusinessMetricsService metricsService;

    /**
     * 全局并发上限5
     */
    private final Semaphore globalSemaphore = new Semaphore(5);

    /**
     * 单用户导出限流Key前缀
     */
    private static final String USER_RATE_LIMIT_KEY = "export:rate:user:%d";
    private static final long USER_RATE_LIMIT_TTL = 5 * 60 * 1000L; // 5分钟

    /**
     * 检查单用户导出频率（5分钟1次）
     * @return true 允许导出；false 频率超限
     */
    public boolean tryAcquireUserRateLimit(Long userId) {
        try {
            String key = String.format(USER_RATE_LIMIT_KEY, userId);
            RBucket<String> bucket = redissonClient.getBucket(key);
            if (bucket.isExists()) {
                return false;
            }
            bucket.set("1", USER_RATE_LIMIT_TTL, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            log.warn("检查用户导出限流失败: userId={}", userId, e);
            return true;
        }
    }

    /**
     * 异步执行导出任务
     */
    @Async("exportTaskExecutorPool")
    public void executeExportTask(ExportTask task, Integer type, String category,
                                   java.time.LocalDate startDate, java.time.LocalDate endDate) {
        String taskId = task.getTaskId();
        Long userId = task.getUserId();
        log.info("开始执行异步导出任务: taskId={}, userId={}", taskId, userId);

        // 获取分布式锁，防止重复执行
        String lockKey = "export:lock:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        boolean semaphoreAcquired = false;
        ExcelWriter excelWriter = null;
        FileOutputStream fos = null;

        try {
            locked = lock.tryLock(10, 30, TimeUnit.MINUTES);
            if (!locked) {
                log.warn("获取分布式锁失败，任务可能正在执行: taskId={}", taskId);
                updateTaskFailed(task.getId(), "任务正在执行中，请勿重复提交");
                return;
            }

            // 获取全局信号量
            semaphoreAcquired = globalSemaphore.tryAcquire(1, TimeUnit.MINUTES);
            if (!semaphoreAcquired) {
                log.warn("获取全局信号量失败，导出并发已满: taskId={}", taskId);
                updateTaskFailed(task.getId(), "当前导出任务较多，请稍后重试");
                return;
            }

            // 更新任务状态为处理中
            updateTaskStatus(task.getId(), ExportTaskStatusEnum.PROCESSING.getValue(), null);

            // 创建导出文件
            String fileName = "export_" + userId + "_" + taskId + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            String filePath = exportProperties.getFilePath();
            File dir = new File(filePath);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new RuntimeException("创建导出目录失败: " + filePath);
            }
            File file = new File(dir, fileName);
            fos = new FileOutputStream(file);
            excelWriter = EasyExcel.write(fos, AccountExcelRow.class).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("账目数据").build();

            // 分页查询并写入Excel
            int pageNum = 1;
            int pageSize = 5000;
            int totalRows = 0;
            while (true) {
                Page<Account> page = new Page<>(pageNum, pageSize);
                LambdaQueryWrapper<Account> wrapper = buildQueryWrapper(userId, type, category, startDate, endDate);
                Page<Account> result = accountMapper.selectPage(page, wrapper);
                if (result.getRecords().isEmpty()) {
                    break;
                }
                List<AccountExcelRow> rows = new ArrayList<>(result.getRecords().size());
                for (Account account : result.getRecords()) {
                    rows.add(new AccountExcelRow(
                            account.getId(),
                            account.getType() == 1 ? "收入" : "支出",
                            account.getCategory(),
                            account.getAmount(),
                            account.getAccountDate(),
                            account.getRemark(),
                            account.getCreateTime()
                    ));
                }
                excelWriter.write(rows, writeSheet);
                totalRows += rows.size();
                log.debug("导出任务写入数据: taskId={}, pageNum={}, rows={}, total={}",
                        taskId, pageNum, rows.size(), totalRows);

                if (!result.hasNext()) {
                    break;
                }
                pageNum++;
            }
            excelWriter.finish();
            excelWriter = null;
            fos.close();
            fos = null;

            // 更新任务为已完成
            ExportTask update = new ExportTask();
            update.setId(task.getId());
            update.setStatus(ExportTaskStatusEnum.COMPLETED.getValue());
            update.setFileUrl(file.getAbsolutePath());
            update.setFileSize(file.length());
            update.setRowCount(totalRows);
            exportTaskMapper.updateById(update);

            metricsService.recordExportSuccess();
            log.info("异步导出任务完成: taskId={}, userId={}, rows={}, fileSize={}",
                    taskId, userId, totalRows, file.length());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateTaskFailed(task.getId(), "任务被中断");
            metricsService.recordExportFail();
            log.error("导出任务被中断: taskId={}", taskId, e);
        } catch (Exception e) {
            updateTaskFailed(task.getId(), "导出失败: " + e.getMessage());
            metricsService.recordExportFail();
            log.error("导出任务执行失败: taskId={}", taskId, e);
        } finally {
            if (excelWriter != null) {
                try { excelWriter.finish(); } catch (Exception ignored) {}
            }
            if (fos != null) {
                try { fos.close(); } catch (Exception ignored) {}
            }
            if (semaphoreAcquired) {
                globalSemaphore.release();
            }
            if (locked) {
                try { lock.unlock(); } catch (Exception ignored) {}
            }
        }
    }

    private LambdaQueryWrapper<Account> buildQueryWrapper(Long userId, Integer type, String category,
                                                           java.time.LocalDate startDate, java.time.LocalDate endDate) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId);
        if (type != null) {
            wrapper.eq(Account::getType, type);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Account::getCategory, category);
        }
        if (startDate != null) {
            wrapper.ge(Account::getAccountDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(Account::getAccountDate, endDate);
        }
        wrapper.orderByDesc(Account::getAccountDate)
               .orderByDesc(Account::getCreateTime);
        return wrapper;
    }

    private void updateTaskStatus(Long id, int status, String errorMsg) {
        ExportTask update = new ExportTask();
        update.setId(id);
        update.setStatus(status);
        if (errorMsg != null) {
            update.setErrorMsg(errorMsg);
        }
        exportTaskMapper.updateById(update);
    }

    private void updateTaskFailed(Long id, String errorMsg) {
        updateTaskStatus(id, ExportTaskStatusEnum.FAILED.getValue(), errorMsg);
    }
}
