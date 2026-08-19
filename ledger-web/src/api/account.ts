/**
 * 账目模块 — /api/account/*
 * 分页响应为 MyBatis-Plus IPage：records/total/size/current
 */
import request from '@/utils/request'
import type {
  AccountAddRequest,
  AccountPageQuery,
  AccountUpdateRequest,
  AccountVO,
  PageResult,
} from '@/types/api'

export function addAccount(data: AccountAddRequest) {
  return request.post<number, number>('/account/add', data)
}

export function pageAccounts(data: AccountPageQuery) {
  return request.post<PageResult<AccountVO>, PageResult<AccountVO>>('/account/page', data)
}

export function updateAccount(data: AccountUpdateRequest) {
  return request.put<unknown, void>('/account/update', data)
}

export function deleteAccount(id: number) {
  return request.delete<unknown, void>(`/account/delete/${id}`)
}
