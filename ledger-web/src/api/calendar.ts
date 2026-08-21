/**
 * 日历统计模块 — /api/v1/calendar（CA-01）
 * 交易日历热力图数据。month 缺省时后端默认当前月。
 */
import request from '@/utils/request'
import type { CalendarHeatmapVO } from '@/types/api'

/** CA-01 获取指定月份的日历热力图数据 */
export function getCalendarHeatmap(month?: string) {
  return request.get<CalendarHeatmapVO, CalendarHeatmapVO>(
    '/v1/calendar/heatmap',
    { params: month ? { month } : {} },
  )
}
