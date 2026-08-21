package com.ledger.service.calendar;

import com.ledger.vo.calendar.CalendarHeatmapVO;

/**
 * 日历热力图服务接口
 */
public interface CalendarService {

    /**
     * 获取指定月份的日历热力图数据（CA-01）
     *
     * @param userId 用户ID
     * @param month  月份 yyyy-MM
     * @return 日历热力图数据
     */
    CalendarHeatmapVO getCalendarHeatmap(Long userId, String month);
}
