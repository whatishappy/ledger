package com.ledger.vo.calendar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 日历单日数据VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayVO {

    /**
     * 日期 yyyy-MM-dd
     */
    private String date;

    /**
     * 当日收入合计
     */
    private BigDecimal income;

    /**
     * 当日支出合计（正数展示）
     */
    private BigDecimal expense;

    /**
     * 当日笔数
     */
    private Integer count;

    /**
     * 热力等级 0-4（0=无 1=低 2=中 3=高 4=极高）
     */
    private Integer level;
}
