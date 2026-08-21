/**
 * 定时交易模块 — /api/scheduled
 * 周期性自动记账规则管理。
 *
 * 说明：同 image 模块，后端通过 @RequestHeader("X-User-Id") 读取用户，
 * 前端依赖 JWT Authorization 头，不主动发送 X-User-Id。
 */
import request from '@/utils/request'
import type { ScheduledTransaction } from '@/types/api'

/** 创建定时交易 */
export function createScheduled(data: ScheduledTransaction) {
  return request.post<ScheduledTransaction, ScheduledTransaction>(
    '/scheduled',
    data,
  )
}

/** 查询当前用户的定时交易列表 */
export function listScheduled() {
  return request.get<ScheduledTransaction[], ScheduledTransaction[]>(
    '/scheduled',
  )
}

/** 更新定时交易 */
export function updateScheduled(id: number, data: ScheduledTransaction) {
  return request.put<ScheduledTransaction, ScheduledTransaction>(
    `/scheduled/${id}`,
    data,
  )
}

/** 删除定时交易 */
export function deleteScheduled(id: number) {
  return request.delete<unknown, void>(`/scheduled/${id}`)
}

/** 启用/停用定时交易（enabled: 0-停用 1-启用） */
export function toggleScheduled(id: number, enabled: number) {
  return request.patch<ScheduledTransaction, ScheduledTransaction>(
    `/scheduled/${id}/toggle`,
    undefined,
    { params: { enabled } },
  )
}

/** 手动触发执行定时交易 */
export function executeScheduled(id: number) {
  return request.post<unknown, void>(`/scheduled/${id}/execute`)
}
