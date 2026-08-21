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
import com.ledger.modules.ai.dto.AiOcrResultDTO;
import com.ledger.modules.ai.service.AiOcrService;
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
    private final AiOcrService aiOcrService;
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

    @Tool("receipt_ocr")
    public String receiptOcr(Long userId, String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isBlank()) {
                return "{\"error\":\"图片URL不能为空\"}";
            }
            AiOcrResultDTO result = aiOcrService.ocrByImageUrl(userId, imageUrl);
            return toJson(result);
        } catch (Exception e) {
            log.warn("AI工具OCR识别失败: userId={}, error={}", userId, e.getMessage());
            return "{\"error\":\"OCR识别失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("predict_expense")
    public String predictExpense(Long userId, String month) {
        try {
            YearMonth targetMonth;
            if (month == null || month.isBlank()) {
                targetMonth = YearMonth.now().plusMonths(1);
            } else {
                targetMonth = YearMonth.parse(month);
            }

            BigDecimal totalExpense = BigDecimal.ZERO;
            int monthCount = 0;
            BigDecimal[] monthlyExpenses = new BigDecimal[6];

            for (int i = 0; i < 6; i++) {
                YearMonth m = targetMonth.minusMonths(i + 1);
                try {
                    DashboardVO dashboard = dashboardService.getDashboard(userId, m.toString());
                    BigDecimal expense = dashboard.getMonthExpense();
                    if (expense != null && expense.compareTo(BigDecimal.ZERO) > 0) {
                        monthlyExpenses[i] = expense;
                        totalExpense = totalExpense.add(expense);
                        monthCount++;
                    } else {
                        monthlyExpenses[i] = BigDecimal.ZERO;
                    }
                } catch (Exception e) {
                    monthlyExpenses[i] = BigDecimal.ZERO;
                }
            }

            if (monthCount == 0) {
                return "{\"prediction\":\"数据不足，无法进行支出预测。请先记录至少一个月的支出数据。\"}";
            }

            BigDecimal avgExpense = totalExpense.divide(new BigDecimal(monthCount), 2, RoundingMode.HALF_UP);

            BigDecimal trendRate = BigDecimal.ZERO;
            if (monthCount >= 2) {
                BigDecimal recent = BigDecimal.ZERO;
                BigDecimal older = BigDecimal.ZERO;
                int recentCount = 0;
                int olderCount = 0;
                for (int i = 0; i < 3 && i < monthlyExpenses.length; i++) {
                    if (monthlyExpenses[i] != null && monthlyExpenses[i].compareTo(BigDecimal.ZERO) > 0) {
                        recent = recent.add(monthlyExpenses[i]);
                        recentCount++;
                    }
                }
                for (int i = 3; i < 6 && i < monthlyExpenses.length; i++) {
                    if (monthlyExpenses[i] != null && monthlyExpenses[i].compareTo(BigDecimal.ZERO) > 0) {
                        older = older.add(monthlyExpenses[i]);
                        olderCount++;
                    }
                }
                if (recentCount > 0 && olderCount > 0) {
                    BigDecimal recentAvg = recent.divide(new BigDecimal(recentCount), 4, RoundingMode.HALF_UP);
                    BigDecimal olderAvg = older.divide(new BigDecimal(olderCount), 4, RoundingMode.HALF_UP);
                    if (olderAvg.compareTo(BigDecimal.ZERO) > 0) {
                        trendRate = recentAvg.subtract(olderAvg).divide(olderAvg, 4, RoundingMode.HALF_UP);
                    }
                }
            }

            BigDecimal predictedExpense = avgExpense.multiply(BigDecimal.ONE.add(trendRate))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal lowerBound = predictedExpense.multiply(new BigDecimal("0.8"))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal upperBound = predictedExpense.multiply(new BigDecimal("1.2"))
                    .setScale(2, RoundingMode.HALF_UP);

            StringBuilder result = new StringBuilder();
            result.append("{");
            result.append("\"targetMonth\":\"").append(targetMonth).append("\",");
            result.append("\"predictedExpense\":").append(predictedExpense).append(",");
            result.append("\"lowerBound\":").append(lowerBound).append(",");
            result.append("\"upperBound\":").append(upperBound).append(",");
            result.append("\"confidence\":\"中\",");
            result.append("\"avgMonthlyExpense\":").append(avgExpense).append(",");
            result.append("\"trendRate\":").append(trendRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)).append("%,");
            result.append("\"dataMonths\":").append(monthCount).append(",");

            String trendDesc;
            if (trendRate.compareTo(new BigDecimal("0.05")) > 0) {
                trendDesc = "近3个月支出呈上升趋势，建议关注大额支出";
            } else if (trendRate.compareTo(new BigDecimal("-0.05")) < 0) {
                trendDesc = "近3个月支出呈下降趋势，消费控制良好";
            } else {
                trendDesc = "近3个月支出保持稳定";
            }
            result.append("\"trendDescription\":\"").append(trendDesc).append("\"");
            result.append("}");

            return result.toString();
        } catch (Exception e) {
            log.warn("AI工具支出预测失败: userId={}, error={}", userId, e.getMessage());
            return "{\"error\":\"支出预测失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("recommend_budget")
    public String recommendBudget(Long userId, String month) {
        try {
            YearMonth targetMonth;
            if (month == null || month.isBlank()) {
                targetMonth = YearMonth.now();
            } else {
                targetMonth = YearMonth.parse(month);
            }

            DashboardVO currentDashboard = dashboardService.getDashboard(userId, targetMonth.toString());
            List<CategoryStatVO> currentStats = currentDashboard.getCategoryStats();

            if (currentStats == null || currentStats.isEmpty()) {
                return "{\"recommendation\":\"暂无消费数据，无法生成预算推荐。请先记录一些账目。\"}";
            }

            List<CategoryBudgetRecommendation> recommendations = new ArrayList<>();
            for (CategoryStatVO cat : currentStats) {
                BigDecimal currentAmount = cat.getAmount();
                BigDecimal avgMonthly = currentAmount;

                BigDecimal recommendedBudget = avgMonthly.multiply(new BigDecimal("1.1"))
                        .setScale(2, RoundingMode.HALF_UP);

                BigDecimal minBudget = avgMonthly.multiply(new BigDecimal("0.9"))
                        .setScale(2, RoundingMode.HALF_UP);

                String advice;
                if (cat.getPercentage().compareTo(new BigDecimal("40")) > 0) {
                    advice = "占比过高，建议设定严格预算并控制消费频率";
                    recommendedBudget = avgMonthly.multiply(new BigDecimal("0.9"))
                            .setScale(2, RoundingMode.HALF_UP);
                } else if (cat.getPercentage().compareTo(new BigDecimal("20")) > 0) {
                    advice = "占比中等，建议设定合理预算并关注消费趋势";
                } else {
                    advice = "占比较低，当前预算设置合理，可根据个人需求调整";
                    recommendedBudget = avgMonthly.multiply(new BigDecimal("1.05"))
                            .setScale(2, RoundingMode.HALF_UP);
                }

                CategoryBudgetRecommendation rec = new CategoryBudgetRecommendation();
                rec.setCategory(cat.getCategory());
                rec.setCurrentAmount(currentAmount);
                rec.setRecommendedBudget(recommendedBudget);
                rec.setMinSuggestedBudget(minBudget);
                rec.setAdvice(advice);
                recommendations.add(rec);
            }

            StringBuilder result = new StringBuilder();
            result.append("{\"month\":\"").append(targetMonth).append("\",");
            result.append("\"recommendations\":[");
            for (int i = 0; i < recommendations.size(); i++) {
                CategoryBudgetRecommendation r = recommendations.get(i);
                if (i > 0) result.append(",");
                result.append("{\"category\":\"").append(r.getCategory()).append("\",");
                result.append("\"currentAmount\":").append(r.getCurrentAmount()).append(",");
                result.append("\"recommendedBudget\":").append(r.getRecommendedBudget()).append(",");
                result.append("\"minSuggestedBudget\":").append(r.getMinSuggestedBudget()).append(",");
                result.append("\"advice\":\"").append(r.getAdvice()).append("\"}");
            }
            result.append("]}");
            return result.toString();
        } catch (Exception e) {
            log.warn("AI工具预算推荐失败: userId={}, error={}", userId, e.getMessage());
            return "{\"error\":\"预算推荐失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("generate_weekly_report")
    public String generateWeeklyReport(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(6);
            LocalDate weekEnd = today;

            StringBuilder report = new StringBuilder();
            report.append("{");
            report.append("\"reportPeriod\":\"").append(weekStart).append("至").append(weekEnd).append("\",");

            DashboardVO currentWeekDashboard = dashboardService.getDashboard(userId, YearMonth.from(today).toString());
            BigDecimal weekExpense = currentWeekDashboard.getMonthExpense();
            BigDecimal weekIncome = currentWeekDashboard.getMonthIncome();

            report.append("\"weekExpense\":").append(weekExpense != null ? weekExpense : BigDecimal.ZERO).append(",");
            report.append("\"weekIncome\":").append(weekIncome != null ? weekIncome : BigDecimal.ZERO).append(",");

            List<CategoryStatVO> stats = currentWeekDashboard.getCategoryStats();
            if (stats != null && !stats.isEmpty()) {
                report.append("\"topCategories\":[");
                int limit = Math.min(5, stats.size());
                for (int i = 0; i < limit; i++) {
                    if (i > 0) report.append(",");
                    CategoryStatVO cat = stats.get(i);
                    report.append("{\"category\":\"").append(cat.getCategory()).append("\",");
                    report.append("\"amount\":").append(cat.getAmount()).append(",");
                    report.append("\"percentage\":").append(cat.getPercentage()).append("}");
                }
                report.append("],");
            }

            List<String> suggestions = new ArrayList<>();
            if (weekExpense != null && weekExpense.compareTo(new BigDecimal("1000")) > 0) {
                suggestions.add("本周支出超过1000元，建议审视大额消费");
            }
            if (stats != null) {
                for (CategoryStatVO cat : stats) {
                    if ("餐饮".equals(cat.getCategory()) && cat.getPercentage().compareTo(new BigDecimal("30")) > 0) {
                        suggestions.add("餐饮支出占比过高，建议尝试在家做饭减少外卖");
                    }
                    if ("购物".equals(cat.getCategory()) && cat.getPercentage().compareTo(new BigDecimal("25")) > 0) {
                        suggestions.add("购物支出占比偏高，建议购物前列清单避免冲动消费");
                    }
                }
            }
            if (suggestions.isEmpty()) {
                suggestions.add("本周消费结构健康，继续保持！");
            }
            report.append("\"suggestions\":").append(toJson(suggestions));
            report.append("}");

            return report.toString();
        } catch (Exception e) {
            log.warn("AI工具周报生成失败: userId={}, error={}", userId, e.getMessage());
            return "{\"error\":\"周报生成失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("generate_saving_suggestions_enhanced")
    public String generateSavingSuggestionsEnhanced(Long userId, String month) {
        try {
            if (month == null || month.isBlank()) {
                month = YearMonth.now().toString();
            }
            DashboardVO dashboard = dashboardService.getDashboard(userId, month);
            List<BudgetVO> budgets = budgetService.listBudgetsWithProgress(userId, month);
            List<String> suggestions = new ArrayList<>();

            BigDecimal monthExpense = dashboard.getMonthExpense();
            List<CategoryStatVO> categoryStats = dashboard.getCategoryStats();

            BigDecimal avgExpense = BigDecimal.ZERO;
            int validMonths = 0;
            for (int i = 1; i <= 3; i++) {
                try {
                    YearMonth prevMonth = YearMonth.parse(month).minusMonths(i);
                    DashboardVO prev = dashboardService.getDashboard(userId, prevMonth.toString());
                    if (prev.getMonthExpense() != null && prev.getMonthExpense().compareTo(BigDecimal.ZERO) > 0) {
                        avgExpense = avgExpense.add(prev.getMonthExpense());
                        validMonths++;
                    }
                } catch (Exception ignored) {}
            }
            if (validMonths > 0) {
                avgExpense = avgExpense.divide(new BigDecimal(validMonths), 2, RoundingMode.HALF_UP);
            }

            if (monthExpense != null && avgExpense.compareTo(BigDecimal.ZERO) > 0
                    && monthExpense.compareTo(avgExpense.multiply(new BigDecimal("1.3"))) > 0) {
                suggestions.add("⚠️ 本月支出较近3月平均值高出30%以上，建议立即审视消费结构");
            }

            if (monthExpense != null && monthExpense.compareTo(BigDecimal.ZERO) > 0 && categoryStats != null) {
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

            if (suggestions.size() > 8) {
                suggestions = suggestions.subList(0, 8);
            }

            return String.join("\n", suggestions);
        } catch (Exception e) {
            log.warn("AI工具增强省钱建议失败: userId={}, error={}", userId, e.getMessage());
            return "生成省钱建议失败：" + e.getMessage();
        }
    }

    private static class CategoryBudgetRecommendation {
        private String category;
        private BigDecimal currentAmount;
        private BigDecimal recommendedBudget;
        private BigDecimal minSuggestedBudget;
        private String advice;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getCurrentAmount() { return currentAmount; }
        public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }
        public BigDecimal getRecommendedBudget() { return recommendedBudget; }
        public void setRecommendedBudget(BigDecimal recommendedBudget) { this.recommendedBudget = recommendedBudget; }
        public BigDecimal getMinSuggestedBudget() { return minSuggestedBudget; }
        public void setMinSuggestedBudget(BigDecimal minSuggestedBudget) { this.minSuggestedBudget = minSuggestedBudget; }
        public String getAdvice() { return advice; }
        public void setAdvice(String advice) { this.advice = advice; }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"JSON序列化失败\"}";
        }
    }
}
