/**
 * Axios 实例 + 拦截器
 *
 * - 请求：注入 `Authorization: Bearer <accessToken>`，`withCredentials` 携带 HttpOnly Cookie
 * - 响应：`code === 0` 成功 → 剥壳返回 `data`；否则统一提示并 reject
 * - 401 / code===401 → 无感刷新（刷新请求在飞则排队等待重放），刷新失败 → 清空登录态跳 /login
 * - `responseType: 'blob'` 的下载请求 → 不进 code 拦截器，直接返回 AxiosResponse
 *
 * 注意：成功判定为 `code === 0`（后端 Result 约定，非 200）。
 */
import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { Result } from '@/types/api'

/** 业务异常：携带后端错误码，便于上层/测试区分（如 2002 乐观锁） */
export class BusinessError extends Error {
  code?: number
  constructor(message: string, code?: number) {
    super(message)
    this.name = 'BusinessError'
    this.code = code
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  withCredentials: true,
})

/* ---------- 请求拦截器：注入 Bearer Token ---------- */
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const userStore = useUserStore()
  if (userStore.accessToken) {
    config.headers.Authorization = `Bearer ${userStore.accessToken}`
  }
  return config
})

/* ---------- 401 刷新状态 ---------- */
let isRefreshing = false
let redirecting = false
type PendingTask = {
  config: AxiosRequestConfig
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
}
let pendingQueue: PendingTask[] = []

function isRefreshRequest(config?: AxiosRequestConfig): boolean {
  return typeof config?.url === 'string' && config.url.includes('/auth/refresh')
}

/** 刷新 accessToken（用原始 axios，避免自身触发拦截器 401 死循环） */
async function doRefresh(): Promise<string> {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || ''
  const { data } = await axios.post<Result<string>>(`${base}/auth/refresh`, null, {
    withCredentials: true,
  })
  if (data.code === 0 && data.data) {
    return data.data
  }
  throw new BusinessError(data.message || '刷新失败', data.code)
}

/** 清空登录态并跳转登录页（防抖，避免重复触发） */
async function redirectToLogin(): Promise<void> {
  if (redirecting) return
  redirecting = true
  const userStore = useUserStore()
  userStore.reset()
  try {
    const { default: router } = await import('@/router')
    if (router.currentRoute.value.path !== '/login') {
      ElMessage.warning('登录已过期，请重新登录')
      await router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath },
      })
    }
  } finally {
    redirecting = false
  }
}

/** 用新 token 重放一个请求（剥壳结果透传） */
function retryWithToken(config: AxiosRequestConfig, token: string): Promise<unknown> {
  return request({
    ...config,
    headers: { ...(config.headers ?? {}), Authorization: `Bearer ${token}` },
  })
}

/** 无感刷新：单一刷新请求在飞，其余 401 请求排队等待重放 */
async function handleUnauthorized(config: AxiosRequestConfig): Promise<unknown> {
  if (isRefreshRequest(config)) {
    await redirectToLogin()
    throw new BusinessError('未登录')
  }

  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingQueue.push({ config, resolve, reject })
    })
  }

  isRefreshing = true
  try {
    const token = await doRefresh()
    const userStore = useUserStore()
    userStore.setAccessToken(token)
    pendingQueue.forEach((task) => task.resolve(retryWithToken(task.config, token)))
    pendingQueue = []
    return retryWithToken(config, token)
  } catch (error) {
    pendingQueue.forEach((task) => task.reject(error))
    pendingQueue = []
    await redirectToLogin()
    throw error
  } finally {
    isRefreshing = false
  }
}

/* ---------- 响应拦截器 ---------- */
request.interceptors.response.use(
  // 拦截器会「剥壳」返回 data，返回值类型与 AxiosResponse 不符，此处断言以通过 axios 类型约束
  (response: AxiosResponse): AxiosResponse | Promise<AxiosResponse> => {
    // 下载类请求：返回原始响应，由调用方处理 Blob
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data as Result
    if (res.code === 0) {
      return res.data as AxiosResponse
    }
    if (res.code === 401) {
      return handleUnauthorized(response.config) as Promise<AxiosResponse>
    }
    const message = res.message || '请求失败'
    if (res.code === 2002) {
      ElMessage.error('该账目已被其他操作修改，请刷新页面重试')
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(new BusinessError(message, res.code))
  },
  (error: AxiosError) => {
    const status = error.response?.status
    if (status === 401) {
      return handleUnauthorized(error.config as AxiosRequestConfig)
    }
    const res = error.response?.data as Result | undefined
    const message = res?.message || error.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
