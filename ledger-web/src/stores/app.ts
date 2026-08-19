/**
 * 应用 UI 状态 — 明暗主题、侧栏折叠、激活菜单
 * isDark / sidebarCollapse 持久化到 localStorage（UI 偏好），与登录态无关。
 */
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    isDark: false,
    sidebarCollapse: false,
    activeMenu: '/dashboard',
  }),
  actions: {
    /** 将 isDark 应用到 <html class="dark">（联动 Element Plus 暗色变量） */
    applyTheme() {
      document.documentElement.classList.toggle('dark', this.isDark)
    },
    toggleDark() {
      this.isDark = !this.isDark
      this.applyTheme()
    },
    toggleSidebar() {
      this.sidebarCollapse = !this.sidebarCollapse
    },
    setActiveMenu(path: string) {
      this.activeMenu = path
    },
  },
  persist: {
    pick: ['isDark', 'sidebarCollapse'],
  },
})
