package com.ledger.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.entity.ScheduledTransaction;
import com.ledger.service.ScheduledTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduled")
@RequiredArgsConstructor
@Tag(name = "定时交易", description = "周期性自动记账管理")
public class ScheduledTransactionController {

    private final ScheduledTransactionService scheduledService;

    @PostMapping
    @Operation(summary = "创建定时交易")
    public Result<ScheduledTransaction> create(
            @RequestBody ScheduledTransaction transaction) {
        Long userId = UserContext.requireUserId();
        transaction.setUserId(userId);
        return Result.success(scheduledService.create(transaction));
    }

    @GetMapping
    @Operation(summary = "查询当前用户的定时交易列表")
    public Result<List<ScheduledTransaction>> list() {
        Long userId = UserContext.requireUserId();
        return Result.success(scheduledService.listByUserId(userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新定时交易")
    public Result<ScheduledTransaction> update(
            @PathVariable Long id,
            @RequestBody ScheduledTransaction transaction) {
        return Result.success(scheduledService.update(id, transaction));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除定时交易")
    public Result<Void> delete(@PathVariable Long id) {
        scheduledService.delete(id);
        return Result.success();
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "启用/停用定时交易")
    public Result<ScheduledTransaction> toggle(
            @PathVariable Long id,
            @RequestParam Integer enabled) {
        return Result.success(scheduledService.toggleEnabled(id, enabled));
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "手动触发执行定时交易")
    public Result<Void> execute(@PathVariable Long id) {
        scheduledService.executeScheduledTransaction(id);
        return Result.success();
    }
}
