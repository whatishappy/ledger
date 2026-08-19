package com.ledger.modules.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.modules.account.dto.AccountAddRequest;
import com.ledger.modules.account.dto.AccountPageRequest;
import com.ledger.modules.account.dto.AccountVO;
import com.ledger.modules.account.service.IAccountService;
import com.ledger.modules.budget.dto.BudgetVO;
import com.ledger.modules.budget.service.IBudgetService;
import com.ledger.modules.statistics.dto.CategoryStatVO;
import com.ledger.modules.statistics.dto.DashboardVO;
import com.ledger.modules.statistics.service.IDashboardService;
import com.ledger.modules.tag.service.TagService;
import com.ledger.modules.tag.vo.TagStatisticsVO;
import com.ledger.service.calendar.CalendarService;
import com.ledger.vo.calendar.CalendarHeatmapVO;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerAiTools {

    private final IAccountService accountService;
    private final IDashboardService dashboardService;
    private final IBudgetService budgetService;
    private final TagService tagService;
    private final CalendarService calendarService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Tool("natural_lang_bookkeeping")
    public String createAccountFromNL(Long userId, String date, String type, String category,
                                       BigDecimal amount, String remark) {
        try {
            AccountAddRequest request = new AccountAddRequest();
            request.setType(Integer.parseInt(type));
            request.setCategory(category);
            request.setAmount(amount);
            request.setAccountDate(LocalDate.parse(date, DATE_FORMATTER));
            request.setRemark(remark);
            Long accountId = accountService.addAccount(userId, request);
            return "记账成功，账目ID=" + accountId;
        } catch (Exception e) {
            log.warn("AI工具记账失败: userId={}, error={}", userId, e.getMessage());
            return "记账失败：" + e.getMessage();
        }
    }

    @Tool("query_transactions")
    public String queryTransactions(Long userId, LocalDate startDate, LocalDate endDate,
                                     String category, Integer limit) {
        try {
            AccountPageRequest request = new AccountPageRequest();
            request.setPageNum(1);
            request.setPageSize(limit != null ? limit : 20);
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setCategory(category);
            IPage<AccountVO> page = accountService.pageQuery(userId, request);
            List<AccountVO> records = page.getRecords();
            return toJson(records);
        } catch (Exception e) {
            log.warn("AI工具查询交易失败: userId={}, error={}", userId, e.getMessage());
            return "查询失败：" + e.getMessage();
        }
    }

    @Tool("get_dashboard_summary")
    public String getDashboardSummary(Long userId, String month) {
        try {
            if (month == null || month.isBlank()) {
                month = YearMonth.now().toString();
            }
            DashboardVO dashboard = dashboardService.getDashboard(userId, month);
            return toJson(dashboard);
        } catch (Exception e) {
            log.warn("AI工具获取仪表盘失败: userId={}, error={}", userId, e.getMessage());
            return "获取仪表盘数据失败：" + e.getMessage();
        }
    }

    @Tool("get_budget_status")
    public String getBudgetStatus(Long userId, String month) {
        try {
            if (month == null || month.isBlank()) {
                month = YearMonth.now().toString();
            }
            List<BudgetVO> budgets = budgetService.listBudgetsWithProgress(userId, month);
            return toJson(budgets);
        } catch (Exception e) {
            log.warn("AI工具获取预算失败: userId={}, error={}", userId, e.getMessage());
            return "获取预算数据失败：" + e.getMessage();
        }
    }

    @Tool("get_tag_statistics")
    public String getTagStatistics(Long userId, Integer year, Integer month) {
        try {
            if (year == null) year = LocalDate.now().getYear();
            if (month == null) month = LocalDate.now().getMonthValue();
            TagStatisticsVO stats = tagService.getTagStatistics(userId, year, month);
            return toJson(stats);
        } catch (Exception e) {
            log.warn("AI工具获取标签统计失败: userId={}, error={}", userId, e.getMessage());
            return "获取标签统计失败：" + e.getMessage();
        }
    }

    @Tool("get_calendar_heatmap")
    public String getCalendarHeatmap(Long userId, String month) {
        try {
            if (month == null || month.isBlank()) {
                month = YearMonth.now().toString();
            }
            CalendarHeatmapVO heatmap = calendarService.getCalendarHeatmap(userId, month);
            return toJson(heatmap);
        } catch (Exception e) {
            log.warn("AI工具获取日历热力图失败: userId={}, error={}", userId, e.getMessage());
            return "获取日历热力图失败：" + e.getMessage();
        }
    }

    @Tool("generate_saving_suggestions")
    public String generateSavingSuggestions(Long userId, String month) {
        try {
            if (month == null || month.isBlank()) {
                month = YearMonth.now().toString();
            }
            DashboardVO dashboard = dashboardService.getDashboard(userId, month);
            List<BudgetVO> budgets = budgetService.listBudgetsWithProgress(userId, month);
            List<String> suggestions = new ArrayList<>();

            BigDecimal monthExpense = dashboard.getMonthExpense();
            List<CategoryStatVO> categoryStats = dashboard.getCategoryStats();

            if (monthExpense.compareTo(BigDecimal.ZERO) > 0 && categoryStats != null) {
                for (CategoryStatVO cat : categoryStats) {
                    BigDecimal pct = cat.getPercentage();
                    String catName = cat.getCategory();
                    BigDecimal amount = cat.getAmount();

                    if (pct.compareTo(new BigDecimal("30")) >= 0) {
                        BigDecimal saveAmount = amount.multiply(new BigDecimal("0.1"))
                                .setScale(0, RoundingMode.HALF_UP);
                        suggestions.add(String.format(
                                "%s支出占总支出%s%%，可考虑适当控制；预计每月可省¥%s",
                                catName, pct, saveAmount));
                    }

                    if ("餐饮".equals(catName) && pct.compareTo(new BigDecimal("20")) >= 0) {
                        suggestions.add("餐饮支出占比较高，可考虑每周减少1次外卖，尝试自己做饭；预计每月可省¥200-400");
                    }

                    if ("娱乐".equals(catName) && pct.compareTo(new BigDecimal("15")) >= 0) {
                        suggestions.add("娱乐支出占比超过15%，可考虑减少非必要订阅或购物；每月可省¥100-300");
                    }

                    if ("购物".equals(catName) && pct.compareTo(new BigDecimal("25")) >= 0) {
                        suggestions.add("购物支出占比较高，建议购物前列清单，避免冲动消费；每月可省¥300-500");
                    }
                }
            }

            for (BudgetVO budget : budgets) {
                if (Boolean.TRUE.equals(budget.getIsOverBudget())) {
                    BigDecimal overSpent = budget.getSpent().subtract(budget.getAmountLimit())
                            .setScale(0, RoundingMode.HALF_UP);
                    suggestions.add(String.format(
                            "%s分类已超支¥%s（进度%s%%），建议下月严格控制该分类支出",
                            budget.getCategory(), overSpent, budget.getProgress()));
                }
            }

            if (suggestions.isEmpty()) {
                suggestions.add("本月支出结构健康，继续保持良好的消费习惯！");
                suggestions.add("建议每月固定收入的20%作为强制储蓄，可设置自动转账");
                suggestions.add("可考虑记录3个月消费明细后再做精细化预算调整");
            }

            if (suggestions.size() > 5) {
                suggestions = suggestions.subList(0, 5);
            }

            return String.join("\n", suggestions);
        } catch (Exception e) {
            log.warn("AI工具生成省钱建议失败: userId={}, error={}", userId, e.getMessage());
            return "生成省钱建议失败：" + e.getMessage();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"JSON序列化失败\"}";
        }
    }
}
