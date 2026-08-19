/**
 * 认证模块 — /api/auth/*
 * 刷新接口返回 `Result<String>`，data 为新的 accessToken（纯字符串）。
 * 依赖 HttpOnly Cookie 中的 refresh_token，无需请求体。
 */
import request from '@/utils/request'

/** 刷新 Access Token → 返回新 token 字符串 */
export function refresh() {
  return request.post<string, string>('/auth/refresh')
}
