/**
 * 统计模块 — /api/statistics/*
 * month 缺省时后端默认当前月。
 *
 * V2.1 新增：同比 /yoy、环比 /mom、多月趋势 /trend（以后端 StatisticsController 为准，
 * 非设计文档 §14.5.1 的 /dashboard/trend?type=&compare=）。
 */
import request from '@/utils/request'
import type { DashboardVO, TrendCompareVO } from '@/types/api'

export function getDashboard(month?: string) {
  return request.get<DashboardVO, DashboardVO>('/statistics/dashboard', {
    params: month ? { month } : {},
  })
}

/** 同比对比（与去年同月） */
export function getYoY(month: string) {
  return request.get<TrendCompareVO, TrendCompareVO>('/statistics/yoy', {
    params: { month },
  })
}

/** 环比对比（与上一月） */
export function getMoM(month: string) {
  return request.get<TrendCompareVO, TrendCompareVO>('/statistics/mom', {
    params: { month },
  })
}

/** 多月趋势对比（返回区间内每月对比数据） */
export function getTrend(startMonth: string, endMonth: string) {
  return request.get<TrendCompareVO[], TrendCompareVO[]>('/statistics/trend', {
    params: { startMonth, endMonth },
  })
}
