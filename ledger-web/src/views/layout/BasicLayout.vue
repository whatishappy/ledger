<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, DataLine, Expand, Fold, Moon, Notebook, Sunny, Wallet } from '@element-plus/icons-vue'
import * as userApi from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import ModifyPasswordDialog from '@/components/ModifyPasswordDialog.vue'
import ConfirmPasswordDialog from '@/components/ConfirmPasswordDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const modifyVisible = ref(false)
const deleteVisible = ref(false)

const menuItems = [
  { path: '/dashboard', title: '仪表盘', icon: DataLine },
  { path: '/account', title: '账目管理', icon: Notebook },
  { path: '/budget', title: '预算管理', icon: Wallet },
]

/** 顶栏面包屑：仪表盘 / 当前页 */
const breadcrumbs = computed(() => {
  const items = [{ title: '首页', path: '/dashboard' }]
  if (route.path !== '/dashboard' && route.meta.title) {
    items.push({ title: route.meta.title as string, path: '' })
  }
  return items
})

const displayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录')

/* 窄屏自动折叠侧栏，避免固定 220px 挤压内容区导致横向溢出 */
const COLLAPSE_BREAKPOINT = 768

function applyResponsiveCollapse() {
  if (window.innerWidth < COLLAPSE_BREAKPOINT && !appStore.sidebarCollapse) {
    appStore.toggleSidebar()
  }
}

onMounted(async () => {
  applyResponsiveCollapse()
  window.addEventListener('resize', applyResponsiveCollapse)
  // 刷新后 Pinia 仅有 accessToken，补拉用户信息
  if (!userStore.userInfo) {
    try {
      const info = await userApi.getInfo()
      userStore.setUserInfo(info)
    } catch {
      /* 401 已由拦截器处理跳转 */
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', applyResponsiveCollapse)
})

function handleCommand(command: string) {
  if (command === 'change-password') modifyVisible.value = true
  else if (command === 'delete-account') deleteVisible.value = true
  else if (command === 'logout') handleLogout()
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await userApi.logout()
  } catch {
    /* 忽略登出接口失败，本地态照常清理 */
  }
  userStore.reset()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside :width="appStore.sidebarCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo" @click="router.push('/dashboard')">
        <span class="logo-mark">
          <el-icon :size="20"><Wallet /></el-icon>
        </span>
        <transition name="fade">
          <span v-if="!appStore.sidebarCollapse" class="logo-text">云记账本</span>
        </transition>
      </div>
      <el-menu
        :default-active="appStore.activeMenu"
        :collapse="appStore.sidebarCollapse"
        :collapse-transition="false"
        router
        class="layout-menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="layout-body">
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <button
            type="button"
            class="collapse-btn"
            :aria-label="appStore.sidebarCollapse ? '展开侧边栏' : '折叠侧边栏'"
            :title="appStore.sidebarCollapse ? '展开侧边栏' : '折叠侧边栏'"
            @click="appStore.toggleSidebar()"
          >
            <el-icon :size="20">
              <Expand v-if="appStore.sidebarCollapse" />
              <Fold v-else />
            </el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="(item, idx) in breadcrumbs"
              :key="idx"
              :to="item.path || undefined"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 明暗切换 -->
          <div class="theme-switch" title="切换明暗主题">
            <el-icon class="theme-icon"><Sunny /></el-icon>
            <el-switch
              :model-value="appStore.isDark"
              @change="appStore.toggleDark()"
              style="--el-switch-on-color: #3b82f6"
            />
            <el-icon class="theme-icon"><Moon /></el-icon>
          </div>

          <!-- 用户头像下拉 -->
          <el-dropdown trigger="hover" @command="handleCommand">
            <div class="user-entry clickable">
              <el-avatar :size="32" class="user-avatar">
                {{ displayName.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ displayName }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="change-password">修改密码</el-dropdown-item>
                <el-dropdown-item command="delete-account" divided>注销账号</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>

      <el-footer class="layout-footer" height="40px">
        个人云端记账本 · 让每一笔收支都有迹可循
      </el-footer>
    </el-container>
  </el-container>

  <ModifyPasswordDialog v-model="modifyVisible" />
  <ConfirmPasswordDialog v-model="deleteVisible" />
</template>

<style scoped lang="scss">
.layout {
  height: 100vh;
  background: var(--el-bg-color);
}

/* ---------- 侧栏 ---------- */
.layout-aside {
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color-overlay);
  border-right: 1px solid var(--el-border-color-light);
  transition: width 0.25s ease;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  flex-shrink: 0;
  cursor: pointer;
  user-select: none;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #1e40af, #3b82f6);
  color: #fff;
  flex-shrink: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 1px;
  color: var(--el-text-color-primary);
  white-space: nowrap;
}

.layout-menu {
  flex: 1;
  border-right: none;
  --el-menu-item-height: 48px;
}

/* ---------- 顶栏 ---------- */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: var(--el-bg-color-overlay);
  border-bottom: 1px solid var(--el-border-color-light);
  backdrop-filter: blur(4px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}
.collapse-btn:hover {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
}
.collapse-btn:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.theme-switch {
  display: flex;
  align-items: center;
  gap: 8px;
}
.theme-icon {
  font-size: 16px;
  color: var(--el-text-color-secondary);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  outline: none;
}
.user-avatar {
  background: linear-gradient(135deg, #1e40af, #3b82f6);
  color: #fff;
  font-weight: 600;
}
.user-name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* ---------- 内容区 ---------- */
.layout-main {
  padding: 20px;
  overflow-y: auto;
  background: var(--el-bg-color);
}

.layout-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color-overlay);
  border-top: 1px solid var(--el-border-color-light);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 窄屏：隐藏次要信息，避免顶栏拥挤 */
@media (max-width: 768px) {
  .layout-header {
    padding: 0 12px;
  }
  .header-left .el-breadcrumb {
    display: none;
  }
  .user-name,
  .arrow {
    display: none;
  }
}
</style>
