package com.ledger.modules.statistics.service.impl;

import com.ledger.common.cache.CacheService;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.budget.dto.BudgetVO;
import com.ledger.modules.budget.service.IBudgetService;
import com.ledger.modules.statistics.dto.CategoryStatVO;
import com.ledger.modules.statistics.dto.DashboardVO;
import com.ledger.modules.statistics.dto.TrendVO;
import com.ledger.modules.statistics.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现（按详细设计 §7.3）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

    private final AccountMapper accountMapper;
    private final IBudgetService budgetService;
    private final CacheService cacheService;

    @Override
    public DashboardVO getDashboard(Long userId, String month) {
        // 默认当前月
        if (month == null || month.isBlank()) {
            month = YearMonth.now().toString();
        }
        // 步骤2：校验月份格式
        validateMonth(month);

        // 步骤3：查询Redis缓存，若命中直接返回
        DashboardVO cached = cacheService.getDashboard(userId, month, DashboardVO.class);
        if (cached != null) {
            log.debug("仪表盘缓存命中: userId={}, month={}", userId, month);
            return cached;
        }

        // 步骤4：计算月度起始日期和结束日期
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // 步骤5：查询该月收入总和（type=1）
        BigDecimal monthIncome = accountMapper.sumAmountByTypeAndDateRange(userId, 1, monthStart, monthEnd);
        if (monthIncome == null) {
            monthIncome = BigDecimal.ZERO;
        }

        // 步骤6：查询该月支出总和（type=0）
        BigDecimal monthExpense = accountMapper.sumAmountByTypeAndDateRange(userId, 0, monthStart, monthEnd);
        if (monthExpense == null) {
            monthExpense = BigDecimal.ZERO;
        }

        // 步骤7：计算结余 = 收入 - 支出
        BigDecimal balance = monthIncome.subtract(monthExpense);

        // 步骤8：查询支出分类占比（GROUP BY category）
        List<Map<String, Object>> categoryList = accountMapper.sumExpenseGroupByCategory(userId, monthStart, monthEnd);

        // 步骤9：计算每个分类占总支出的百分比
        List<CategoryStatVO> categoryStats = buildCategoryStats(categoryList, monthExpense);

        // 步骤10：查询近7日每日收支（GROUP BY DATE(account_date)）
        LocalDate trendStart = LocalDate.now().minusDays(6);
        LocalDate trendEnd = LocalDate.now();
        List<Map<String, Object>> trendList = accountMapper.sumAmountGroupByDate(userId, trendStart, trendEnd);
        List<TrendVO> trend = buildTrend(trendList, trendStart, trendEnd);

        // 步骤11：调用预算模块获取当前月份预算进度
        List<BudgetVO> budgetProgress = budgetService.listBudgetsWithProgress(userId, month);

        // 步骤12：组装Dashboard对象
        DashboardVO dashboard = new DashboardVO(
                month,
                monthIncome,
                monthExpense,
                balance,
                categoryStats,
                trend,
                budgetProgress
        );

        // 步骤13：写入Redis缓存，过期时间5分钟
        cacheService.setDashboard(userId, month, dashboard);

        // 步骤14：返回Dashboard数据
        log.info("仪表盘数据聚合完成: userId={}, month={}, income={}, expense={}",
                userId, month, monthIncome, monthExpense);
        return dashboard;
    }

    /**
     * 构建分类占比统计
     */
    private List<CategoryStatVO> buildCategoryStats(List<Map<String, Object>> categoryList, BigDecimal totalExpense) {
        if (categoryList == null || categoryList.isEmpty()) {
            return Collections.emptyList();
        }
        return categoryList.stream()
                .map(row -> {
                    String category = (String) row.get("category");
                    BigDecimal amount = toBigDecimal(row.get("amount"));
                    // 计算百分比 = 分类金额 / 总支出 × 100
                    BigDecimal percentage = BigDecimal.ZERO;
                    if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                        percentage = amount.multiply(new BigDecimal("100"))
                                .divide(totalExpense, 2, RoundingMode.HALF_UP);
                    }
                    return new CategoryStatVO(category, amount, percentage);
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建近7日趋势（按日期补全，无数据日期填0）
     */
    private List<TrendVO> buildTrend(List<Map<String, Object>> trendList, LocalDate start, LocalDate end) {
        // 按日期分组：收入和支出
        Map<LocalDate, BigDecimal[]> dateMap = new LinkedHashMap<>();
        // 初始化所有日期，确保无数据日期也展示
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            dateMap.put(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            cursor = cursor.plusDays(1);
        }

        // 填充查询数据
        if (trendList != null) {
            for (Map<String, Object> row : trendList) {
                Object dateObj = row.get("accountDate");
                LocalDate date = toDate(dateObj);
                if (date == null || !dateMap.containsKey(date)) {
                    continue;
                }
                Object typeObj = row.get("type");
                BigDecimal amount = toBigDecimal(row.get("amount"));
                BigDecimal[] arr = dateMap.get(date);
                // type=1 收入，type=0 支出
                int type = toInt(typeObj);
                if (type == 1) {
                    arr[0] = arr[0].add(amount);
                } else if (type == 0) {
                    arr[1] = arr[1].add(amount);
                }
            }
        }

        // 转为VO列表
        List<TrendVO> result = new ArrayList<>();
        dateMap.forEach((date, arr) -> result.add(new TrendVO(date, arr[0], arr[1])));
        return result;
    }

    /**
     * 校验月份格式
     */
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

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private int toInt(Object value) {
        if (value == null) {
            return -1;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? 1 : 0;
        }
        String s = value.toString();
        if ("true".equalsIgnoreCase(s)) return 1;
        if ("false".equalsIgnoreCase(s)) return 0;
        return Integer.parseInt(s);
    }

    private LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
