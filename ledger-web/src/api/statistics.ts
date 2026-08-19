/**
 * 统计模块 — /api/statistics/*
 * month 缺省时后端默认当前月。
 */
import request from '@/utils/request'
import type { DashboardVO } from '@/types/api'

export function getDashboard(month?: string) {
  return request.get<DashboardVO, DashboardVO>('/statistics/dashboard', {
    params: month ? { month } : {},
  })
}
