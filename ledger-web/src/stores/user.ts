/**
 * 用户状态
 *
 * 安全约束（前端设计文档 §4.1）：
 * - accessToken 纯内存存储，**不持久化**（防 XSS 持久窃取），页面刷新后经 /auth/refresh 无感恢复
 * - refreshToken 由后端 HttpOnly Cookie 管理，前端不读写
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfoVO } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref('')
  const userInfo = ref<UserInfoVO | null>(null)
  const loading = ref(false)

  function setAccessToken(token: string) {
    accessToken.value = token
  }

  function setUserInfo(info: UserInfoVO | null) {
    userInfo.value = info
  }

  /** 清空登录态（登出/改密/注销/401 失效时调用） */
  function reset() {
    accessToken.value = ''
    userInfo.value = null
  }

  function isLoggedIn(): boolean {
    return !!accessToken.value
  }

  return { accessToken, userInfo, loading, setAccessToken, setUserInfo, reset, isLoggedIn }
})
