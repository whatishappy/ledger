/**
 * 导出模块 — /api/export/*
 *
 * exportExcel 契约（后端同步/异步自动判断）：
 * - ≤10000 行：后端直接写文件流到响应体（Content-Type 为 spreadsheet），此时 JSON 分支不生效
 * - >10000 行：返回 Result<String>，data 为 taskId
 * 因此前端统一用 `responseType: 'blob'` 请求，再根据响应 Content-Type 判断：
 *   - JSON → Blob.text() 解析出 taskId → 轮询状态
 *   - spreadsheet → 同步文件，直接下载
 */
import request from '@/utils/request'
import type { AxiosResponse } from 'axios'
import type { ExportRequest, ExportTaskVO } from '@/types/api'

export function exportExcel(data: ExportRequest) {
  return request.post<Blob, AxiosResponse<Blob>>('/export/excel', data, {
    responseType: 'blob',
  })
}

export function getExportStatus(taskId: string) {
  return request.get<ExportTaskVO, ExportTaskVO>(`/export/status/${taskId}`)
}

export function downloadExportFile(taskId: string) {
  return request.get<Blob, AxiosResponse<Blob>>(`/export/download/${taskId}`, {
    responseType: 'blob',
  })
}
