import { describe, it, expect } from 'vitest'
import {
  currentMonth,
  formatLocalDate,
  formatYearMonth,
  getMonthRange,
  isFutureMonth,
  parseLocalDate,
  today,
} from '@/utils/date'

describe('date 工具', () => {
  it('formatLocalDate 输出 YYYY-MM-DD（补零）', () => {
    expect(formatLocalDate(new Date(2026, 7, 18))).toBe('2026-08-18')
    expect(formatLocalDate(new Date(2026, 0, 5))).toBe('2026-01-05')
  })

  it('parseLocalDate 解析本地时区日期', () => {
    const d = parseLocalDate('2026-08-18')
    expect(d.getFullYear()).toBe(2026)
    expect(d.getMonth()).toBe(7)
    expect(d.getDate()).toBe(18)
  })

  it('LocalDate 与 Date 互转 round-trip', () => {
    expect(formatLocalDate(parseLocalDate('2026-08-18'))).toBe('2026-08-18')
  })

  it('formatYearMonth 输出 YYYY-MM', () => {
    expect(formatYearMonth(new Date(2026, 7, 1))).toBe('2026-08')
  })

  it('getMonthRange 计算首末日（含闰年）', () => {
    expect(getMonthRange('2026-08')).toEqual({ start: '2026-08-01', end: '2026-08-31' })
    expect(getMonthRange('2026-02')).toEqual({ start: '2026-02-01', end: '2026-02-28' })
    expect(getMonthRange('2024-02')).toEqual({ start: '2024-02-01', end: '2024-02-29' })
  })

  it('today / currentMonth 格式正确', () => {
    expect(today()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect(currentMonth()).toMatch(/^\d{4}-\d{2}$/)
  })

  it('isFutureMonth 判断未来月份', () => {
    expect(isFutureMonth('2099-01')).toBe(true)
    expect(isFutureMonth('2020-01')).toBe(false)
  })
})
