/**
 * 用户模块 — /api/user/*
 * 契约：注册/登录/登出/获取信息/注销（注销密码走 query 参数）
 */
import request from '@/utils/request'
import type { LoginRequest, LoginVO, RegisterRequest, UserInfoVO } from '@/types/api'

export function register(data: RegisterRequest) {
  return request.post<unknown, void>('/user/register', data)
}

export function login(data: LoginRequest) {
  return request.post<LoginVO, LoginVO>('/user/login', data)
}

export function logout() {
  return request.post<unknown, void>('/user/logout')
}

export function getInfo() {
  return request.get<UserInfoVO, UserInfoVO>('/user/info')
}

/** 注销账号：后端用 @RequestParam password（query 参数） */
export function deleteAccount(password: string) {
  return request.delete<unknown, void>('/user/delete', { params: { password } })
}

/**
 * 修改密码（前端设计文档 §4.5）
 * ⚠️ 跨端依赖：后端暂无 PUT /api/user/password 实现，联调前需后端补充
 * （校验旧密码→1006，成功自增 token_version 使当前会话失效）
 */
export function changePassword(oldPassword: string, newPassword: string) {
  return request.put<unknown, void>('/user/password', { oldPassword, newPassword })
}
