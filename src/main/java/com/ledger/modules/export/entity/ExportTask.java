package com.ledger.modules.export.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 导出任务实体（对应 export_task 表，详细设计 §10.2.4）
 */
@Data
@TableName("export_task")
public class ExportTask implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一标识（客户端查询凭证）
     */
    private String taskId;

    /**
     * 发起用户ID
     */
    private Long userId;

    /**
     * 任务状态：0-待处理，1-处理中，2-已完成，3-失败，4-已过期
     */
    private Integer status;

    /**
     * 文件下载地址（完成时填充）
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
     * 失败原因（任务失败时填充）
     */
    private String errorMsg;

    /**
     * 文件过期时间（默认创建时间+7天）
     */
    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
