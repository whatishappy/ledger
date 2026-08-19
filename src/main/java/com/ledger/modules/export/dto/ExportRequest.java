package com.ledger.modules.export.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 导出请求（E-01，按详细设计 §8.3）
 */
@Data
public class ExportRequest {

    /**
     * 收支类型（可选）：0-支出，1-收入
     */
    private Integer type;

    /**
     * 收支分类（可选）
     */
    private String category;

    /**
     * 起始日期（可选）
     */
    private LocalDate startDate;

    /**
     * 结束日期（可选）
     */
    private LocalDate endDate;
}
