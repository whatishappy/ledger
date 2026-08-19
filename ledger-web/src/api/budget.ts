/**
 * 预算模块 — /api/budget/*
 * 契约：单条设定 { category, month, amountLimit }，重复设定返回 3001；
 * 查询返回含进度的 BudgetVO 数组。
 */
import request from '@/utils/request'
import type { BudgetAddRequest, BudgetVO } from '@/types/api'

export function addBudget(data: BudgetAddRequest) {
  return request.post<number, number>('/budget/add', data)
}

export function listBudgets(month: string) {
  return request.get<BudgetVO[], BudgetVO[]>('/budget/list', { params: { month } })
}
