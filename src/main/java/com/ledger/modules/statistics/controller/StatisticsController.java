package com.ledger.modules.statistics.controller;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.modules.statistics.dto.DashboardVO;
import com.ledger.modules.statistics.service.IDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计模块", description = "仪表盘数据聚合（含缓存管理）")
public class StatisticsController {

    private final IDashboardService dashboardService;

    /**
     * 仪表盘数据聚合（S-01，按详细设计 §7.3）
     * 返回月度总览+分类占比+趋势+预算进度
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘数据")
    public Result<DashboardVO> getDashboard(@RequestParam(required = false) String month) {
        Long userId = UserContext.requireUserId();
        DashboardVO dashboard = dashboardService.getDashboard(userId, month);
        return Result.success(dashboard);
    }
}
