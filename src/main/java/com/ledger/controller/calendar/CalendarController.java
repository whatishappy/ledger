package com.ledger.controller.calendar;

import com.ledger.common.context.UserContext;
import com.ledger.common.result.Result;
import com.ledger.service.calendar.CalendarService;
import com.ledger.vo.calendar.CalendarHeatmapVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日历模块控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
@Tag(name = "日历模块", description = "交易日历热力图数据（含缓存管理）")
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 获取指定月份的日历热力图数据（CA-01）
     */
    @GetMapping("/heatmap")
    @Operation(summary = "获取日历热力图数据")
    public Result<CalendarHeatmapVO> getHeatmap(@RequestParam(required = false) String month) {
        Long userId = UserContext.requireUserId();
        CalendarHeatmapVO heatmap = calendarService.getCalendarHeatmap(userId, month);
        return Result.success(heatmap);
    }
}
