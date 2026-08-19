package com.ledger.dto.calendar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单日账目聚合数据DTO（Mapper查询结果映射）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayAggregate {

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 当日收入合计
     */
    private BigDecimal income;

    /**
     * 当日支出合计
     */
    private BigDecimal expense;

    /**
     * 当日笔数
     */
    private Integer count;
}
