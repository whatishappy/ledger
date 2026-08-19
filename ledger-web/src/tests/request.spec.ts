import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { ElMessage } from 'element-plus'
import router from '@/router'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() },
}))

vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { path: '/dashboard', fullPath: '/dashboard' } },
    push: vi.fn(),
  },
}))

const ok = (data: unknown) => ({
  code: 0,
  message: 'ok',
  data,
  timestamp: '2026-08-18T00:00:00',
})
const unauthorized = {
  code: 401,
  message: '未登录或登录已过期',
  data: null,
  timestamp: '2026-08-18T00:00:00',
}

describe('request 拦截器', () => {
  let mockRequest: MockAdapter
  let mockRaw: MockAdapter
  const pushSpy = vi.mocked(router.push)

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    // doRefresh 走原始 axios（避免拦截器死循环），需单独挂 MockAdapter
    mockRequest = new MockAdapter(request)
    mockRaw = new MockAdapter(axios)
  })

  afterEach(() => {
    mockRequest.restore()
    mockRaw.restore()
  })

  it('code===0 时剥壳返回 data', async () => {
    mockRequest.onGet('/api/user/info').reply(200, ok({ id: 1, username: 'demo' }))
    const data = await request.get('/api/user/info')
    expect(data).toEqual({ id: 1, username: 'demo' })
  })

  it('业务错误：提示 message 并 reject BusinessError', async () => {
    mockRequest.onGet('/api/user/info').reply(200, {
      code: 500,
      message: '用户名不存在',
      data: null,
      timestamp: '2026-08-18T00:00:00',
    })
    const promise = request.get('/api/user/info')
    await expect(promise).rejects.toThrowError('用户名不存在')
    await expect(promise).rejects.toMatchObject({ name: 'BusinessError', code: 500 })
    expect(ElMessage.error).toHaveBeenCalledWith('用户名不存在')
  })

  it('2002 乐观锁：使用专用提示文案', async () => {
    mockRequest.onPut('/api/accounts').reply(200, {
      code: 2002,
      message: '版本冲突',
      data: null,
      timestamp: '2026-08-18T00:00:00',
    })
    await expect(request.put('/api/accounts')).rejects.toThrow()
    expect(ElMessage.error).toHaveBeenCalledWith('该账目已被其他操作修改，请刷新页面重试')
  })

  it('blob 下载请求直接返回原始响应', async () => {
    const blob = new Blob(['xlsx-content'])
    mockRequest
      .onGet('/api/export')
      .reply(200, blob, {
        'content-type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
    const res = await request.get('/api/export', { responseType: 'blob' })
    expect(res.data).toBe(blob)
  })

  it('请求拦截器注入 Bearer token', async () => {
    const store = useUserStore()
    store.setAccessToken('test-token')
    let captured: string | undefined
    mockRequest.onGet('/api/user/info').reply((config) => {
      captured = (config.headers as Record<string, unknown>).Authorization as string
      return [200, ok(null)]
    })
    await request.get('/api/user/info')
    expect(captured).toBe('Bearer test-token')
  })

  it('401 无感刷新：并发请求排队重放，仅发起一次刷新', async () => {
    const store = useUserStore()
    store.setAccessToken('old-token')

    let expiredCount = 0
    mockRequest.onGet('/api/expired').reply(() => {
      expiredCount += 1
      return expiredCount <= 2 ? [401, unauthorized] : [200, ok({ ok: true })]
    })

    let resolveRefresh!: (value: [number, unknown]) => void
    let refreshRequested!: () => void
    const refreshRequestedPromise = new Promise<void>((r) => {
      refreshRequested = r
    })
    mockRaw.onPost('/auth/refresh').reply(
      () =>
        new Promise((resolve) => {
          refreshRequested()
          resolveRefresh = resolve
        }),
    )

    const p1 = request.get('/api/expired')
    const p2 = request.get('/api/expired')

    await refreshRequestedPromise
    // 让第二个请求的 401 响应先完成入队，再放行刷新
    await new Promise((r) => setTimeout(r, 0))
    resolveRefresh([200, ok('new-token')])

    await expect(p1).resolves.toEqual({ ok: true })
    await expect(p2).resolves.toEqual({ ok: true })
    expect(store.accessToken).toBe('new-token')
    expect(expiredCount).toBe(4)
  })

  it('刷新失败：清空登录态并跳转登录页', async () => {
    const store = useUserStore()
    store.setAccessToken('old-token')
    store.setUserInfo({ id: 1, username: 'demo', nickname: 'demo', status: 1 })

    mockRequest.onGet('/api/expired').reply(401, unauthorized)
    mockRaw.onPost('/auth/refresh').reply(500, {
      code: 500,
      message: '服务器错误',
      data: null,
      timestamp: '2026-08-18T00:00:00',
    })

    await expect(request.get('/api/expired')).rejects.toThrow()
    expect(store.accessToken).toBe('')
    expect(store.userInfo).toBeNull()
    expect(ElMessage.warning).toHaveBeenCalledWith('登录已过期，请重新登录')
    expect(pushSpy).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/dashboard' } })
  })
})
