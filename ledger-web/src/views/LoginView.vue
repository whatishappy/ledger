<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Lock, User, UserFilled, Wallet } from '@element-plus/icons-vue'
import * as userApi from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const activeTab = ref<'login' | 'register'>('login')
const loginLoading = ref(false)
const registerLoading = ref(false)

/* ---------- 登录表单 ---------- */
const loginFormRef = ref<FormInstance>()
const loginForm = reactive({
  username: '',
  password: '',
  remember: true,
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持字母、数字、下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loginLoading.value = true
  try {
    const data = await userApi.login({
      username: loginForm.username,
      password: loginForm.password,
    })
    userStore.setAccessToken(data.accessToken)
    userStore.setUserInfo(data.userInfo)
    // 记忆品牌偏好：登录即进入对应主题下已保存的暗色状态
    appStore.applyTheme()
    ElMessage.success('登录成功，欢迎回来')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } finally {
    loginLoading.value = false
  }
}

/* ---------- 注册表单 ---------- */
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持字母、数字、下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  registerLoading.value = true
  try {
    await userApi.register({
      username: registerForm.username,
      password: registerForm.password,
    })
    ElMessage.success('注册成功，请登录')
    // 注册成功后切到登录页并预填用户名
    loginForm.username = registerForm.username
    loginForm.password = ''
    activeTab.value = 'login'
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌面板：玻璃拟态（仅此处允许） -->
    <aside class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo">
          <el-icon :size="30"><Wallet /></el-icon>
          <span class="brand-name">云记账本</span>
        </div>
        <h1 class="brand-title">把每一笔收支<br />都理得清清楚楚</h1>
        <p class="brand-subtitle">
          简单、可信赖的个人财务管理。<br />云端存储，随时查看，安心记账。
        </p>
        <ul class="brand-points">
          <li>收入 · 支出 · 结余一目了然</li>
          <li>月度预算自动追踪与预警</li>
          <li>数据云端同步，多端访问</li>
        </ul>
      </div>
    </aside>

    <!-- 右侧登录/注册面板 -->
    <main class="login-panel">
      <!-- 返回介绍页（grill-me 共识：介绍页 ↔ 登录页双向闭环） -->
      <a class="back-home" href="/" aria-label="返回首页">← 返回首页</a>
      <div class="form-card">
        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              size="large"
              label-position="top"
              @keyup.enter="handleLogin"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="用户名"
                  :prefix-icon="User"
                  clearable
                  autocomplete="username"
                  name="username"
                  aria-label="用户名"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="密码"
                  :prefix-icon="Lock"
                  show-password
                  autocomplete="current-password"
                  name="password"
                  aria-label="密码"
                />
              </el-form-item>
              <el-form-item class="remember-row">
                <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
                <span class="remember-hint">登录状态最长保持 7 天</span>
              </el-form-item>
              <el-button
                type="primary"
                class="submit-btn"
                :loading="loginLoading"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              size="large"
              label-position="top"
              @keyup.enter="handleRegister"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="registerForm.username"
                  placeholder="用户名（3-50 位字母、数字、下划线）"
                  :prefix-icon="UserFilled"
                  clearable
                  autocomplete="username"
                  name="username"
                  aria-label="用户名"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="密码（6-100 位）"
                  :prefix-icon="Lock"
                  show-password
                  autocomplete="new-password"
                  name="new-password"
                  aria-label="密码"
                />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="确认密码"
                  :prefix-icon="Lock"
                  show-password
                  autocomplete="new-password"
                  name="confirm-password"
                  aria-label="确认密码"
                />
              </el-form-item>
              <el-button
                type="primary"
                class="submit-btn"
                :loading="registerLoading"
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--el-bg-color);
}

/* ---------- 品牌面板 ---------- */
.brand-panel {
  position: relative;
  flex: 1 1 55%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #1e3a8a 0%, #1e40af 45%, #3b82f6 100%);
  color: #fff;
}

/* 装饰性渐变光斑，增强层次 */
.brand-panel::before,
.brand-panel::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.45;
  pointer-events: none;
}
.brand-panel::before {
  width: 360px;
  height: 360px;
  top: -80px;
  right: -60px;
  background: rgba(255, 255, 255, 0.14);
}
.brand-panel::after {
  width: 420px;
  height: 420px;
  bottom: -120px;
  left: -80px;
  background: rgba(3, 15, 60, 0.5);
}

.brand-inner {
  position: relative;
  z-index: 1;
  max-width: 460px;
  padding: 48px;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-title {
  margin: 36px 0 16px;
  font-size: 40px;
  line-height: 1.35;
  font-weight: 700;
}

.brand-subtitle {
  margin: 0 0 32px;
  font-size: 16px;
  line-height: 1.8;
  opacity: 0.85;
}

.brand-points {
  list-style: none;
  margin: 0;
  padding: 0;
  font-size: 15px;
  line-height: 2.2;
  opacity: 0.92;
}

.brand-points li::before {
  content: '✓';
  margin-right: 10px;
  font-weight: 700;
}

/* ---------- 右侧表单 ---------- */
.login-panel {
  position: relative;
  flex: 1 1 45%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: var(--el-bg-color);
}

/* 返回介绍页链接 */
.back-home {
  position: absolute;
  top: 24px;
  right: 32px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  text-decoration: none;
  transition: color 0.2s;
}

.back-home:hover {
  color: var(--color-primary, #1e40af);
}

.form-card {
  width: 100%;
  max-width: 400px;
  padding: 8px 8px 16px;
}

.remember-row {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.remember-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: var(--color-primary, #1e40af);
}

/* 大留白登录场景 base 16px */
:deep(.el-form-item__label) {
  font-size: 14px;
}

:deep(.el-input__inner) {
  font-size: 16px;
}

/* 响应式：窄屏隐藏品牌面板，只留表单 */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
  .login-panel {
    flex: 1 1 100%;
  }
}
</style>
