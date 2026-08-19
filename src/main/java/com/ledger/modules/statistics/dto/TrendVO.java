package com.ledger.modules.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 近7日收支趋势VO（折线图数据，按详细设计 §7.3）
 */
@Data
@AllArgsConstructor
public class TrendVO {

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 当日收入
     */
    private BigDecimal income;

    /**
     * 当日支出
     */
    private BigDecimal expense;
}
