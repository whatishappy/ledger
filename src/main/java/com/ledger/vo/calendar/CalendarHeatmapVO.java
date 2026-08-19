package com.ledger.vo.calendar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 日历热力图聚合数据VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarHeatmapVO {

    /**
     * 月份 yyyy-MM
     */
    private String month;

    /**
     * 月收入汇总
     */
    private BigDecimal totalIncome;

    /**
     * 月支出汇总
     */
    private BigDecimal totalExpense;

    /**
     * 月总笔数
     */
    private Integer totalCount;

    /**
     * 最高单日支出（用于分level边界）
     */
    private Integer maxDailyExpense;

    /**
     * 当月每天数组，按日期排序
     */
    private List<CalendarDayVO> days;
}
