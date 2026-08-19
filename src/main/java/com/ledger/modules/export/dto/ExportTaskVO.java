package com.ledger.modules.export.dto;

import com.ledger.modules.export.entity.ExportTask;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出任务状态VO（E-02 查询结果，按详细设计 §8.3）
 */
@Data
@AllArgsConstructor
public class ExportTaskVO {

    private String taskId;

    /**
     * 状态描述：待处理/处理中/已完成/失败/已过期
     */
    private String status;

    /**
     * 状态码
     */
    private Integer statusCode;

    /**
     * 文件下载地址（完成时才有）
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 导出行数
     */
    private Integer rowCount;

    /**
     * 失败原因（失败时才有）
     */
    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;

    public static ExportTaskVO from(ExportTask task) {
        return new ExportTaskVO(
                task.getTaskId(),
                com.ledger.common.enums.ExportTaskStatusEnum.fromValue(task.getStatus()).getDescription(),
                task.getStatus(),
                task.getFileUrl(),
                task.getFileSize(),
                task.getRowCount(),
                task.getErrorMsg(),
                task.getCreateTime(),
                task.getExpireTime()
        );
    }
}
