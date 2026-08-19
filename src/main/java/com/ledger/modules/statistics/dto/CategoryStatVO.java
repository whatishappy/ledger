package com.ledger.modules.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支出分类占比VO（饼图数据，按详细设计 §7.3）
 */
@Data
@AllArgsConstructor
public class CategoryStatVO {

    /**
     * 支出分类
     */
    private String category;

    /**
     * 该分类支出金额
     */
    private BigDecimal amount;

    /**
     * 占总支出百分比（保留2位小数）
     */
    private BigDecimal percentage;
}
