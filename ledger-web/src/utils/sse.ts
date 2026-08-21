/**
 * SSE 流式解析工具（V2.1 新增）
 *
 * 用途：AI 对话 POST /v1/ai/chat:stream 走 fetch + ReadableStream（非 EventSource，
 * 因 EventSource 仅支持 GET 且无法自定义 Authorization 头）。
 *
 * 后端实际帧格式（AiChatServiceImpl，与设计文档 §13.9 的假设有出入）：
 * - chunk:  data: {"type":"chunk","content":"已"}\n\n  （每帧 3 字）
 * - tool:   data: {"type":"tool_call","tool":"...","result":...}\n\n
 * - done:   data: [DONE]\n\n  （字面量 [DONE]，非 JSON）
 * - error:  error: {"code":6001,"message":"..."}\n\n  （error: 前缀，非 data:）
 *
 * 容错点：后端控制器用 ServerSentEvent.data(raw) 二次包装已带 data: 前缀的 raw 串，
 * 可能产生 data:data: 双前缀；stripDataPrefix 至多剥离两次前缀以兼容。
 */
import type { AiAttachment, AiStreamFrame } from '@/types/api'
import { useUserStore } from '@/stores/user'

export interface StreamChatParams {
  content: string
  sessionId?: number
  attachments?: AiAttachment[]
}

export interface StreamCallbacks {
  /** chunk 帧（文本增量）与 tool_call 帧（工具调用）均经此回调 */
  onChunk: (frame: AiStreamFrame) => void
  onError: (err: Error) => void
  onDone: () => void
}

const AI_CHAT_STREAM_PATH = '/v1/ai/chat:stream'

/** 剥离重复的 data: 前缀（容错后端 ServerSentEvent 二次包装，至多 2 次） */
function stripDataPrefix(line: string): string {
  let s = line
  for (let i = 0; i < 2; i++) {
    if (s.startsWith('data:')) {
      s = s.slice(5)
      if (s.startsWith(' ')) s = s.slice(1)
    } else {
      break
    }
  }
  return s
}

/**
 * 发起 AI 流式对话。立即返回 AbortController 以便 UI 中断；所有结果经回调上抛。
 * @throws 不抛异常，网络/HTTP 错误统一走 onError 回调
 */
export function streamAiChat(
  params: StreamChatParams,
  callbacks: StreamCallbacks,
): AbortController {
  const controller = new AbortController()
  const base = (import.meta.env.VITE_API_BASE_URL as string) || ''
  const userStore = useUserStore()

  let finished = false
  const finish = (fn: () => void) => {
    if (finished) return
    finished = true
    fn()
  }

  void (async () => {
    try {
      const resp = await fetch(`${base}${AI_CHAT_STREAM_PATH}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${userStore.accessToken}`,
        },
        credentials: 'include',
        body: JSON.stringify(params),
        signal: controller.signal,
      })
      if (!resp.ok || !resp.body) {
        finish(() => callbacks.onError(new Error(`AI 流式请求失败：HTTP ${resp.status}`)))
        return
      }

      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''

        for (const rawLine of lines) {
          const line = rawLine.replace(/\r$/, '')
          if (!line) continue

          // 后端 sink 直发的 error: 前缀帧
          if (line.startsWith('error:')) {
            const payload = line.slice(6).trim()
            try {
              const parsed = JSON.parse(payload)
              finish(() => callbacks.onError(new Error(parsed.message || 'AI 服务异常')))
            } catch {
              finish(() => callbacks.onError(new Error(payload || 'AI 服务异常')))
            }
            continue
          }
          // event:error 行：错误 JSON 在随后 data: 行，跳过事件头
          if (line.startsWith('event:')) continue

          if (!line.startsWith('data:')) continue
          const payload = stripDataPrefix(line)
          if (payload === '') continue
          if (payload === '[DONE]') {
            finish(callbacks.onDone)
            continue
          }
          if (payload.startsWith('{')) {
            try {
              const parsed = JSON.parse(payload) as AiStreamFrame & {
                code?: number
                message?: string
              }
              // error 事件 data 或带 code/message 的错误对象
              if (parsed.type === 'error' || (parsed.code != null && parsed.message)) {
                finish(() => callbacks.onError(new Error(parsed.message || 'AI 服务异常')))
              } else if (parsed.type) {
                callbacks.onChunk(parsed as AiStreamFrame)
              }
            } catch {
              // 流式分片可能产生不完整 JSON，忽略此行
            }
          }
        }
      }
      // 流自然结束但未收到 [DONE]，视为完成
      finish(callbacks.onDone)
    } catch (e) {
      if (controller.signal.aborted) return
      finish(() => callbacks.onError(e instanceof Error ? e : new Error(String(e))))
    }
  })()

  return controller
}
