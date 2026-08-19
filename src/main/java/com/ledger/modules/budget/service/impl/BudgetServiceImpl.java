package com.ledger.modules.budget.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ledger.common.cache.CacheService;
import com.ledger.common.enums.AccountCategoryEnum;
import com.ledger.common.exception.BusinessException;
import com.ledger.common.result.ResultCode;
import com.ledger.modules.account.mapper.AccountMapper;
import com.ledger.modules.budget.dto.BudgetAddRequest;
import com.ledger.modules.budget.dto.BudgetVO;
import com.ledger.modules.budget.entity.Budget;
import com.ledger.modules.budget.event.BudgetChangeEvent;
import com.ledger.modules.budget.mapper.BudgetMapper;
import com.ledger.modules.budget.service.IBudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预算服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements IBudgetService {

    private final BudgetMapper budgetMapper;
    private final AccountMapper accountMapper;
    private final CacheService cacheService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addBudget(Long userId, BudgetAddRequest request) {
        // 步骤2：校验分类、月份、金额合法性
        validateBudgetRequest(request);

        // 步骤3：查询是否存在相同用户+分类+月份的预算记录
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getCategory, request.getCategory())
                .eq(Budget::getMonth, request.getMonth());
        Long existingCount = budgetMapper.selectCount(wrapper);
        // 步骤4：若存在，终止流程，返回错误码3001
        if (existingCount > 0) {
            throw new BusinessException(ResultCode.BUDGET_ALREADY_EXISTS);
        }

        // 步骤5：构建预算实体
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategory(request.getCategory());
        budget.setMonth(request.getMonth());
        budget.setAmountLimit(request.getAmountLimit());

        // 步骤6：保存预算记录
        budgetMapper.insert(budget);

        // 步骤7：发布预算变更事件（清除对应月份缓存）
        eventPublisher.publishEvent(new BudgetChangeEvent(userId, request.getMonth()));

        log.info("设定预算成功: userId={}, budgetId={}, category={}, month={}",
                userId, budget.getId(), request.getCategory(), request.getMonth());
        return budget.getId();
    }

    @Override
    public List<BudgetVO> listBudgetsWithProgress(Long userId, String month) {
        // 步骤2：校验月份格式（YYYY-MM），且不晚于当前月份
        validateMonth(month);

        // 优先查缓存
        List<BudgetVO> cached = cacheService.getBudget(userId, month, List.class);
        if (cached != null) {
            log.debug("预算列表缓存命中: userId={}, month={}", userId, month);
            return cached;
        }

        // 步骤3：查询该用户该月的所有预算记录
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)
                .eq(Budget::getMonth, month);
        List<Budget> budgets = budgetMapper.selectList(wrapper);

        // 步骤4：若无预算，返回空列表
        if (budgets.isEmpty()) {
            return Collections.emptyList();
        }

        // 步骤5：计算该月月初和月末日期
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 步骤6：一次性查询该月所有支出分类的汇总金额（GROUP BY category）
        List<Map<String, Object>> spentList = accountMapper.sumSpentGroupByCategory(userId, startDate, endDate);

        // 步骤7：将查询结果转为Map（分类 → 已消费总额）
        Map<String, BigDecimal> spentMap = spentList.stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("category"),
                        row -> toBigDecimal(row.get("spent"))
                ));

        // 步骤8~9：遍历预算列表，从Map中获取每个分类的已消费金额，计算进度
        List<BudgetVO> result = budgets.stream()
                .map(budget -> {
                    BigDecimal spent = spentMap.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
                    // 进度 = 已消费金额 / 预算上限 × 100%
                    BigDecimal progress = spent.multiply(new BigDecimal("100"))
                            .divide(budget.getAmountLimit(), 2, RoundingMode.HALF_UP);
                    boolean overBudget = progress.compareTo(new BigDecimal("100")) > 0;
                    return new BudgetVO(
                            budget.getId(),
                            budget.getCategory(),
                            budget.getAmountLimit(),
                            spent,
                            progress,
                            overBudget
                    );
                })
                .collect(Collectors.toList());

        // 步骤10：写入缓存（10分钟）
        cacheService.setBudget(userId, month, result);

        return result;
    }

    /**
     * 校验预算请求参数
     */
    private void validateBudgetRequest(BudgetAddRequest request) {
        // ① 仅支出类分类可设定预算（收入类不允许）
        AccountCategoryEnum categoryEnum = AccountCategoryEnum.getByValue(request.getCategory());
        if (categoryEnum == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "收支分类无效");
        }
        if (!categoryEnum.isExpense()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "仅支出分类可设定预算");
        }
        // ③ 预算金额必须大于0（已在 DTO 注解校验，此处兜底）
        if (request.getAmountLimit() == null || request.getAmountLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "预算金额必须大于0");
        }
        // ④ 月份不能晚于当前月份
        validateMonth(request.getMonth());
    }

    /**
     * 校验月份格式和范围
     */
    private void validateMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "月份格式必须为YYYY-MM");
        }
        try {
            YearMonth requestMonth = YearMonth.parse(month);
            YearMonth currentMonth = YearMonth.now();
            if (requestMonth.isAfter(currentMonth)) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "月份不能晚于当前月份");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_FAILED, "月份格式无效");
        }
    }

    /**
     * 安全转换 BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }
}
