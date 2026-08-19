/**
 * 路由配置 — createWebHistory（History 模式，部署时 Nginx try_files 兜底）
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' },
  },
  {
    path: '/',
    component: () => import('@/views/layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '仪表盘', icon: 'DataLine' },
      },
      {
        path: 'account',
        name: 'Account',
        component: () => import('@/views/AccountView.vue'),
        meta: { title: '账目管理', icon: 'Notebook' },
      },
      {
        path: 'budget',
        name: 'Budget',
        component: () => import('@/views/BudgetView.vue'),
        meta: { title: '预算管理', icon: 'Wallet' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
