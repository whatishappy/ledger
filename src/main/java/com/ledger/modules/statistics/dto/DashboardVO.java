package com.ledger.modules.statistics.dto;

import com.ledger.modules.budget.dto.BudgetVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘聚合数据VO（S-01 查询结果，按详细设计 §7.3）
 */
@Data
@AllArgsConstructor
public class DashboardVO {

    /**
     * 月份（YYYY-MM）
     */
    private String month;

    /**
     * 月度总收入
     */
    private BigDecimal monthIncome;

    /**
     * 月度总支出
     */
    private BigDecimal monthExpense;

    /**
     * 结余（收入 - 支出）
     */
    private BigDecimal balance;

    /**
     * 支出分类占比（饼图数据）
     */
    private List<CategoryStatVO> categoryStats;

    /**
     * 近7日收支趋势（折线图数据）
     */
    private List<TrendVO> trend;

    /**
     * 预算进度（调用预算模块获取）
     */
    private List<BudgetVO> budgetProgress;
}
