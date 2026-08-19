/**
 * 格式化工具 — 金额千分位、分类图标映射、收支类型
 */
import type { Component } from 'vue'
import {
  Bowl,
  Van,
  ShoppingCart,
  Wallet,
  Film,
  MoreFilled,
  TrendCharts,
} from '@element-plus/icons-vue'
import { AccountType, type CategoryName } from '@/types/business'

/** 分类 → 图标组件（SVG，禁用 emoji 作为图标） */
const CATEGORY_ICONS: Record<CategoryName, Component> = {
  餐饮: Bowl,
  交通: Van,
  购物: ShoppingCart,
  工资: Wallet,
  娱乐: Film,
  其他: MoreFilled,
}

export function getCategoryIcon(category: string): Component {
  return CATEGORY_ICONS[category as CategoryName] ?? TrendCharts
}

/** 金额 → 千分位字符串（保留 2 位小数，负数红由调用方控制） */
export function formatMoney(amount: number | string | null | undefined): string {
  const num = Number(amount ?? 0)
  if (Number.isNaN(num)) return '0.00'
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

/** 金额展示：支出带负号，收入带正号 */
export function formatSignedMoney(amount: number, type: number): string {
  const sign = type === AccountType.EXPENSE ? '-' : '+'
  return `${sign}${formatMoney(amount)}`
}

/** 收支类型 → 文案 */
export function typeLabel(type: number): string {
  return type === AccountType.EXPENSE ? '支出' : '收入'
}
