package com.ledger.modules.export.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.account.entity.Account;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.export.config.ExportProperties;
import com.ledger.modules.export.dto.AccountExcelRow;
import com.ledger.modules.export.dto.ExportRequest;
import com.ledger.modules.export.dto.ExportTaskVO;
import com.ledger.modules.export.entity.ExportTask;
import com.ledger.modules.export.executor.ExportTaskExecutor;
import com.ledger.modules.export.mapper.ExportTaskMapper;
import com.ledger.modules.export.service.IExportService;
import com.ledger.modules.monitor.service.BusinessMetricsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 导出服务实现（按详细设计 §8.3）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements IExportService {

    private final AccountMapper accountMapper;
    private final ExportTaskMapper exportTaskMapper;
    private final ExportTaskExecutor exportTaskExecutor;
    private final ExportProperties exportProperties;
    private final BusinessMetricsService metricsService;

    @Override
    public String exportExcel(Long userId, ExportRequest request, HttpServletResponse response) {
        // 步骤2：校验导出频率是否超限（单用户5分钟1次）
        if (!exportTaskExecutor.tryAcquireUserRateLimit(userId)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "导出频率超限，请5分钟后重试");
        }

        // 步骤3：查询符合条件的数据总量
        long totalCount = accountMapper.countByConditions(
                userId, request.getType(), request.getCategory(),
                request.getStartDate(), request.getEndDate());

        // 校验最大导出行数
        if (totalCount > exportProperties.getMaxRows()) {
            throw new BusinessException(ResultCode.EXPORT_TOO_MANY_ROWS);
        }

        // 步骤4：判断数据量是否≤10000行
        if (totalCount <= exportProperties.getSyncThreshold()) {
            // 步骤5：同步导出
            syncExport(userId, request, response);
            return null;
        }

        // 步骤6：异步导出，创建导出任务记录
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ExportTask task = new ExportTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setStatus(0); // 待处理
        task.setExpireTime(LocalDateTime.now().plusDays(exportProperties.getRetainDays()));
        exportTaskMapper.insert(task);

        // 步骤7：提交任务到异步线程池执行
        exportTaskExecutor.executeExportTask(task, request.getType(), request.getCategory(),
                request.getStartDate(), request.getEndDate());

        // 步骤8~9：返回taskId给客户端
        log.info("异步导出任务已创建: taskId={}, userId={}, totalCount={}", taskId, userId, totalCount);
        return taskId;
    }

    @Override
    public ExportTaskVO getExportStatus(Long userId, String taskId) {
        ExportTask task = getByTaskIdAndUserId(taskId, userId);
        return ExportTaskVO.from(task);
    }

    @Override
    public Resource downloadExportFile(Long userId, String taskId) {
        ExportTask task = getByTaskIdAndUserId(taskId, userId);
        if (task.getStatus() != 2) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "任务未完成，无法下载");
        }
        if (StringUtils.isEmpty(task.getFileUrl())) {
            throw new BusinessException(ResultCode.EXPORT_TASK_NOT_FOUND);
        }
        File file = new File(task.getFileUrl());
        if (!file.exists()) {
            throw new BusinessException(ResultCode.EXPORT_TASK_NOT_FOUND, "文件已被清理");
        }
        return new FileSystemResource(file);
    }

    /**
     * 同步导出（≤10000行）
     */
    private void syncExport(Long userId, ExportRequest request, HttpServletResponse response) {
        try {
            // 设置响应头
            String fileName = "账目导出_" + LocalDateTime.now().getNano() + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=utf-8''" + encodedFileName);

            // 分页查询数据并写入Excel
            int pageNum = 1;
            int pageSize = 5000;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(baos, AccountExcelRow.class).build();
            com.alibaba.excel.write.metadata.WriteSheet writeSheet = EasyExcel.writerSheet("账目数据").build();

            while (true) {
                Page<Account> page = new Page<>(pageNum, pageSize);
                LambdaQueryWrapper<Account> wrapper = buildQueryWrapper(userId, request);
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
                if (!result.hasNext()) {
                    break;
                }
                pageNum++;
            }
            excelWriter.finish();
            response.getOutputStream().write(baos.toByteArray());
            response.getOutputStream().flush();
            metricsService.recordExportSuccess();
            log.info("同步导出完成: userId={}", userId);
        } catch (Exception e) {
            log.error("同步导出失败: userId={}", userId, e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "导出失败: " + e.getMessage());
        }
    }

    private LambdaQueryWrapper<Account> buildQueryWrapper(Long userId, ExportRequest request) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId);
        if (request.getType() != null) {
            wrapper.eq(Account::getType, request.getType());
        }
        if (StringUtils.hasText(request.getCategory())) {
            wrapper.eq(Account::getCategory, request.getCategory());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(Account::getAccountDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(Account::getAccountDate, request.getEndDate());
        }
        wrapper.orderByDesc(Account::getAccountDate)
               .orderByDesc(Account::getCreateTime);
        return wrapper;
    }

    private ExportTask getByTaskIdAndUserId(String taskId, Long userId) {
        LambdaQueryWrapper<ExportTask> wrapper = new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getTaskId, taskId)
                .eq(ExportTask::getUserId, userId);
        ExportTask task = exportTaskMapper.selectOne(wrapper);
        if (task == null) {
            throw new BusinessException(ResultCode.EXPORT_TASK_NOT_FOUND);
        }
        return task;
    }
}
