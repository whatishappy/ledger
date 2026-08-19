package com.ledger.modules.budget.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.budget.dto.BudgetAddRequest;
import com.ledger.modules.budget.dto.BudgetVO;
import com.ledger.modules.budget.service.IBudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预算控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
@Tag(name = "预算模块", description = "预算CRUD、预算进度查询（含N+1优化）")
public class BudgetController {

    private final IBudgetService budgetService;

    /**
     * 设定月度预算（B-01，按详细设计 §6.3）
     */
    @PostMapping("/add")
    @Operation(summary = "设定月度预算")
    public Result<Long> addBudget(@Valid @RequestBody BudgetAddRequest request) {
        Long userId = UserContext.requireUserId();
        Long budgetId = budgetService.addBudget(userId, request);
        return Result.success(budgetId);
    }

    /**
     * 查询预算列表（B-04，按详细设计 §6.4）
     * 含进度计算，N+1已优化
     */
    @GetMapping("/list")
    @Operation(summary = "查询预算列表（含进度）")
    public Result<List<BudgetVO>> listBudgets(@RequestParam String month) {
        Long userId = UserContext.requireUserId();
        List<BudgetVO> budgets = budgetService.listBudgetsWithProgress(userId, month);
        return Result.success(budgets);
    }
}
