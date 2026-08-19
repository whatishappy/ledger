import { describe, it, expect } from 'vitest'
import { formatMoney, formatSignedMoney, getCategoryIcon, typeLabel } from '@/utils/format'

describe('format 工具', () => {
  it('formatMoney 千分位 + 两位小数', () => {
    expect(formatMoney(1234567.8)).toBe('1,234,567.80')
    expect(formatMoney(0)).toBe('0.00')
    expect(formatMoney(null)).toBe('0.00')
    expect(formatMoney('88.5')).toBe('88.50')
    expect(formatMoney(100)).toBe('100.00')
  })

  it('formatSignedMoney 按类型带正负号', () => {
    expect(formatSignedMoney(100, 0)).toBe('-100.00')
    expect(formatSignedMoney(100, 1)).toBe('+100.00')
  })

  it('typeLabel 类型文案', () => {
    expect(typeLabel(0)).toBe('支出')
    expect(typeLabel(1)).toBe('收入')
  })

  it('getCategoryIcon 已知分类返回图标，未知回退默认', () => {
    expect(getCategoryIcon('餐饮')).toBeDefined()
    expect(getCategoryIcon('工资')).toBeDefined()
    expect(getCategoryIcon('不存在的分类')).toBeDefined()
  })
})
