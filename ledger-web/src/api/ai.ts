/**
 * AI 智能助手模块 — /api/v1/ai（AI-01~AI-08）
 *
 * 注意：
 * - AI-01 流式聊天端点为 POST /v1/ai/chat:stream（SSE），不走 axios，
 *   实现在 utils/sse.ts 的 streamAiChat；本文件仅含 axios 接口。
 * - 请求体字段为 content（非 message），以后端 AiChatRequest 为准。
 */
import request from '@/utils/request'
import type {
  AiChatMessageVO,
  AiChatSessionVO,
  AiOcrResult,
  AiQuotaVO,
} from '@/types/api'

/** AI-02 查询会话列表 */
export function listAiSessions(page: number = 1, size: number = 20) {
  return request.get<AiChatSessionVO[], AiChatSessionVO[]>('/v1/ai/sessions', {
    params: { page, size },
  })
}

/** AI-03 查询会话历史消息 */
export function listAiMessages(sessionId: number, limit: number = 50) {
  return request.get<AiChatMessageVO[], AiChatMessageVO[]>(
    `/v1/ai/sessions/${sessionId}/messages`,
    { params: { limit } },
  )
}

/** AI-04 删除会话 */
export function deleteAiSession(sessionId: number) {
  return request.delete<unknown, void>(`/v1/ai/sessions/${sessionId}`)
}

/** AI-05 查询今日 AI 配额 */
export function getAiQuota() {
  return request.get<AiQuotaVO, AiQuotaVO>('/v1/ai/quota')
}

/** AI-06 上传小票图片并 OCR 识别（multipart） */
export function uploadReceipt(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<AiOcrResult, AiOcrResult>('/v1/ai/receipt/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** AI-07 通过图片 URL 进行 OCR 识别 */
export function ocrReceiptByUrl(imageUrl: string) {
  return request.post<AiOcrResult, AiOcrResult>('/v1/ai/receipt/ocr', undefined, {
    params: { imageUrl },
  })
}

/** AI-08 查询 AI 模型健康状态（主/备模型可用性） */
export function getAiHealth() {
  return request.get<Record<string, unknown>, Record<string, unknown>>(
    '/v1/ai/health',
  )
}
