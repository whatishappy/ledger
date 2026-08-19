package com.ledger.modules.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 预算进度VO（B-04 查询结果，按详细设计 §6.4）
 */
@Data
@AllArgsConstructor
public class BudgetVO {

    /**
     * 预算ID
     */
    private Long id;

    /**
     * 支出分类
     */
    private String category;

    /**
     * 预算上限金额
     */
    private BigDecimal amountLimit;

    /**
     * 已消费金额（该月该分类所有支出账目金额之和）
     */
    private BigDecimal spent;

    /**
     * 进度百分比（已消费金额 / 预算上限 × 100，保留2位小数）
     */
    private BigDecimal progress;

    /**
     * 是否超支（进度超过100%）
     */
    private Boolean isOverBudget;
}
