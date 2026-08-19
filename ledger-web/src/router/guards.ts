/**
 * 路由守卫
 *
 * 逻辑（前端设计文档 §4.3）：
 * 1. whiteList = ['/login', '/404'] 直接放行
 * 2. 已登录（accessToken 存在）→ 访问 /login 则跳 /dashboard，否则放行
 * 3. 无 accessToken（刷新/首次进入）→ 调 /auth/refresh 无感恢复
 *    - 成功 → 写入 Pinia → 放行
 *    - 失败 → 跳 /login?redirect=当前路径
 */
import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { refresh } from '@/api/auth'

const whiteList = ['/login', '/404']

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const userStore = useUserStore()
    const appStore = useAppStore()

    if (to.meta.title) {
      document.title = `${to.meta.title as string} · 个人云端记账本`
    }

    // 白名单（无需登录）
    if (whiteList.includes(to.path)) {
      // 已登录访问登录页 → 送回仪表盘
      if (to.path === '/login' && userStore.isLoggedIn()) {
        return { path: '/dashboard' }
      }
      return true
    }

    // 已有 accessToken
    if (userStore.isLoggedIn()) {
      appStore.setActiveMenu(to.path)
      return true
    }

    // 无 token：尝试无感刷新
    try {
      const token = await refresh()
      userStore.setAccessToken(token)
      appStore.setActiveMenu(to.path)
      return true
    } catch {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  })
}
