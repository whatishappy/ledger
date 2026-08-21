/**
 * AI 助手状态管理（V2.1 新增）
 *
 * 状态：会话列表、当前会话、消息、配额、流式标记、Drawer 显隐。
 * Actions：抽屉控制、会话增删查、SSE 发消息（流式追加助手回复）、OCR 上传、配额刷新、中断。
 *
 * SSE 帧 → 消息追加策略：
 * - chunk 帧：累积到 streamingContent，写入助手占位消息 content
 * - tool_call 帧：以 [TOOL_CALL] 标记追加，组件渲染为 Tool 卡片
 * - done / 流结束：固定占位消息并刷新配额与会话列表
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as aiApi from '@/api/ai'
import { streamAiChat } from '@/utils/sse'
import type {
  AiChatMessageVO,
  AiChatSessionVO,
  AiOcrResult,
  AiQuotaVO,
  AiStreamFrame,
} from '@/types/api'

/** 后端 LocalDateTime 序列化格式 yyyy-MM-dd HH:mm:ss */
function nowStr(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export const useAiStore = defineStore('ai', () => {
  const sessions = ref<AiChatSessionVO[]>([])
  const currentSessionId = ref<number | null>(null)
  const messages = ref<AiChatMessageVO[]>([])
  const quota = ref<AiQuotaVO | null>(null)
  const isStreaming = ref(false)
  const drawerVisible = ref(false)
  /** 当前正在生成的助手消息全文（流式追加） */
  const streamingContent = ref('')

  let abortController: AbortController | null = null

  /* ---------- 抽屉 ---------- */
  function openDrawer() {
    drawerVisible.value = true
  }
  function closeDrawer() {
    drawerVisible.value = false
  }

  /* ---------- 会话 ---------- */
  async function loadSessions() {
    sessions.value = await aiApi.listAiSessions(1, 50)
  }

  async function selectSession(sessionId: number) {
    currentSessionId.value = sessionId
    messages.value = await aiApi.listAiMessages(sessionId, 50)
    streamingContent.value = ''
  }

  async function deleteSession(sessionId: number) {
    await aiApi.deleteAiSession(sessionId)
    sessions.value = sessions.value.filter((s) => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
      streamingContent.value = ''
    }
  }

  /** 新建会话：清空当前对话区，下次发送不传 sessionId，后端自动建会话 */
  function newSession() {
    currentSessionId.value = null
    messages.value = []
    streamingContent.value = ''
  }

  /* ---------- 配额 ---------- */
  async function refreshQuota() {
    quota.value = await aiApi.getAiQuota()
  }

  /* ---------- 流式对话 ---------- */
  async function sendMessage(content: string) {
    if (isStreaming.value || !content.trim()) return

    const sid = currentSessionId.value
    const now = nowStr()

    // 压入用户消息 + 助手占位消息
    const userMsg: AiChatMessageVO = {
      id: Date.now(),
      sessionId: sid ?? 0,
      role: 'user',
      content,
      tokens: 0,
      createdAt: now,
    }
    const assistantMsg: AiChatMessageVO = {
      id: Date.now() + 1,
      sessionId: sid ?? 0,
      role: 'assistant',
      content: '',
      tokens: 0,
      createdAt: now,
    }
    messages.value = [...messages.value, userMsg, assistantMsg]

    isStreaming.value = true
    streamingContent.value = ''

    abortController = streamAiChat(
      { content, sessionId: sid ?? undefined },
      {
        onChunk: (frame: AiStreamFrame) => {
          if (frame.type === 'chunk' && frame.content) {
            streamingContent.value += frame.content
          } else if (frame.type === 'tool_call') {
            // 围栏标记，避免 result JSON 含 ] 时解析歧义
            streamingContent.value +=
              `\n[TOOL_CALL_BEGIN]\n${frame.tool ?? ''}\n${JSON.stringify(frame.result ?? '')}\n[TOOL_CALL_END]\n`
          }
          assistantMsg.content = streamingContent.value
        },
        onError: (err: Error) => {
          isStreaming.value = false
          assistantMsg.content =
            streamingContent.value || `（AI 回复失败：${err.message}）`
        },
        onDone: () => {
          isStreaming.value = false
          assistantMsg.content = streamingContent.value
          // 流结束后刷新配额与会话列表（会话可能由后端新建）
          void refreshQuota().catch(() => {})
          void loadSessions().catch(() => {})
        },
      },
    )
  }

  /** 中断当前流式输出 */
  function stopStreaming() {
    abortController?.abort()
    isStreaming.value = false
  }

  /* ---------- OCR 上传 ---------- */
  async function uploadReceipt(file: File): Promise<AiOcrResult> {
    return aiApi.uploadReceipt(file)
  }

  return {
    sessions,
    currentSessionId,
    messages,
    quota,
    isStreaming,
    drawerVisible,
    streamingContent,
    openDrawer,
    closeDrawer,
    loadSessions,
    selectSession,
    deleteSession,
    newSession,
    refreshQuota,
    sendMessage,
    stopStreaming,
    uploadReceipt,
  }
})
