package com.ledger.service.calendar.impl;

import com.ledger.common.cache.CacheService;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.dto.calendar.CalendarDayAggregate;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.service.calendar.CalendarService;
import com.ledger.vo.calendar.CalendarDayVO;
import com.ledger.vo.calendar.CalendarHeatmapVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日历热力图服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    private final AccountMapper accountMapper;
    private final CacheService cacheService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public CalendarHeatmapVO getCalendarHeatmap(Long userId, String month) {
        if (month == null || month.isBlank()) {
            month = YearMonth.now().toString();
        }
        validateMonth(month);

        CalendarHeatmapVO cached = cacheService.getCalendar(userId, month, CalendarHeatmapVO.class);
        if (cached != null) {
            log.debug("日历热力图缓存命中: userId={}, month={}", userId, month);
            return cached;
        }

        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<CalendarDayAggregate> aggregates = accountMapper.getDailyAggregates(userId, startDate, endDate);
        Map<LocalDate, CalendarDayAggregate> aggregateMap = new HashMap<>();
        if (aggregates != null) {
            for (CalendarDayAggregate agg : aggregates) {
                aggregateMap.put(agg.getDate(), agg);
            }
        }

        int maxDailyExpenseInt = 0;
        BigDecimal maxDailyExpense = BigDecimal.ZERO;
        List<CalendarDayVO> days = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int totalCount = 0;

        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            CalendarDayAggregate agg = aggregateMap.get(cursor);
            BigDecimal income = (agg != null && agg.getIncome() != null) ? agg.getIncome() : BigDecimal.ZERO;
            BigDecimal expense = (agg != null && agg.getExpense() != null) ? agg.getExpense() : BigDecimal.ZERO;
            int count = (agg != null && agg.getCount() != null) ? agg.getCount() : 0;

            if (expense.compareTo(maxDailyExpense) > 0) {
                maxDailyExpense = expense;
            }

            totalIncome = totalIncome.add(income);
            totalExpense = totalExpense.add(expense);
            totalCount += count;

            days.add(new CalendarDayVO(
                    cursor.format(DATE_FORMATTER),
                    income,
                    expense,
                    count,
                    0
            ));

            cursor = cursor.plusDays(1);
        }

        maxDailyExpenseInt = maxDailyExpense.setScale(0, RoundingMode.DOWN).intValue();
        BigDecimal maxBD = maxDailyExpense;
        for (CalendarDayVO day : days) {
            day.setLevel(calculateLevel(day.getExpense(), maxBD));
        }

        CalendarHeatmapVO result = new CalendarHeatmapVO(
                month,
                totalIncome,
                totalExpense,
                totalCount,
                maxDailyExpenseInt,
                days
        );

        cacheService.setCalendar(userId, month, result);

        log.info("日历热力图数据聚合完成: userId={}, month={}, income={}, expense={}",
                userId, month, totalIncome, totalExpense);
        return result;
    }

    /**
     * 计算单日热力等级
     * 0档（无）：expense=0 → level=0
     * 1档（低）：>0 且 <= max*0.25 → level=1
     * 2档（中）：>max*0.25 且 <= max*0.5 → level=2
     * 3档（高）：>max*0.5 且 <= max*0.75 → level=3
     * 4档（极高）：>max*0.75 → level=4
     * 如果expense>0且maxDailyExpense=0（异常边缘），统一level=1
     */
    private int calculateLevel(BigDecimal expense, BigDecimal maxDailyExpense) {
        if (expense == null || expense.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        if (maxDailyExpense == null || maxDailyExpense.compareTo(BigDecimal.ZERO) == 0) {
            return 1;
        }
        BigDecimal q1 = maxDailyExpense.multiply(new BigDecimal("0.25"));
        BigDecimal q2 = maxDailyExpense.multiply(new BigDecimal("0.5"));
        BigDecimal q3 = maxDailyExpense.multiply(new BigDecimal("0.75"));

        if (expense.compareTo(q1) <= 0) {
            return 1;
        } else if (expense.compareTo(q2) <= 0) {
            return 2;
        } else if (expense.compareTo(q3) <= 0) {
            return 3;
        } else {
            return 4;
        }
    }

    private void validateMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "月份格式必须为YYYY-MM");
        }
        try {
            YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "月份格式无效");
        }
    }
}
